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

package uk.gov.hmrc.vatsignup.service

import play.api.test.Helpers._
import org.scalatest.{WordSpec, Matchers}
import uk.gov.hmrc.vatsignup.repositories.mocks.MockSubscriptionRequestRepository
import uk.gov.hmrc.vatsignup.helpers.TestConstants._
import uk.gov.hmrc.vatsignup.models.{Digital, LimitedCompany, SubscriptionRequest, SubscriptionRequestSummary}
import uk.gov.hmrc.vatsignup.services.RetrieveSubscriptionRequestSummaryService
import uk.gov.hmrc.vatsignup.services.RetrieveSubscriptionRequestSummaryService.{IncompleteSubscriptionRequest, NoSubscriptionRequestFound}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class RetrieveSubscriptionRequestSummaryServiceSpec extends WordSpec with Matchers
  with MockSubscriptionRequestRepository {

  object TestRetrieveSubscriptionRequestSummaryService extends RetrieveSubscriptionRequestSummaryService(mockSubscriptionRequestRepository)

  "retrieveSubscriptionRequestSummary" should {
    "return a subscription request summary" when {
      "the database holds a full subscription request" in {
        val testSubscriptionRequest = SubscriptionRequest(
          vatNumber = testVatNumber,
          businessEntity = Some(LimitedCompany(testCompanyNumber)),
          email = Some(testEmail),
          transactionEmail = Some(testEmail),
          contactPreference = Some(Digital),
          isDirectDebit = false
        )

        mockFindById(testVatNumber)(Future.successful(Some(testSubscriptionRequest)))

        val res = await(TestRetrieveSubscriptionRequestSummaryService.retrieveSubscriptionRequestSummary(testVatNumber))

        val expectedSubscriptionRequestSummary = SubscriptionRequestSummary(
          vatNumber = testVatNumber,
          businessEntity = LimitedCompany(testCompanyNumber),
          optSignUpEmail = Some(testEmail),
          transactionEmail = testEmail,
          contactPreference = Digital
        )

        res shouldBe Right(expectedSubscriptionRequestSummary)
      }
    }
    s"return an $IncompleteSubscriptionRequest" when {
      "any required data is missing" in {
        val testSubscriptionRequest = SubscriptionRequest(
          vatNumber = testVatNumber,
          businessEntity = Some(LimitedCompany(testCompanyNumber)),
          email = Some(testEmail),
          transactionEmail = Some(testEmail),
          contactPreference = None,
          isDirectDebit = false
        )

        mockFindById(testVatNumber)(Future.successful(Some(testSubscriptionRequest)))

        val res = await(TestRetrieveSubscriptionRequestSummaryService.retrieveSubscriptionRequestSummary(testVatNumber))

        res shouldBe Left(IncompleteSubscriptionRequest)
      }
      "there is no subscription request for the VAT number" in {
        mockFindById(testVatNumber)(Future.successful(None))

        val res = await(TestRetrieveSubscriptionRequestSummaryService.retrieveSubscriptionRequestSummary(testVatNumber))

        res shouldBe Left(NoSubscriptionRequestFound)
      }
    }
  }
}


