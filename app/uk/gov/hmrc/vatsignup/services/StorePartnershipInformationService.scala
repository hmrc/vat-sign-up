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

import cats.data.EitherT
import cats.implicits._
import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.vatsignup.models.PartnershipBusinessEntity
import uk.gov.hmrc.vatsignup.repositories.SubscriptionRequestRepository
import uk.gov.hmrc.vatsignup.services.StorePartnershipInformationService._

import scala.concurrent.{ExecutionContext, Future}


@Singleton
class StorePartnershipInformationService @Inject()(subscriptionRequestRepository: SubscriptionRequestRepository,
                                                   partnershipKnownFactsService: PartnershipKnownFactsService
                                                  )
                                                  (implicit ec: ExecutionContext) {

  def storePartnershipInformationWithEnrolment(vatNumber: String,
                                               partnershipInformation: PartnershipBusinessEntity,
                                               enrolmentSautr: String
                                              )(implicit hc: HeaderCarrier): Future[Either[StorePartnershipInformationFailure, StorePartnershipInformationSuccess.type]] = {
    if (enrolmentSautr == partnershipInformation.sautr) {
      storePartnershipInformationCore(vatNumber, partnershipInformation)
    } else {
      Future.successful(Left(EnrolmentMatchFailure))
    }
  }

  def storePartnershipInformation(vatNumber: String,
                                  partnershipInformation: PartnershipBusinessEntity,
                                  businessPostcode: String
                                 )(implicit hc: HeaderCarrier): Future[Either[StorePartnershipInformationFailure, StorePartnershipInformationSuccess.type]] = {
    for {
      _ <- EitherT(partnershipKnownFactsService.checkKnownFactsMatch(partnershipInformation.sautr, businessPostcode)) leftMap {
        case PartnershipKnownFactsService.PostCodeDoesNotMatch =>
          KnownFactsMismatch
        case PartnershipKnownFactsService.NoPostCodesReturned =>
          InsufficientData
        case PartnershipKnownFactsService.InvalidSautr =>
          InvalidSautr
        case _ =>
          GetPartnershipKnownFactsFailure
      }
      _ <- EitherT(storePartnershipInformationCore(vatNumber, partnershipInformation)
      )
    } yield StorePartnershipInformationSuccess
  }.value

  private def storePartnershipInformationCore(vatNumber: String,
                                              partnership: PartnershipBusinessEntity
                                             )(implicit hc: HeaderCarrier)
  : Future[Either[StorePartnershipInformationFailure, StorePartnershipInformationSuccess.type]] = {
    subscriptionRequestRepository.upsertBusinessEntity(vatNumber, partnership) map {
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

  case object EnrolmentMatchFailure extends StorePartnershipInformationFailure

  case object KnownFactsMismatch extends StorePartnershipInformationFailure

  case object InvalidSautr extends StorePartnershipInformationFailure

  case object InsufficientData extends StorePartnershipInformationFailure

  case object GetPartnershipKnownFactsFailure extends StorePartnershipInformationFailure

}
