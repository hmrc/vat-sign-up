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

import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.vatsignup.models.Overseas
import uk.gov.hmrc.vatsignup.repositories.SubscriptionRequestRepository
import uk.gov.hmrc.vatsignup.services.StoreOverseasService._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class StoreOverseasService @Inject()(subscriptionRequestRepository: SubscriptionRequestRepository)(implicit ec: ExecutionContext) {

  def storeOverseas(vatNumber: String)(implicit hc: HeaderCarrier): Future[Either[StoreOverseasFailure, StoreOverseasSuccess.type]] = {

    subscriptionRequestRepository.upsertBusinessEntity(vatNumber, Overseas) map {
      _ => Right(StoreOverseasSuccess)
    } recover {
      case e: NoSuchElementException => Left(OverseasDatabaseFailureNoVATNumber)
      case _ => Left(OverseasDatabaseFailure)
    }
  }

}

object StoreOverseasService {

  case object StoreOverseasSuccess

  sealed trait StoreOverseasFailure

  case object OverseasDatabaseFailure extends StoreOverseasFailure

  case object OverseasDatabaseFailureNoVATNumber extends StoreOverseasFailure

}
