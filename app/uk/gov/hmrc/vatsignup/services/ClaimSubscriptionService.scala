/*
 * Copyright 2018 HM Revenue & Customs
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

import java.text.SimpleDateFormat

import cats.data.EitherT
import cats.implicits._
import javax.inject.{Inject, Singleton}
import play.api.mvc.Request
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.auth.core.authorise.EmptyPredicate
import uk.gov.hmrc.auth.core.retrieve.Retrievals.{credentials, groupIdentifier}
import uk.gov.hmrc.auth.core.retrieve.{Credentials, ~}
import uk.gov.hmrc.http.{ForbiddenException, HeaderCarrier}
import uk.gov.hmrc.vatsignup.connectors.{KnownFactsConnector, TaxEnrolmentsConnector}
import uk.gov.hmrc.vatsignup.httpparsers.AllocateEnrolmentResponseHttpParser.EnrolSuccess
import uk.gov.hmrc.vatsignup.httpparsers.KnownFactsHttpParser.KnownFacts
import uk.gov.hmrc.vatsignup.httpparsers.UpsertEnrolmentResponseHttpParser._
import uk.gov.hmrc.vatsignup.httpparsers.{AllocateEnrolmentResponseHttpParser, KnownFactsHttpParser}
import uk.gov.hmrc.vatsignup.models.monitoring.ClaimSubscriptionAuditing.ClaimSubscriptionAuditModel
import uk.gov.hmrc.vatsignup.services.ClaimSubscriptionService._
import uk.gov.hmrc.vatsignup.services.monitoring.AuditService

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ClaimSubscriptionService @Inject()(authConnector: AuthConnector,
                                         knownFactsConnector: KnownFactsConnector,
                                         taxEnrolmentsConnector: TaxEnrolmentsConnector,
                                         auditService: AuditService
                                        )(implicit ec: ExecutionContext) {

  def claimSubscription(vatNumber: String,
                        businessPostcode: Option[String],
                        vatRegistrationDate: Option[String],
                        isFromBta: Boolean
                       )(implicit hc: HeaderCarrier, request: Request[_]): Future[ClaimSubscriptionResponse] = {
    for {
      knownFacts <- getKnownFacts(vatNumber)
      _ <- EitherT.fromEither[Future](checkKnownFactsMatch(
        businessPostcode,
        vatRegistrationDate,
        knownFacts
      ))
      _ <- upsertAndAllocateEnrolment(vatNumber, knownFacts, isFromBta)
    } yield SubscriptionClaimed

  }.value

  private def checkKnownFactsMatch(optBusinessPostcode: Option[String],
                                   optVatRegistrationDate: Option[String],
                                   storedKnownFacts: KnownFacts): Either[ClaimSubscriptionService.KnownFactsMismatch.type, KnownFactsMatch] = {
    (optBusinessPostcode, optVatRegistrationDate) match {
      case (Some(businessPostcode), Some(vatRegistrationDate)) =>
        if ((storedKnownFacts.vatRegistrationDate == vatRegistrationDate)
          &&
          ((storedKnownFacts.businessPostcode filterNot (_.isWhitespace)) equalsIgnoreCase (businessPostcode filterNot (_.isWhitespace)))
        ) Right(KnownFactsMatched)
        else Left(KnownFactsMismatch)
      case _ =>
        Right(KnownFactsNotSupplied)
    }
  }

  private def getKnownFacts(vatNumber: String)(implicit hc: HeaderCarrier): EitherT[Future, ClaimSubscriptionFailure, KnownFacts] =
    EitherT(knownFactsConnector.getKnownFacts(vatNumber)) leftMap {
      case KnownFactsHttpParser.InvalidVatNumber => InvalidVatNumber
      case KnownFactsHttpParser.VatNumberNotFound => VatNumberNotFound
      case err => KnownFactsFailure
    }

  private def upsertAndAllocateEnrolment(vatNumber: String,
                                         knownFacts: KnownFacts,
                                         isFromBta: Boolean)(
                                           implicit hc: HeaderCarrier, request: Request[_]
                                         ): EitherT[Future, ClaimSubscriptionFailure, EnrolSuccess.type] = {
    EitherT.right(authConnector.authorise(EmptyPredicate, credentials and groupIdentifier)) flatMap {
      case Credentials(credentialId, GGProviderId) ~ Some(groupId) =>
        for {
          _ <- upsertEnrolment(vatNumber, knownFacts, isFromBta)
          res <- allocateEnrolment(vatNumber, knownFacts, isFromBta, groupId, credentialId)
        } yield res
      case _ =>
        EitherT.liftF(Future.failed(new ForbiddenException("Invalid auth credentials")))
    }
  }

  private def allocateEnrolment(vatNumber: String,
                                knownFacts: KnownFacts,
                                isFromBta: Boolean,
                                groupId: String,
                                credentialId: String
                               )(
                                 implicit hc: HeaderCarrier, request: Request[_]
                               ): EitherT[Future, ClaimSubscriptionFailure, EnrolSuccess.type] =
    EitherT(taxEnrolmentsConnector.allocateEnrolment(
      groupId = groupId,
      credentialId = credentialId,
      vatNumber = vatNumber,
      postcode = knownFacts.businessPostcode,
      vatRegistrationDate = knownFacts.vatRegistrationDate.toTaxEnrolmentsFormat
    )) bimap({
      case enrolFailure@AllocateEnrolmentResponseHttpParser.EnrolFailure(message) =>
        auditService.audit(ClaimSubscriptionAuditModel(
          vatNumber,
          businessPostcode = knownFacts.businessPostcode,
          vatRegistrationDate = knownFacts.vatRegistrationDate.toTaxEnrolmentsFormat,
          isFromBta = isFromBta,
          isSuccess = false,
          failureMessage = Some(enrolFailure.message)
        ))
        ClaimSubscriptionService.EnrolFailure
    }, result => {
      auditService.audit(ClaimSubscriptionAuditModel(
        vatNumber,
        businessPostcode = knownFacts.businessPostcode,
        vatRegistrationDate = knownFacts.vatRegistrationDate.toTaxEnrolmentsFormat,
        isFromBta = isFromBta,
        isSuccess = true,
        failureMessage = None
      ))
      result
    })

  private def upsertEnrolment(vatNumber: String, knownFacts: KnownFacts, isFromBta: Boolean)(
                               implicit hc: HeaderCarrier, request: Request[_]
                             ): EitherT[Future, ClaimSubscriptionFailure, UpsertEnrolmentSuccess.type] =
    EitherT.right(taxEnrolmentsConnector.upsertEnrolment(
      vatNumber = vatNumber,
      postcode = knownFacts.businessPostcode,
      vatRegistrationDate = knownFacts.vatRegistrationDate.toTaxEnrolmentsFormat
    ) map {
      case Right(_) => UpsertEnrolmentSuccess
      case Left(UpsertEnrolmentFailure(_, message)) => {
        auditService.audit(ClaimSubscriptionAuditModel(
          vatNumber,
          businessPostcode = knownFacts.businessPostcode,
          vatRegistrationDate = knownFacts.vatRegistrationDate.toTaxEnrolmentsFormat,
          isFromBta = isFromBta,
          isSuccess = false,
          failureMessage = Some(message)
        ))
        UpsertEnrolmentSuccess
    }})

}

object ClaimSubscriptionService {
  val GGProviderId = "GovernmentGateway"

  type ClaimSubscriptionResponse = Either[ClaimSubscriptionFailure, SubscriptionClaimed.type]

  case object SubscriptionClaimed

  sealed trait KnownFactsMatch

  case object KnownFactsMatched extends KnownFactsMatch

  case object KnownFactsNotSupplied extends KnownFactsMatch

  sealed trait ClaimSubscriptionFailure

  case object VatNumberNotFound extends ClaimSubscriptionFailure

  case object KnownFactsMismatch extends ClaimSubscriptionFailure

  case object InvalidVatNumber extends ClaimSubscriptionFailure

  case object KnownFactsFailure extends ClaimSubscriptionFailure

  case object EnrolFailure extends ClaimSubscriptionFailure

  implicit class KnownFactsDateFormatter(date: String) {
    def toTaxEnrolmentsFormat: String = {
      val desFormat = new SimpleDateFormat("yyyy-MM-dd")
      val taxEnrolmentsFormat = new SimpleDateFormat("dd/MM/yy")
      val parsedDate = desFormat parse date

      taxEnrolmentsFormat format parsedDate
    }
  }

}
