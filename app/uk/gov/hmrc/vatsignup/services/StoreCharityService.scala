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
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.vatsignup.models.Charity
import uk.gov.hmrc.vatsignup.repositories.SubscriptionRequestRepository
import uk.gov.hmrc.vatsignup.services.StoreCharityService._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class StoreCharityService @Inject()(subscriptionRequestRepository: SubscriptionRequestRepository)(
                                     implicit ec: ExecutionContext
                                   ) {

  def storeCharity(vatNumber: String)(
                   implicit hc: HeaderCarrier
                  ): Future[Either[StoreCharityFailure, StoreCharitySuccess.type]] = {

    subscriptionRequestRepository.upsertBusinessEntity(vatNumber, Charity) map {
      _ => Right(StoreCharitySuccess)
    } recover {
      case e: NoSuchElementException => Left(CharityDatabaseFailureNoVATNumber)
      case _ => Left(CharityDatabaseFailure)
    }
  }

}

object StoreCharityService {

  case object StoreCharitySuccess

  sealed trait StoreCharityFailure

  case object CharityDatabaseFailure extends StoreCharityFailure

  case object CharityDatabaseFailureNoVATNumber extends StoreCharityFailure

}
