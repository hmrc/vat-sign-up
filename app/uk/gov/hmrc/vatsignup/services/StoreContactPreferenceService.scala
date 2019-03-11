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

import javax.inject.Inject
import uk.gov.hmrc.vatsignup.models.ContactPreference
import uk.gov.hmrc.vatsignup.repositories.SubscriptionRequestRepository
import uk.gov.hmrc.vatsignup.services.StoreContactPreferenceService._

import scala.concurrent.{ExecutionContext, Future}

class StoreContactPreferenceService @Inject()(subscriptionRequestRepository: SubscriptionRequestRepository
                                             )(implicit ec: ExecutionContext) {
  def storeContactPreference(vatNumber: String,
                             contactPreference: ContactPreference): Future[Either[StoreContactPreferenceFailure, ContactPreferenceStored.type]] =
    subscriptionRequestRepository.upsertContactPreference(vatNumber, contactPreference) map {
      _ => Right(ContactPreferenceStored)
    } recover {
      case e: NoSuchElementException => Left(ContactPreferenceNoVatFound)
      case _ => Left(ContactPreferenceDatabaseFailure)
    }
}

object StoreContactPreferenceService {
  case object ContactPreferenceStored

  sealed trait StoreContactPreferenceFailure

  case object ContactPreferenceDatabaseFailure extends StoreContactPreferenceFailure

  case object ContactPreferenceNoVatFound extends StoreContactPreferenceFailure
}
