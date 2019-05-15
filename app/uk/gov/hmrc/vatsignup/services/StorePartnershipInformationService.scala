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
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.vatsignup.config.featureswitch.{FeatureSwitching, SkipPartnershipKnownFactsMismatch}
import uk.gov.hmrc.vatsignup.models.PartnershipBusinessEntity
import uk.gov.hmrc.vatsignup.repositories.SubscriptionRequestRepository
import uk.gov.hmrc.vatsignup.services.StorePartnershipInformationService._

import scala.concurrent.{ExecutionContext, Future}


@Singleton
class StorePartnershipInformationService @Inject()(subscriptionRequestRepository: SubscriptionRequestRepository,
                                                   partnershipKnownFactsService: PartnershipKnownFactsService
                                                  )(implicit ec: ExecutionContext) extends FeatureSwitching {

  def storePartnershipInformationWithEnrolment(vatNumber: String,
                                               partnershipInformation: PartnershipBusinessEntity,
                                               enrolmentSautr: String
                                              )(implicit hc: HeaderCarrier): Future[StorePartnershipResponse] = {
    if (partnershipInformation.sautr contains enrolmentSautr) {
      storePartnershipInformationCore(vatNumber, partnershipInformation)
    } else {
      Future.successful(Left(EnrolmentMatchFailure))
    }
  }

  def storePartnershipInformation(vatNumber: String,
                                  partnershipInformation: PartnershipBusinessEntity,
                                  businessPostcode: Option[String]
                                 )(implicit hc: HeaderCarrier, request: Request[_]): Future[StorePartnershipResponse] = {

    (partnershipInformation.sautr, businessPostcode) match {
      case (Some(sautr), Some(postCode)) =>
        partnershipKnownFactsService.checkKnownFactsMatch(vatNumber, sautr, postCode) flatMap {
          case Right(_) =>
            storePartnershipInformationCore(vatNumber, partnershipInformation)
          case Left(PartnershipKnownFactsService.NoPostCodesReturned) if isEnabled(SkipPartnershipKnownFactsMismatch) =>
            storePartnershipInformationCore(vatNumber, PartnershipBusinessEntity.copyWithoutSautr(partnershipInformation))
          case Left(PartnershipKnownFactsService.NoPostCodesReturned) =>
            Future.successful(Left(InsufficientData))
          case Left(PartnershipKnownFactsService.PostCodeDoesNotMatch) =>
            Future.successful(Left(KnownFactsMismatch))
          case Left(PartnershipKnownFactsService.InvalidSautr) =>
            Future.successful(Left(InvalidSautr))
          case _ =>
            Future.successful(Left(GetPartnershipKnownFactsFailure))
        }
      case (None, None) =>
        storePartnershipInformationCore(vatNumber, partnershipInformation)
      case _ =>
        Future.successful(Left(InsufficientData))
    }
  }

  private def storePartnershipInformationCore(vatNumber: String,
                                              partnership: PartnershipBusinessEntity
                                             )(implicit hc: HeaderCarrier): Future[StorePartnershipResponse] = {
    subscriptionRequestRepository.upsertBusinessEntity(vatNumber, partnership) map {
      _ => Right(StorePartnershipInformationSuccess)
    } recover {
      case e: NoSuchElementException => Left(PartnershipInformationDatabaseFailureNoVATNumber)
      case _ => Left(PartnershipInformationDatabaseFailure)
    }

  }

}

object StorePartnershipInformationService {

  type StorePartnershipResponse = Either[StorePartnershipInformationFailure, StorePartnershipInformationSuccess]

  sealed trait StorePartnershipInformationSuccess

  case object StorePartnershipInformationSuccess extends StorePartnershipInformationSuccess

  case object NoSaUtrProvided extends StorePartnershipInformationSuccess

  sealed trait StorePartnershipInformationFailure

  case object PartnershipInformationDatabaseFailureNoVATNumber extends StorePartnershipInformationFailure

  case object PartnershipInformationDatabaseFailure extends StorePartnershipInformationFailure

  case object EnrolmentMatchFailure extends StorePartnershipInformationFailure

  case object KnownFactsMismatch extends StorePartnershipInformationFailure

  case object InvalidSautr extends StorePartnershipInformationFailure

  case object InsufficientData extends StorePartnershipInformationFailure

  case object GetPartnershipKnownFactsFailure extends StorePartnershipInformationFailure

}
