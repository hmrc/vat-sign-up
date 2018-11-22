/*
 * Copyright 2018 HM Revenue & Customs
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
import uk.gov.hmrc.vatsignup.models.UnincorporatedAssociation
import uk.gov.hmrc.vatsignup.repositories.SubscriptionRequestRepository
import uk.gov.hmrc.vatsignup.services.StoreUnincorporatedAssociationService._

import scala.concurrent.{ExecutionContext, Future}


@Singleton
class StoreUnincorporatedAssociationService @Inject()(subscriptionRequestRepository: SubscriptionRequestRepository)(
                                                      implicit ec: ExecutionContext
                                                     ) {

  def storeUnincorporatedAssociation(vatNumber: String)(
                                     implicit hc: HeaderCarrier
                                    ): Future[Either[StoreUnincorporatedAssociationFailure, StoreUnincorporatedAssociationSuccess.type]] = {

    subscriptionRequestRepository.upsertBusinessEntity(vatNumber, UnincorporatedAssociation) map {
      _ => Right(StoreUnincorporatedAssociationSuccess)
    } recover {
      case e: NoSuchElementException => Left(UnincorporatedAssociationDatabaseFailureNoVATNumber)
      case _ => Left(UnincorporatedAssociationDatabaseFailure)
    }
  }

}

object StoreUnincorporatedAssociationService {

  case object StoreUnincorporatedAssociationSuccess

  sealed trait StoreUnincorporatedAssociationFailure

  case object UnincorporatedAssociationDatabaseFailure extends StoreUnincorporatedAssociationFailure

  case object UnincorporatedAssociationDatabaseFailureNoVATNumber extends StoreUnincorporatedAssociationFailure

}
