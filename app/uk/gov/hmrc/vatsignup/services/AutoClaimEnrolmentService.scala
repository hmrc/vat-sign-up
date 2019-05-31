/*
 * Copyright 2019 HM Revenue & Customs
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
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.vatsignup.connectors.{EnrolmentStoreProxyConnector, KnownFactsConnector, TaxEnrolmentsConnector}
import uk.gov.hmrc.vatsignup.httpparsers.KnownFactsHttpParser.KnownFacts
import uk.gov.hmrc.vatsignup.httpparsers.{AllocateEnrolmentResponseHttpParser, _}
import uk.gov.hmrc.vatsignup.services.AutoClaimEnrolmentService._
import uk.gov.hmrc.vatsignup.utils.KnownFactsDateFormatter.KnownFactsDateFormatter

import scala.concurrent.{ExecutionContext, Future}


@Singleton
class AutoClaimEnrolmentService @Inject()(knownFactsConnector: KnownFactsConnector,
                                          taxEnrolmentsConnector: TaxEnrolmentsConnector,
                                          enrolmentStoreProxyConnector: EnrolmentStoreProxyConnector,
                                          checkEnrolmentAllocationService: CheckEnrolmentAllocationService,
                                          assignEnrolmentToUserService: AssignEnrolmentToUserService
                                         )(implicit ec: ExecutionContext) {

  def autoClaimEnrolment(vatNumber: String)(implicit hc: HeaderCarrier): Future[AutoClaimEnrolmentResponse] = {
    for {
      groupId <- getLegacyEnrolmentAllocation(vatNumber)
      credentialIds <- getUserIDs(vatNumber)
      knownFacts <- getKnownFacts(vatNumber)
      _ <- upsertEnrolmentAllocation(vatNumber, knownFacts)
      principalCredentialId = credentialIds.head
      _ <- allocateEnrolment(vatNumber, groupId, principalCredentialId, knownFacts)
      _ <- assignEnrolmentToUser(credentialIds filterNot (_ == principalCredentialId), vatNumber)
    } yield EnrolmentAssigned
  }.value

  private def getLegacyEnrolmentAllocation(vatNumber: String)(implicit hc: HeaderCarrier): EitherT[Future, AutoClaimEnrolmentFailure, String] = {
    EitherT(checkEnrolmentAllocationService.getGroupIdForLegacyVatEnrolment(vatNumber)) transform {
      case Right(_) =>
        Left(EnrolmentNotAllocated)
      case Left(CheckEnrolmentAllocationService.EnrolmentAlreadyAllocated(groupId)) =>
        Right(groupId)
      case Left(CheckEnrolmentAllocationService.UnexpectedEnrolmentStoreProxyFailure(status)) =>
        Left(EnrolmentStoreProxyFailure(status))
    }
  }

  private def getUserIDs(vatNumber: String)(implicit hc: HeaderCarrier): EitherT[Future, AutoClaimEnrolmentFailure, Set[String]] = {
    EitherT(enrolmentStoreProxyConnector.getUserIds(vatNumber)) transform {
      case Right(QueryUsersHttpParser.UsersFound(retrievedUserIds)) if retrievedUserIds.nonEmpty =>
        Right(retrievedUserIds)
      case Right(QueryUsersHttpParser.NoUsersFound) =>
        Left(NoUsersFound)
      case _ =>
        Left(EnrolmentStoreProxyConnectionFailure)
    }
  }

  private def getKnownFacts(vatNumber: String)(implicit hc: HeaderCarrier): EitherT[Future, AutoClaimEnrolmentFailure, KnownFacts] =
    EitherT(knownFactsConnector.getKnownFacts(vatNumber)) leftMap {
      case KnownFactsHttpParser.InvalidVatNumber => InvalidVatNumber
      case KnownFactsHttpParser.VatNumberNotFound => VatNumberNotFound
      case _ => KnownFactsFailure
    }

  private def upsertEnrolmentAllocation(vatNumber: String,
                                        knownFacts: KnownFacts
                                       )(implicit hc: HeaderCarrier): EitherT[Future, AutoClaimEnrolmentFailure, AutoClaimEnrolmentSuccess] =
    EitherT(taxEnrolmentsConnector.upsertEnrolment(
      vatNumber = vatNumber,
      postcode = knownFacts.businessPostcode,
      vatRegistrationDate = knownFacts.vatRegistrationDate.toTaxEnrolmentsFormat
    )) transform {
      case Right(UpsertEnrolmentResponseHttpParser.UpsertEnrolmentSuccess) =>
        Right(AutoClaimEnrolmentService.UpsertEnrolmentSuccess)
      case Left(UpsertEnrolmentResponseHttpParser.UpsertEnrolmentFailure(_, message)) =>
        Left(AutoClaimEnrolmentService.UpsertEnrolmentFailure(message))
    }

  private def allocateEnrolment(vatNumber: String,
                                groupId: String,
                                credentialId: String,
                                knownFacts: KnownFacts
                               )(implicit hc: HeaderCarrier): EitherT[Future, AutoClaimEnrolmentFailure, AutoClaimEnrolmentSuccess] = {
    EitherT(taxEnrolmentsConnector.allocateEnrolment(
      groupId = groupId,
      credentialId = credentialId,
      vatNumber = vatNumber,
      postcode = knownFacts.businessPostcode,
      vatRegistrationDate = knownFacts.vatRegistrationDate.toTaxEnrolmentsFormat
    )) transform {
      case Right(AllocateEnrolmentResponseHttpParser.EnrolSuccess) =>
        Right(AutoClaimEnrolmentService.EnrolSuccess)
      case Left(AllocateEnrolmentResponseHttpParser.EnrolFailure(message)) =>
        Left(AutoClaimEnrolmentService.EnrolFailure(message))
    }
  }

  private def assignEnrolmentToUser(credentialIds: Set[String], vatNumber: String)
                                   (implicit hc: HeaderCarrier): EitherT[Future, AutoClaimEnrolmentFailure, AutoClaimEnrolmentSuccess] = {
    EitherT(assignEnrolmentToUserService.assignEnrolment(
      userIds = credentialIds,
      vatNumber = vatNumber
    )) transform {
      case Right(AssignEnrolmentToUserService.EnrolmentAssignedToUsers) =>
        Right(AutoClaimEnrolmentService.EnrolmentAssigned)
      case Left(AssignEnrolmentToUserService.EnrolmentAssignmentFailed) =>
        Left(AutoClaimEnrolmentService.EnrolmentAssignmentFailure)
    }
  }

}

object AutoClaimEnrolmentService {

  type AutoClaimEnrolmentResponse = Either[AutoClaimEnrolmentFailure, AutoClaimEnrolmentSuccess]

  sealed trait AutoClaimEnrolmentSuccess

  case object EnrolSuccess extends AutoClaimEnrolmentSuccess

  case object EnrolmentAssigned extends AutoClaimEnrolmentSuccess

  case object EnrolmentAlreadyAllocated extends AutoClaimEnrolmentSuccess

  case class KnownFacts(businessPostcode: String, vatRegistrationDate: String)

  case object UpsertEnrolmentSuccess extends AutoClaimEnrolmentSuccess

  sealed trait AutoClaimEnrolmentFailure

  case object NoUsersFound extends AutoClaimEnrolmentFailure

  case class EnrolFailure(message: String) extends AutoClaimEnrolmentFailure

  case object EnrolmentNotAllocated extends AutoClaimEnrolmentFailure

  case object InvalidVatNumber extends AutoClaimEnrolmentFailure

  case object KnownFactsFailure extends AutoClaimEnrolmentFailure

  case object VatNumberNotFound extends AutoClaimEnrolmentFailure

  case class EnrolmentStoreProxyFailure(status: Int) extends AutoClaimEnrolmentFailure

  case object EnrolmentStoreProxyConnectionFailure extends AutoClaimEnrolmentFailure

  case class UpsertEnrolmentFailure(failureMessage: String) extends AutoClaimEnrolmentFailure

  case object EnrolmentAssignmentFailure extends AutoClaimEnrolmentFailure

}