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
import uk.gov.hmrc.vatsignup.models.PartnershipEntityType.{GeneralPartnership, LimitedPartnershipBase}
import uk.gov.hmrc.vatsignup.models.PartnershipInformation
import uk.gov.hmrc.vatsignup.repositories.SubscriptionRequestRepository
import uk.gov.hmrc.vatsignup.services.StorePartnershipInformationService._

import scala.concurrent.{ExecutionContext, Future}


@Singleton
class StorePartnershipInformationService @Inject()(subscriptionRequestRepository: SubscriptionRequestRepository)
                                                  (implicit ec: ExecutionContext) {

  def storePartnershipInformation(vatNumber: String,
                                  partnershipInformation: PartnershipInformation
                                 )(implicit hc: HeaderCarrier): Future[Either[StorePartnershipInformationFailure, StorePartnershipInformationSuccess.type]] = {

    val result = partnershipInformation.partnershipType match {
      case GeneralPartnership =>
        subscriptionRequestRepository.upsertPartnership(
          vatNumber = vatNumber,
          sautr = partnershipInformation.sautr,
          partnershipType = GeneralPartnership
        )
      case limited: LimitedPartnershipBase =>
        subscriptionRequestRepository.upsertPartnershipLimited(
          vatNumber = vatNumber,
          sautr = partnershipInformation.sautr,
          crn = partnershipInformation.crn.getOrElse(""),
          partnershipType = limited
        )
    }
    result map {
      _ => Right(StorePartnershipInformationSuccess)
    } recover {
      case e: NoSuchElementException => Left(PartnershipInformationDatabaseFailureNoVATNumber)
      case _ => Left(PartnershipInformationDatabaseFailure)
    }

  }

}

object StorePartnershipInformationService {

  case object StorePartnershipInformationSuccess

  sealed trait StorePartnershipInformationFailure

  case object PartnershipInformationDatabaseFailureNoVATNumber extends StorePartnershipInformationFailure

  case object PartnershipInformationDatabaseFailure extends StorePartnershipInformationFailure

}
