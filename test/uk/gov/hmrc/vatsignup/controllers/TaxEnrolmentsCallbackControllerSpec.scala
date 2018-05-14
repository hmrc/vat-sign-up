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

import play.api.http.Status._
import play.api.libs.json.{JsValue, Json}
import play.api.test.FakeRequest
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.vatsignup.helpers.TestConstants.testVatNumber
import uk.gov.hmrc.vatsignup.models.SubscriptionState._
import uk.gov.hmrc.vatsignup.service.mocks.MockSubscriptionNotificationService
import uk.gov.hmrc.vatsignup.services.SubscriptionNotificationService._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class TaxEnrolmentsCallbackControllerSpec extends UnitSpec with MockSubscriptionNotificationService {

  object TestTaxEnrolmentsCallbackController
    extends TaxEnrolmentsCallbackController(mockSubscriptionNotificationService)

  def testRequest(subscriptionState: String): FakeRequest[JsValue] =
    FakeRequest().withBody(Json.obj(TestTaxEnrolmentsCallbackController.stateKey -> subscriptionState))

  "taxEnrolmentsCallback" when {
    "the subscription notification service returns NotificationSent" should {
      "return NO_CONTENT" in {
        mockSendEmailNotification(testVatNumber, Success)(Future.successful(Right(NotificationSent)))

        val res = await(TestTaxEnrolmentsCallbackController.taxEnrolmentsCallback(testVatNumber)(testRequest(Success.jsonName)))
        status(res) shouldBe NO_CONTENT
      }
    }
    "the subscription notification service returns EmailRequestDataNotFound" should {
      "return PRECONDITION_FAILED" in {
        mockSendEmailNotification(testVatNumber, Success)(Future.successful(Left(EmailRequestDataNotFound)))

        val res = await(TestTaxEnrolmentsCallbackController.taxEnrolmentsCallback(testVatNumber)(testRequest(Success.jsonName)))
        status(res) shouldBe PRECONDITION_FAILED
      }
    }

    "the subscription notification service returns EmailServiceFailure" should {
      "return PRECONDITION_FAILED" in {
        mockSendEmailNotification(testVatNumber, Success)(Future.successful(Left(EmailServiceFailure)))

        val res = await(TestTaxEnrolmentsCallbackController.taxEnrolmentsCallback(testVatNumber)(testRequest(Success.jsonName)))
        status(res) shouldBe BAD_GATEWAY
      }
    }

    "the subscription notification service returns DelegatedSubscription" should {
      "return PRECONDITION_FAILED" in {
        mockSendEmailNotification(testVatNumber, Success)(Future.successful(Left(DelegatedSubscription)))

        val res = await(TestTaxEnrolmentsCallbackController.taxEnrolmentsCallback(testVatNumber)(testRequest(Success.jsonName)))
        status(res) shouldBe NO_CONTENT
      }
    }
  }

}
