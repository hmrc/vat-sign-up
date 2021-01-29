/*
 * Copyright 2021 HM Revenue & Customs
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

import javax.inject.{Inject, Singleton}
import reactivemongo.api.commands.WriteResult
import uk.gov.hmrc.auth.core.Enrolments
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException, NotFoundException, UnprocessableEntityException}
import uk.gov.hmrc.vatsignup.models._
import uk.gov.hmrc.vatsignup.repositories.SubscriptionRequestRepository
import uk.gov.hmrc.vatsignup.services.MigratedSignUpRequestService._
import uk.gov.hmrc.vatsignup.utils.EnrolmentUtils._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class MigratedSignUpRequestService @Inject()(subscriptionRequestRepository: SubscriptionRequestRepository)
                                            (implicit ec: ExecutionContext) {

  def getSignUpRequest(vatNumber: String, enrolments: Enrolments)
                      (implicit hc: HeaderCarrier): Future[MigratedSignUpRequest] = {

    val isDelegated = enrolments.agentReferenceNumber.isDefined

    subscriptionRequestRepository.findById(vatNumber) map {
      case Some(subscriptionRequest) =>
        subscriptionRequest.businessEntity match {
          case Some(entity) =>
            MigratedSignUpRequest(
              vatNumber = vatNumber,
              businessEntity = entity,
              isDelegated = isDelegated,
              isMigratable = subscriptionRequest.isMigratable
            )
          case _ =>
            throw new UnprocessableEntityException(
              s"[MigratedSignUpRequestService] Subscription request for VAT number $vatNumber did not contain Business Entity"
            )
        }
      case None =>
        throw new NotFoundException(
          s"[MigratedSignUpRequestService] Subscription request not found for VAT number $vatNumber"
        )
      case _ =>
        throw new InternalServerException(
          s"[MigratedSignUpRequestService] Database failure when retrieving MigratedSignUpRequest for $vatNumber"
        )
    }
  }

  def deleteSignUpRequest(vatNumber: String)
                         (implicit hc: HeaderCarrier): Future[SignUpRequestDeleted.type] =

    subscriptionRequestRepository.deleteRecord(vatNumber) map {
      case _: WriteResult =>
        SignUpRequestDeleted
      case _ =>
        throw new InternalServerException("Database failure: Failed to delete MigratedSignUpRequest")
    }
}

object MigratedSignUpRequestService {

  type MigratedSignUpRequestResponse = Either[SignUpRequestFailure, SubscriptionRequest]

  case object SignUpRequestDeleted

  case object RequestAuthorised

  sealed trait SignUpRequestFailure

  case object SignUpRequestNotFound extends SignUpRequestFailure

  case object InsufficientData extends SignUpRequestFailure

}
