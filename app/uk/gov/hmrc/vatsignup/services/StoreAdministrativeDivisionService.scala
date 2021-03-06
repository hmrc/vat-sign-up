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
import uk.gov.hmrc.vatsignup.models.AdministrativeDivision
import uk.gov.hmrc.vatsignup.repositories.SubscriptionRequestRepository
import uk.gov.hmrc.vatsignup.services.StoreAdministrativeDivisionService._

import scala.concurrent.{ExecutionContext, Future}


@Singleton
class StoreAdministrativeDivisionService @Inject()(subscriptionRequestRepository: SubscriptionRequestRepository)
                                                  (implicit ec: ExecutionContext) {

  def storeAdministrativeDivision(vatNumber: String)
                                 (implicit hc: HeaderCarrier): Future[Either[StoreAdministrativeDivisionFailure, StoreAdministrativeDivisionSuccess.type]] = {
    subscriptionRequestRepository.upsertBusinessEntity(vatNumber, AdministrativeDivision) map {
      _ => Right(StoreAdministrativeDivisionSuccess)
    } recover {
      case e: NoSuchElementException => Left(AdministrativeDivisionDatabaseFailureNoVATNumber)
      case _ => Left(AdministrativeDivisionDatabaseFailure)
    }
  }

}

object StoreAdministrativeDivisionService {

  case object StoreAdministrativeDivisionSuccess

  sealed trait StoreAdministrativeDivisionFailure

  case object AdministrativeDivisionDatabaseFailure extends StoreAdministrativeDivisionFailure

  case object AdministrativeDivisionDatabaseFailureNoVATNumber extends StoreAdministrativeDivisionFailure

}

