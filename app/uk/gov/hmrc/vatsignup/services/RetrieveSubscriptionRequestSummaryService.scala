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

import javax.inject.Inject
import uk.gov.hmrc.vatsignup.models.{SubscriptionRequest, SubscriptionRequestSummary}
import uk.gov.hmrc.vatsignup.repositories.SubscriptionRequestRepository
import uk.gov.hmrc.vatsignup.services.RetrieveSubscriptionRequestSummaryService._

import scala.concurrent.{ExecutionContext, Future}

class RetrieveSubscriptionRequestSummaryService @Inject()(subscriptionRequestRepository: SubscriptionRequestRepository)(implicit ec: ExecutionContext) {
  def retrieveSubscriptionRequestSummary(vatNumber: String): Future[RetrieveSubscriptionRequestSummaryResponse] = {
    subscriptionRequestRepository.findById(vatNumber) map {
      case Some(SubscriptionRequest(_, _, Some(businessEntity), optSignUpEmail, Some(transactionEmail), _, _, _, Some(contactPreference))) =>
        Right(SubscriptionRequestSummary(vatNumber, businessEntity, optSignUpEmail, transactionEmail, contactPreference))
      case Some(_) =>
        Left(IncompleteSubscriptionRequest)
      case None =>
        Left(NoSubscriptionRequestFound)
    }
  }
}

object RetrieveSubscriptionRequestSummaryService {
  type RetrieveSubscriptionRequestSummaryResponse = Either[RetrieveSubscriptionRequestSummaryFailure, SubscriptionRequestSummary]

  sealed trait RetrieveSubscriptionRequestSummaryFailure

  case object IncompleteSubscriptionRequest extends RetrieveSubscriptionRequestSummaryFailure

  case object NoSubscriptionRequestFound extends RetrieveSubscriptionRequestSummaryFailure

}
