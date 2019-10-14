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

import javax.inject.{Inject, Singleton}
import play.api.mvc.Request
import uk.gov.hmrc.auth.core.Enrolments
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.vatsignup.repositories.SubscriptionRequestRepository
import uk.gov.hmrc.vatsignup.services.StoreMigratedVRNService.{StoreMigratedVRNFailure, _}
import uk.gov.hmrc.vatsignup.utils.EnrolmentUtils._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class StoreMigratedVRNService @Inject()(subscriptionRequestRepository: SubscriptionRequestRepository)(implicit ec: ExecutionContext) {

  def storeVatNumber(vatNumber: String,
                     enrolments: Enrolments
                    )(implicit hc: HeaderCarrier, request: Request[_]): Future[Either[StoreMigratedVRNFailure, StoreMigratedVRNSuccess.type]] = {

    def upsertVatNumber(vatNumber: String) =
      subscriptionRequestRepository.upsertVatNumber(vatNumber, isMigratable = true, isDirectDebit = false) map {
        result =>
          if (result.ok) {
            Right(StoreMigratedVRNSuccess)
          }
          else {
            Left(UpsertMigratedVRNFailure)
          }
      }

    enrolments.vatNumber match {
      case Some(enrolmentVatNumber) if enrolmentVatNumber == vatNumber => upsertVatNumber(vatNumber)
      case Some(_) => Future.successful(Left(DoesNotMatch))
      case None => Future.successful(Left(NoVatEnrolment))
    }
  }
}

object StoreMigratedVRNService {

  case object StoreMigratedVRNSuccess

  trait StoreMigratedVRNFailure

  case object DoesNotMatch extends StoreMigratedVRNFailure

  case object NoVatEnrolment extends StoreMigratedVRNFailure

  case object UpsertMigratedVRNFailure extends StoreMigratedVRNFailure

}
