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

import uk.gov.hmrc.vatsignup.models.{NinoSource, SoleTrader}
import uk.gov.hmrc.vatsignup.repositories.SubscriptionRequestRepository

import scala.concurrent.{ExecutionContext, Future}

class StoreNinoWithoutMatchingService(subscriptionRequestRepository: SubscriptionRequestRepository)(implicit ec: ExecutionContext) {

  import StoreNinoWithoutMatchingService._

  def storeNino(vatNumber: String,
                nino: String,
                ninoSource: NinoSource
               ): Future[Either[StoreNinoWithoutMatchingFailure, StoreNinoWithoutMatchingSuccess.type]] = {
    for {
      _ <- subscriptionRequestRepository.upsertBusinessEntity(vatNumber, SoleTrader(nino))
      _ <- subscriptionRequestRepository.upsertNinoSource(vatNumber, ninoSource)
    } yield StoreNinoWithoutMatchingSuccess
  } map {
    _ => Right(StoreNinoWithoutMatchingSuccess)
  } recover {
    case e: NoSuchElementException => Left(DatabaseFailureVatNumberNotFound)
    case _ => Left(DatabaseFailure)
  }

}

object StoreNinoWithoutMatchingService {

  sealed trait StoreNinoWithoutMatchingFailure

  case object StoreNinoWithoutMatchingSuccess

  case object DatabaseFailureVatNumberNotFound extends StoreNinoWithoutMatchingFailure

  case object DatabaseFailure extends StoreNinoWithoutMatchingFailure

}
