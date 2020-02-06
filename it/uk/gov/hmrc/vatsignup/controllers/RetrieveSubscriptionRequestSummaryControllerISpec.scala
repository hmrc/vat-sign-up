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

package uk.gov.hmrc.vatsignup.controllers

import play.api.test.Helpers._
import uk.gov.hmrc.vatsignup.helpers.IntegrationTestConstants._
import uk.gov.hmrc.vatsignup.helpers._
import uk.gov.hmrc.vatsignup.helpers.servicemocks.AuthStub._
import uk.gov.hmrc.vatsignup.models._

import scala.concurrent.ExecutionContext.Implicits.global

class RetrieveSubscriptionRequestSummaryControllerISpec extends ComponentSpecBase with CustomMatchers with TestSubmissionRequestRepository {

  "GET /submission-request/vat-number/:vrn" should {
    "return OK with a completed JSON model" when {
      "the database successfully returns a subscription request model" in {
        stubAuth(OK, successfulAuthResponse())

        val testSubscriptionRequest = SubscriptionRequest(
          vatNumber = testVatNumber,
          businessEntity = Some(LimitedCompany(testCompanyNumber)),
          email = Some(testEmail),
          transactionEmail = Some(testEmail),
          contactPreference = Some(Digital),
          isDirectDebit = false
        )

        await(submissionRequestRepo.insert(testSubscriptionRequest))

        val res = get(s"/subscription-request/vat-number/$testVatNumber")

        val expectedSubscriptionRequestSummary = SubscriptionRequestSummary(
          vatNumber = testVatNumber,
          businessEntity = LimitedCompany(testCompanyNumber),
          optSignUpEmail = Some(testEmail),
          transactionEmail = testEmail,
          contactPreference = Digital
        )

        res should have(
          httpStatus(OK),
          jsonBodyAs(expectedSubscriptionRequestSummary)
        )
      }
    }

    "return BAD_REQUEST" when {
      "the database returns an incomplete subscription request model" in {
        stubAuth(OK, successfulAuthResponse())

        val testSubscriptionRequest = SubscriptionRequest(
          vatNumber = testVatNumber,
          businessEntity = Some(LimitedCompany(testCompanyNumber)),
          email = Some(testEmail),
          transactionEmail = Some(testEmail),
          contactPreference = None,
          isDirectDebit = false
        )

        await(submissionRequestRepo.insert(testSubscriptionRequest))

        val res = get(s"/subscription-request/vat-number/$testVatNumber")

        res should have(
          httpStatus(BAD_REQUEST)
        )
      }
    }

    "return NOT_FOUND" when {
      "the database does not contain a subscription request for the supplied vat number" in {
        stubAuth(OK, successfulAuthResponse())

        val res = get(s"/subscription-request/vat-number/$testVatNumber")

        res should have(
          httpStatus(NOT_FOUND)
        )
      }
    }
  }

}
