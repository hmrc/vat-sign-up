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
import org.scalatest.{Matchers, WordSpec}
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.Enrolments
import uk.gov.hmrc.vatsignup.config.Constants.EmailVerification.EmailVerifiedKey
import uk.gov.hmrc.vatsignup.connectors.mocks.MockAuthConnector
import uk.gov.hmrc.vatsignup.helpers.TestConstants._
import uk.gov.hmrc.vatsignup.service.mocks.MockStoreEmailService
import uk.gov.hmrc.vatsignup.services.StoreEmailService._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class StoreEmailControllerSpec extends WordSpec
  with Matchers
  with MockAuthConnector
  with MockStoreEmailService {

  object TestStoreEmailController
    extends StoreEmailController(mockAuthConnector, mockStoreEmailService, stubControllerComponents())

  implicit private val system: ActorSystem = ActorSystem()
  implicit private val materializer: ActorMaterializer = ActorMaterializer()

  "storeEmail" when {
    "the e-mail address has been stored correctly" should {
      "return OK with the email verification state" in {
        val emailVerified = true

        mockAuthRetrieveAgentEnrolment()
        mockStoreEmail(testVatNumber, testEmail, Enrolments(Set(testAgentEnrolment)))(Future.successful(Right(StoreEmailSuccess(emailVerified))))

        val request = FakeRequest() withBody testEmail

        val res = TestStoreEmailController.storeEmail(testVatNumber)(request)

        status(res) shouldBe OK
        contentAsJson(res) shouldBe Json.obj(EmailVerifiedKey -> emailVerified)
      }
    }

    "if vat number doesn't exist" should {
      "return NOT_FOUND" in {
        mockAuthRetrieveAgentEnrolment()
        mockStoreEmail(testVatNumber, testEmail, Enrolments(Set(testAgentEnrolment)))(Future.successful(Left(EmailDatabaseFailureNoVATNumber)))

        val request = FakeRequest() withBody testEmail

        val res = TestStoreEmailController.storeEmail(testVatNumber)(request)

        status(res) shouldBe NOT_FOUND
      }
    }

    "the e-mail storage has failed" should {
      "return INTERNAL_SERVER_ERROR" in {
        mockAuthRetrieveAgentEnrolment()
        mockStoreEmail(testVatNumber, testEmail, Enrolments(Set(testAgentEnrolment)))(Future.successful(Left(EmailDatabaseFailure)))

        val request = FakeRequest() withBody testEmail

        val res = TestStoreEmailController.storeEmail(testVatNumber)(request)

        status(res) shouldBe INTERNAL_SERVER_ERROR
      }
    }
  }

}
