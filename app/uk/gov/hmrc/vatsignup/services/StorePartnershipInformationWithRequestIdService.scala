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
import uk.gov.hmrc.vatsignup.models.{PartnershipEntityType, PartnershipInformation}
import uk.gov.hmrc.vatsignup.repositories.UnconfirmedSubscriptionRequestRepository
import uk.gov.hmrc.vatsignup.services.StorePartnershipInformationWithRequestIdService._

import scala.concurrent.{ExecutionContext, Future}


@Singleton
class StorePartnershipInformationWithRequestIdService @Inject()(unconfirmedSubscriptionRequestRepository: UnconfirmedSubscriptionRequestRepository)
                                                               (implicit ec: ExecutionContext) {

  def storePartnershipInformation(vatNumber: String,
                                  partnershipInformation: PartnershipInformation
                         )(implicit hc: HeaderCarrier): Future[Either[StorePartnershipUtrFailure, StorePartnershipUtrSuccess.type]] = {
    unconfirmedSubscriptionRequestRepository.upsertPartnershipUtr(vatNumber, partnershipInformation.partnershipType, partnershipInformation.sautr) map {
      _ => Right(StorePartnershipUtrSuccess)
    } recover {
      case e: NoSuchElementException => Left(PartnershipUtrDatabaseFailureNoVATNumber)
      case _ => Left(PartnershipUtrDatabaseFailure)
    }
  }

}

object StorePartnershipInformationWithRequestIdService {

  case object StorePartnershipUtrSuccess

  sealed trait StorePartnershipUtrFailure

  case object PartnershipUtrDatabaseFailureNoVATNumber extends StorePartnershipUtrFailure

  case object PartnershipUtrDatabaseFailure extends StorePartnershipUtrFailure

}
