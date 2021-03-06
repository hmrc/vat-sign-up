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
import play.api.mvc.Request
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.vatsignup.models._
import uk.gov.hmrc.vatsignup.repositories.SubscriptionRequestRepository

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class StoreNinoService @Inject()(subscriptionRequestRepository: SubscriptionRequestRepository
                                )(implicit ec: ExecutionContext) {

  import StoreNinoService._

  def storeNino(vatNumber: String, nino: String)(implicit hc: HeaderCarrier, request: Request[_]): Future[StoreNinoServiceResponse] = {
    subscriptionRequestRepository.upsertBusinessEntity(vatNumber, SoleTrader(nino)) map {
      _ => Right(StoreNinoSuccess)
    } recover {
      case _: NoSuchElementException => Left(NinoDatabaseFailureNoVATNumber)
      case _ => Left(NinoDatabaseFailure)
    }
  }
}

object StoreNinoService {

  type StoreNinoServiceResponse = Either[StoreNinoFailure, StoreNinoSuccess.type]

  case object StoreNinoSuccess

  sealed trait StoreNinoFailure

  case object NinoDatabaseFailure extends StoreNinoFailure

  case object NinoDatabaseFailureNoVATNumber extends StoreNinoFailure

}
