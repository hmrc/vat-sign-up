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

package uk.gov.hmrc.vatsignup.controllers

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import org.scalatest.{Matchers, WordSpec}
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.vatsignup.connectors.mocks.MockAuthConnector
import uk.gov.hmrc.vatsignup.controllers.NewStoreTransactionEmailController.reasonKey
import uk.gov.hmrc.vatsignup.helpers.TestConstants._
import uk.gov.hmrc.vatsignup.models.StoreTransactionEmailRequest
import uk.gov.hmrc.vatsignup.service.mocks.MockNewStoreEmailService
import uk.gov.hmrc.vatsignup.services.NewStoreEmailService._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class NewStoreTransactionEmailControllerSpec extends WordSpec with Matchers with MockAuthConnector with MockNewStoreEmailService {

  object TestController extends NewStoreTransactionEmailController(
    mockAuthConnector,
    mockNewStoreEmailService,
    stubControllerComponents()
  )

  val testRequest: StoreTransactionEmailRequest = StoreTransactionEmailRequest(
    testEmail,
    testPasscode
  )

  val testReason = "testReason"

  implicit private val system: ActorSystem = ActorSystem()
  implicit private val materializer: ActorMaterializer = ActorMaterializer()

  "storeTransactionEmail" should {
    "return OK when email successfully verified" in {
      mockAuthorise()(Future.successful(Unit))

      mockStoreTransactionEmail(testVatNumber, testRequest)(Future.successful(Right(StoreEmailSuccess)))

      val request = FakeRequest().withBody(testRequest)

      val res = TestController.storeTransactionEmail(testVatNumber)(request)

      status(res) shouldBe CREATED
    }

    "if vat number doesn't exist" should {
      "return NOT_FOUND" in {
        mockAuthorise()(Future.successful(Unit))

        mockStoreTransactionEmail(testVatNumber, testRequest)(Future.successful(Left(EmailDatabaseFailureNoVATNumber)))

        val request = FakeRequest().withBody(testRequest)

        val res = TestController.storeTransactionEmail(testVatNumber)(request)

        status(res) shouldBe NOT_FOUND
      }
    }

    "the e-mail storage has failed" should {
      "return INTERNAL_SERVER_ERROR" in {
        mockAuthorise()(Future.successful(Unit))

        mockStoreTransactionEmail(testVatNumber, testRequest)(Future.successful(Left(EmailDatabaseFailure)))

        val request = FakeRequest().withBody(testRequest)

        val res = TestController.storeTransactionEmail(testVatNumber)(request)

        status(res) shouldBe INTERNAL_SERVER_ERROR
      }
    }

    "the call to email verification has failed" should {
      "return BAD_GATEWAY with a reason" in {
        mockAuthorise()(Future.successful(Unit))

        mockStoreTransactionEmail(testVatNumber, testRequest)(Future.successful(Left(EmailVerificationFailure(testReason))))

        val request = FakeRequest().withBody(testRequest)

        val res = TestController.storeTransactionEmail(testVatNumber)(request)

        status(res) shouldBe BAD_GATEWAY
        contentAsJson(res) shouldBe Json.obj(reasonKey -> testReason)
      }
    }
  }
}
