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
import uk.gov.hmrc.vatsignup.config.Constants._
import uk.gov.hmrc.vatsignup.connectors.AuthenticatorConnector
import uk.gov.hmrc.vatsignup.models.monitoring.UserMatchingAuditing.UserMatchingAuditModel
import uk.gov.hmrc.vatsignup.models._
import uk.gov.hmrc.vatsignup.repositories.SubscriptionRequestRepository
import uk.gov.hmrc.vatsignup.services.monitoring.AuditService

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class StoreNinoService @Inject()(subscriptionRequestRepository: SubscriptionRequestRepository,
                                 authenticatorConnector: AuthenticatorConnector
                                )(implicit ec: ExecutionContext) {

  import StoreNinoService._

  def storeNino(vatNumber: String, nino: String, ninoSource: NinoSource)
                              (implicit hc: HeaderCarrier, request: Request[_]): Future[StoreNinoServiceResponse] = {
    storeNinoToMongo(vatNumber, nino, ninoSource)
  }

  private def storeNinoToMongo(vatNumber: String, nino: String, ninoSource: NinoSource): Future[StoreNinoServiceResponse] = {
    val res = for {
      _ <- subscriptionRequestRepository.upsertBusinessEntity(vatNumber, SoleTrader(nino))
      _ <- subscriptionRequestRepository.upsertNinoSource(vatNumber, ninoSource)
    } yield StoreNinoSuccess

    res map {
      _ => Right(StoreNinoSuccess)
    } recover {
      case e: NoSuchElementException => Left(NinoDatabaseFailureNoVATNumber)
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
