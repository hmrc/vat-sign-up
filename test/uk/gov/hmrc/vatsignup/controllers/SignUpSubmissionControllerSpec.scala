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

import play.api.test.Helpers._
import play.api.test.FakeRequest
import uk.gov.hmrc.auth.core.Enrolments
import org.scalatest.{WordSpec, Matchers}
import uk.gov.hmrc.vatsignup.connectors.mocks.MockAuthConnector
import uk.gov.hmrc.vatsignup.helpers.TestConstants._
import uk.gov.hmrc.vatsignup.service.mocks.MockSubmissionOrchestrationService
import uk.gov.hmrc.vatsignup.services.SubmissionOrchestrationService._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class SignUpSubmissionControllerSpec extends WordSpec with Matchers
  with MockAuthConnector with MockSubmissionOrchestrationService {

  object TestSignUpSubmissionController extends SignUpSubmissionController(
    mockAuthConnector,
    mockSubmissionOrchestrationService,
    stubControllerComponents()
  )

  "submitSignUpRequest" when {
    "the user is a delegate and" when {
      val enrolments = Enrolments(Set(testAgentEnrolment))
      "the submission orchestration service returns a success" should {
        "return NO_CONTENT" in {
          mockAuthRetrieveAgentEnrolment()
          mockSubmitSignUpRequest(testVatNumber, enrolments)(Future.successful(Right(SignUpRequestSubmitted)))

          val res = TestSignUpSubmissionController.submitSignUpRequest(testVatNumber)(FakeRequest())

          status(res) shouldBe NO_CONTENT
        }
      }
      "the submission orchestration service returns InsufficientData" should {
        "return BAD_REQUEST" in {
          mockAuthRetrieveAgentEnrolment()
          mockSubmitSignUpRequest(testVatNumber, enrolments)(Future.successful(Left(InsufficientData)))

          val res = TestSignUpSubmissionController.submitSignUpRequest(testVatNumber)(FakeRequest())

          status(res) shouldBe BAD_REQUEST
        }
      }
      "the submission orchestration service returns any other error" should {
        "return BAD_GATEWAY" in {
          mockAuthRetrieveAgentEnrolment()
          mockSubmitSignUpRequest(testVatNumber, enrolments)(Future.successful(Left(EmailVerificationFailure)))

          val res = TestSignUpSubmissionController.submitSignUpRequest(testVatNumber)(FakeRequest())

          status(res) shouldBe BAD_GATEWAY
        }
      }
      "the submission orchestration service throws an exception" should {
        "return the exception" in {
          mockAuthRetrieveAgentEnrolment()

          val testException = new Exception()
          mockSubmitSignUpRequest(testVatNumber, enrolments)(Future.failed(testException))

          val res = TestSignUpSubmissionController.submitSignUpRequest(testVatNumber)(FakeRequest())

          intercept[Exception](status(res)) shouldBe testException
        }
      }
    }
    "the user is principal and" when {
      val enrolments = Enrolments(Set(testPrincipalEnrolment))
      "the submission orchestration service returns a success" should {
        "return NO_CONTENT" in {
          mockAuthRetrievePrincipalEnrolment()
          mockSubmitSignUpRequest(testVatNumber, enrolments)(Future.successful(Right(SignUpRequestSubmitted)))

          val res = TestSignUpSubmissionController.submitSignUpRequest(testVatNumber)(FakeRequest())

          status(res) shouldBe NO_CONTENT
        }
      }
      "the submission orchestration service returns InsufficientData" should {
        "return BAD_REQUEST" in {
          mockAuthRetrievePrincipalEnrolment()
          mockSubmitSignUpRequest(testVatNumber, enrolments)(Future.successful(Left(InsufficientData)))

          val res = TestSignUpSubmissionController.submitSignUpRequest(testVatNumber)(FakeRequest())

          status(res) shouldBe BAD_REQUEST
        }
      }
      "the submission orchestration service returns any other error" should {
        "return BAD_GATEWAY" in {
          mockAuthRetrievePrincipalEnrolment()
          mockSubmitSignUpRequest(testVatNumber, enrolments)(Future.successful(Left(EmailVerificationFailure)))

          val res = TestSignUpSubmissionController.submitSignUpRequest(testVatNumber)(FakeRequest())

          status(res) shouldBe BAD_GATEWAY
        }
      }
      "the submission orchestration service throws an exception" should {
        "return the exception" in {
          mockAuthRetrievePrincipalEnrolment()

          val testException = new Exception()
          mockSubmitSignUpRequest(testVatNumber, enrolments)(Future.failed(testException))

          val res = TestSignUpSubmissionController.submitSignUpRequest(testVatNumber)(FakeRequest())

          intercept[Exception](status(res)) shouldBe testException
        }
      }
    }
  }

}
