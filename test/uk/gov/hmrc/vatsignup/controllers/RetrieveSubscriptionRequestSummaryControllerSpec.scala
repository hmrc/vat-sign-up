/*
 * Copyright 2020 HM Revenue & Customs
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

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import play.api.test.Helpers._
import play.api.libs.json.Json
import play.api.test.FakeRequest
import org.scalatest.{WordSpec, Matchers}
import uk.gov.hmrc.vatsignup.connectors.mocks.MockAuthConnector
import uk.gov.hmrc.vatsignup.helpers.TestConstants._
import uk.gov.hmrc.vatsignup.models.{Digital, LimitedCompany, SubscriptionRequestSummary}
import uk.gov.hmrc.vatsignup.service.mocks.MockRetrieveSubscriptionRequestSummaryService
import uk.gov.hmrc.vatsignup.services.RetrieveSubscriptionRequestSummaryService.{IncompleteSubscriptionRequest, NoSubscriptionRequestFound}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class RetrieveSubscriptionRequestSummaryControllerSpec extends WordSpec with Matchers
  with MockAuthConnector with MockRetrieveSubscriptionRequestSummaryService {

  object TestRetrieveSubscriptionRequestSummaryController extends RetrieveSubscriptionRequestSummaryController(
    mockAuthConnector,
    mockRetrieveSubscriptionRequestSummaryService,
    stubControllerComponents()
  )

  implicit val system: ActorSystem = ActorSystem()
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  "retrieveSubscriptionnRequestSummary" should {
    "return OK with a completed subscription request summary" when {
      "the RetrieveSubscriptionRequestSummaryService returns a successful subscription request summary" in {
        mockAuthorise()(Future.successful(Unit))

        val testSubscriptionRequestSummary = SubscriptionRequestSummary(
          vatNumber = testVatNumber,
          businessEntity = LimitedCompany(testCompanyNumber),
          optSignUpEmail = Some(testEmail),
          transactionEmail = testEmail,
          contactPreference = Digital
        )

        mockRetrieveSubscriptionRequestSummary(testVatNumber)(Future.successful(Right(testSubscriptionRequestSummary)))

        val res = TestRetrieveSubscriptionRequestSummaryController.retrieveSubscriptionRequestSummary(testVatNumber)(FakeRequest())

        status(res) shouldBe OK
        contentAsJson(res) shouldBe Json.toJson(testSubscriptionRequestSummary)
      }
    }

    "return BAD_REQUEST" when {
      s"the RetrieveSubscriptionRequestSummaryService returns an $IncompleteSubscriptionRequest" in {
        mockAuthorise()(Future.successful(Unit))
        mockRetrieveSubscriptionRequestSummary(testVatNumber)(Future.successful(Left(IncompleteSubscriptionRequest)))

        val res = TestRetrieveSubscriptionRequestSummaryController.retrieveSubscriptionRequestSummary(testVatNumber)(FakeRequest())

        status(res) shouldBe BAD_REQUEST
      }
    }


    "return NOT_FOUND" when {
      s"the RetrieveSubscriptionRequestSummaryService returns an $NoSubscriptionRequestFound" in {
        mockAuthorise()(Future.successful(Unit))
        mockRetrieveSubscriptionRequestSummary(testVatNumber)(Future.successful(Left(NoSubscriptionRequestFound)))

        val res = TestRetrieveSubscriptionRequestSummaryController.retrieveSubscriptionRequestSummary(testVatNumber)(FakeRequest())

        status(res) shouldBe NOT_FOUND
      }
    }
  }

}
