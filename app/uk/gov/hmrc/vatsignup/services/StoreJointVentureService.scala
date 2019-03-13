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
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.vatsignup.models.JointVenture
import uk.gov.hmrc.vatsignup.repositories.SubscriptionRequestRepository
import uk.gov.hmrc.vatsignup.services.StoreJointVentureService._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class StoreJointVentureService @Inject()(subscriptionRequestRepository: SubscriptionRequestRepository)(
                                         implicit ec: ExecutionContext
                                        ) {

  def storeJointVenture(vatNumber: String)(
                        implicit hc: HeaderCarrier
                       ): Future[Either[StoreJointVentureFailure, StoreJointVentureSuccess.type]] = {

    subscriptionRequestRepository.upsertBusinessEntity(vatNumber, JointVenture) map {
      _ => Right(StoreJointVentureSuccess)
    } recover {
      case e: NoSuchElementException => Left(JointVentureDatabaseFailureNoVATNumber)
      case _ => Left(JointVentureDatabaseFailure)
    }
  }

}

object StoreJointVentureService {

  case object StoreJointVentureSuccess

  sealed trait StoreJointVentureFailure

  case object JointVentureDatabaseFailure extends StoreJointVentureFailure

  case object JointVentureDatabaseFailureNoVATNumber extends StoreJointVentureFailure

}
