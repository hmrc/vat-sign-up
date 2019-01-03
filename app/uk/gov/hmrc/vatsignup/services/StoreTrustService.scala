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
import uk.gov.hmrc.vatsignup.models.Trust
import uk.gov.hmrc.vatsignup.repositories.SubscriptionRequestRepository
import uk.gov.hmrc.vatsignup.services.StoreTrustService.{StoreTrustFailure, StoreTrustSuccess, TrustDatabaseFailure, TrustDatabaseFailureNoVATNumber}

import scala.concurrent.{ExecutionContext, Future}


@Singleton
class StoreTrustService @Inject()(subscriptionRequestRepository: SubscriptionRequestRepository)(
  implicit ec: ExecutionContext
) {

  def storeTrust(vatNumber: String)(
    implicit hc: HeaderCarrier
  ): Future[Either[StoreTrustFailure, StoreTrustSuccess.type]] = {

    subscriptionRequestRepository.upsertBusinessEntity(vatNumber, Trust) map {
      _ => Right(StoreTrustSuccess)
    } recover {
      case e: NoSuchElementException => Left(TrustDatabaseFailureNoVATNumber)
      case _ => Left(TrustDatabaseFailure)
    }
  }
}

object StoreTrustService {

  case object StoreTrustSuccess

  sealed trait StoreTrustFailure

  case object TrustDatabaseFailure extends StoreTrustFailure

  case object TrustDatabaseFailureNoVATNumber extends StoreTrustFailure

}

