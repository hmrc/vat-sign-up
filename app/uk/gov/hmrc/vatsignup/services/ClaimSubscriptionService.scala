/*
 * Copyright 2020 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.vatsignup.services

import cats.data.EitherT
import cats.implicits._
import javax.inject.{Inject, Singleton}
import play.api.mvc.Request
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.auth.core.authorise.EmptyPredicate
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals.{credentials, groupIdentifier}
import uk.gov.hmrc.auth.core.retrieve.{Credentials, ~}
import uk.gov.hmrc.http.{ForbiddenException, HeaderCarrier}
import uk.gov.hmrc.vatsignup.connectors.{TaxEnrolmentsConnector, VatCustomerDetailsConnector}
import uk.gov.hmrc.vatsignup.httpparsers.AllocateEnrolmentResponseHttpParser.EnrolSuccess
import uk.gov.hmrc.vatsignup.httpparsers.UpsertEnrolmentResponseHttpParser.UpsertEnrolmentFailure
import uk.gov.hmrc.vatsignup.httpparsers.{AllocateEnrolmentResponseHttpParser, VatCustomerDetailsHttpParser}
import uk.gov.hmrc.vatsignup.models.KnownFacts
import uk.gov.hmrc.vatsignup.models.monitoring.ClaimSubscriptionAuditing.ClaimSubscriptionAuditModel
import uk.gov.hmrc.vatsignup.services.ClaimSubscriptionService._
import uk.gov.hmrc.vatsignup.services.monitoring.AuditService
import uk.gov.hmrc.vatsignup.utils.KnownFactsDateFormatter.KnownFactsDateFormatter

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ClaimSubscriptionService @Inject()(authConnector: AuthConnector,
                                         vatCustomerDetailsConnector: VatCustomerDetailsConnector,
                                         taxEnrolmentsConnector: TaxEnrolmentsConnector,
                                         checkEnrolmentAllocationService: CheckEnrolmentAllocationService,
                                         auditService: AuditService
                                        )(implicit ec: ExecutionContext) {

  def claimSubscription(vatNumber: String,
                        optBusinessPostcode: Option[String],
                        vatRegistrationDate: String,
                        isFromBta: Boolean
                       )(implicit hc: HeaderCarrier, request: Request[_]): Future[ClaimSubscriptionResponse] = {
    for {
      _ <- getEnrolmentAllocationStatus(vatNumber)
      knownFacts <- getKnownFacts(vatNumber)
      _ <- EitherT.fromEither[Future](checkKnownFactsMatch(
        optBusinessPostcode,
        vatRegistrationDate,
        knownFacts
      ))
      _ <- upsertAndAllocateEnrolment(vatNumber, knownFacts, isFromBta)
    } yield SubscriptionClaimed

  }.value

  def claimSubscriptionWithEnrolment(vatNumber: String,
                                     isFromBta: Boolean
                                    )(implicit hc: HeaderCarrier,
                                      request: Request[_]
                                    ): Future[ClaimSubscriptionResponse] = {
    for {
      _ <- getEnrolmentAllocationStatus(vatNumber)
      knownFacts <- getKnownFacts(vatNumber)
      _ <- upsertAndAllocateEnrolment(vatNumber, knownFacts, isFromBta)
    } yield SubscriptionClaimed
  }.value

  private def checkKnownFactsMatch(optBusinessPostcode: Option[String],
                                   vatRegistrationDate: String,
                                   storedKnownFacts: KnownFacts): Either[ClaimSubscriptionService.KnownFactsMismatch.type, KnownFactsMatch] = {

    val registrationDateMatch = vatRegistrationDate.equals(storedKnownFacts.vatRegistrationDate)
    val postCodeMatch = optBusinessPostcode.isEmpty ||
      optBusinessPostcode.map(_ filterNot (_.isWhitespace)).contains(storedKnownFacts.businessPostcode filterNot (_.isWhitespace))

    if (registrationDateMatch && postCodeMatch) Right(KnownFactsMatched)
    else Left(KnownFactsMismatch)
  }

  private def getKnownFacts(vatNumber: String)(implicit hc: HeaderCarrier): EitherT[Future, ClaimSubscriptionFailure, KnownFacts] =
    EitherT(vatCustomerDetailsConnector.getVatCustomerDetails(vatNumber)) map { details =>
      details.knownFacts
    } leftMap {
      case VatCustomerDetailsHttpParser.InvalidVatNumber => InvalidVatNumber
      case VatCustomerDetailsHttpParser.VatNumberNotFound => VatNumberNotFound
      case VatCustomerDetailsHttpParser.Deregistered => Deregistered
      case _ => KnownFactsFailure
    }

  private def getEnrolmentAllocationStatus(vatNumber: String)
                                          (implicit hc: HeaderCarrier): EitherT[Future, ClaimSubscriptionFailure, EnrolmentNotAllocated.type] = {
    EitherT(checkEnrolmentAllocationService.getGroupIdForMtdVatEnrolment(vatNumber)) transform {
      case Right(_) =>
        Right(EnrolmentNotAllocated)
      case Left(CheckEnrolmentAllocationService.EnrolmentAlreadyAllocated(_)) =>
        Left(EnrolmentAlreadyAllocated)
      case Left(CheckEnrolmentAllocationService.UnexpectedEnrolmentStoreProxyFailure(status)) =>
        Left(CheckEnrolmentAllocationFailed(status))
    }
  }

  private def upsertAndAllocateEnrolment(vatNumber: String,
                                         knownFacts: KnownFacts,
                                         isFromBta: Boolean)(
                                          implicit hc: HeaderCarrier, request: Request[_]
                                        ): EitherT[Future, ClaimSubscriptionFailure, EnrolSuccess.type] = {
    EitherT.right(authConnector.authorise(EmptyPredicate, credentials and groupIdentifier)) flatMap {
      case Some(Credentials(credentialId, GGProviderId)) ~ Some(groupId) =>
        for {
          upsertEnrolmentResponse <- upsertEnrolment(vatNumber, knownFacts, isFromBta)
          res <- allocateEnrolment(vatNumber, knownFacts, isFromBta, groupId, credentialId, upsertEnrolmentResponse)
        } yield res
      case _ =>
        EitherT.liftF(Future.failed(new ForbiddenException("Invalid auth credentials")))
    }
  }

  private def allocateEnrolment(vatNumber: String,
                                knownFacts: KnownFacts,
                                isFromBta: Boolean,
                                groupId: String,
                                credentialId: String,
                                upsertEnrolmentResponse: UpsertEnrolmentResponse
                               )(
                                 implicit hc: HeaderCarrier, request: Request[_]
                               ): EitherT[Future, ClaimSubscriptionFailure, EnrolSuccess.type] =
    EitherT(taxEnrolmentsConnector.allocateEnrolment(
      groupId = groupId,
      credentialId = credentialId,
      vatNumber = vatNumber,
      postcode = knownFacts.businessPostcode,
      vatRegistrationDate = knownFacts.vatRegistrationDate.toTaxEnrolmentsFormat
    )) bimap( {
      enrolFailure: AllocateEnrolmentResponseHttpParser.EnrolFailure =>
        val upsertEnrolmentFailureMessage = upsertEnrolmentResponse match {
          case UpsertEnrolmentSuccess => None
          case IgnoredUpsertEnrolmentFailure(upsertEnrolmentErrorMessage) => Some(upsertEnrolmentErrorMessage)
        }
        auditService.audit(ClaimSubscriptionAuditModel(
          vatNumber,
          businessPostcode = knownFacts.businessPostcode,
          vatRegistrationDate = knownFacts.vatRegistrationDate.toTaxEnrolmentsFormat,
          isFromBta = isFromBta,
          isSuccess = false,
          allocateEnrolmentFailureMessage = Some(enrolFailure.message),
          upsertEnrolmentFailureMessage = upsertEnrolmentFailureMessage
        ))
        ClaimSubscriptionService.EnrolFailure
    }, result => {
      auditService.audit(ClaimSubscriptionAuditModel(
        vatNumber,
        businessPostcode = knownFacts.businessPostcode,
        vatRegistrationDate = knownFacts.vatRegistrationDate.toTaxEnrolmentsFormat,
        isFromBta = isFromBta,
        isSuccess = true
      ))
      result
    })

  private def upsertEnrolment(vatNumber: String, knownFacts: KnownFacts, isFromBta: Boolean)(
    implicit hc: HeaderCarrier, request: Request[_]
  ): EitherT[Future, ClaimSubscriptionFailure, UpsertEnrolmentResponse] =
    EitherT(taxEnrolmentsConnector.upsertEnrolment(
      vatNumber = vatNumber,
      postcode = knownFacts.businessPostcode,
      vatRegistrationDate = knownFacts.vatRegistrationDate.toTaxEnrolmentsFormat
    )) transform {
      case Right(_) =>
        Right(UpsertEnrolmentSuccess)
      case Left(UpsertEnrolmentFailure(_, message)) =>
        Right(IgnoredUpsertEnrolmentFailure(message))
    }

}

object ClaimSubscriptionService {
  val GGProviderId = "GovernmentGateway"

  type ClaimSubscriptionResponse = Either[ClaimSubscriptionFailure, SubscriptionClaimed.type]

  case object SubscriptionClaimed

  case object EnrolmentNotAllocated

  sealed trait KnownFactsMatch

  case object KnownFactsMatched extends KnownFactsMatch

  sealed trait ClaimSubscriptionFailure

  case object EnrolmentAlreadyAllocated extends ClaimSubscriptionFailure

  case object VatNumberNotFound extends ClaimSubscriptionFailure

  case object KnownFactsMismatch extends ClaimSubscriptionFailure

  case object Deregistered extends ClaimSubscriptionFailure

  case object InvalidVatNumber extends ClaimSubscriptionFailure

  case object KnownFactsFailure extends ClaimSubscriptionFailure

  case object EnrolFailure extends ClaimSubscriptionFailure

  case class CheckEnrolmentAllocationFailed(status: Int) extends ClaimSubscriptionFailure

  sealed trait UpsertEnrolmentResponse

  case object UpsertEnrolmentSuccess extends UpsertEnrolmentResponse

  case class IgnoredUpsertEnrolmentFailure(failureMessage: String) extends UpsertEnrolmentResponse

}
