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

package uk.gov.hmrc.vatsignup.service

import org.scalatest.EitherValues
import play.api.test.FakeRequest
import reactivemongo.api.commands.{UpdateWriteResult, WriteResult}
import uk.gov.hmrc.auth.core.Enrolments
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.vatsignup.helpers.TestConstants._
import uk.gov.hmrc.vatsignup.models.SignUpRequest
import uk.gov.hmrc.vatsignup.repositories.mocks.{MockEmailRequestRepository, MockSubscriptionRequestRepository}
import uk.gov.hmrc.vatsignup.service.mocks.{MockSignUpRequestService, MockSubmissionService}
import uk.gov.hmrc.vatsignup.services.SubmissionOrchestrationService
import uk.gov.hmrc.vatsignup.services.SubmissionOrchestrationService._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class SubmissionOrchestratorServiceSpec extends UnitSpec with EitherValues
  with MockSubscriptionRequestRepository with MockEmailRequestRepository with MockSubmissionService with MockSignUpRequestService {

  object TestSubmissionOrchestrationService extends SubmissionOrchestrationService(
    mockSignUpRequestService,
    mockSubmissionService,
    mockSubscriptionRequestRepository,
    mockEmailRequestRepository
  )

  private implicit val hc: HeaderCarrier = HeaderCarrier()
  private implicit val request = FakeRequest("POST", "testUrl")

  val testSignUpRequest = SignUpRequest(
    vatNumber = testVatNumber,
    businessEntity = testSoleTrader,
    signUpEmail = Some(testSignUpEmail),
    transactionEmail = testSignUpEmail,
    isDelegated = true,
    isMigratable = true,
    contactPreference = Some(testContactPreference)
  )
  val enrolments = Enrolments(Set(testAgentEnrolment))

  "submitSignUpRequest" when {
    "getSignUpRequest returns a SignUpRequest" when {
      "submit sign up request was successful" when {
        "upsert email was successful" when {
          "delete record request was successful" when {
            "return SignUpRequestSubmitted" in {

              mockGetSignUpRequest(testVatNumber, enrolments)(Future.successful(Right(testSignUpRequest)))
              mockSubmitSignUpRequestSuccessful(testSignUpRequest, enrolments)
              mockUpsertEmailAfterSubscription(testVatNumber, testEmail, isDelegated = true)(mock[UpdateWriteResult])
              mockDeleteRecord(testVatNumber)(mock[WriteResult])

              val res = await(TestSubmissionOrchestrationService.submitSignUpRequest(testVatNumber, enrolments))

              res.right.value shouldBe SignUpRequestSubmitted
            }
          }
          "delete record request failed" when {
            "return DeleteRecordFailure" in {

              mockGetSignUpRequest(testVatNumber, enrolments)(Future.successful(Right(testSignUpRequest)))
              mockSubmitSignUpRequestSuccessful(testSignUpRequest, enrolments)
              mockUpsertEmailAfterSubscription(testVatNumber, testEmail, isDelegated = true)(mock[UpdateWriteResult])
              mockDeleteRecord(testVatNumber)(Future.failed(new Exception))

              val res = await(TestSubmissionOrchestrationService.submitSignUpRequest(testVatNumber, enrolments))

              res.left.value shouldBe DeleteRecordFailure
            }
          }
        }
        "upsert email failed" should {
          "return DeleteRecordFailure" in {

            mockGetSignUpRequest(testVatNumber, enrolments)(Future.successful(Right(testSignUpRequest)))
            mockSubmitSignUpRequestSuccessful(testSignUpRequest, enrolments)
            mockUpsertEmailAfterSubscription(testVatNumber, testEmail, isDelegated = true)(Future.failed(new Exception))

            val res = await(TestSubmissionOrchestrationService.submitSignUpRequest(testVatNumber, enrolments))

            res.left.value shouldBe DeleteRecordFailure
          }
        }
      }
      "the submit sign up request failed" when {
        "it returned an EnrolmentFailure" should {
          "return an EnrolmentFailure" in {

            mockGetSignUpRequest(testVatNumber, enrolments)(Future.successful(Right(testSignUpRequest)))
            mockSubmitEnrolmentFailure(testSignUpRequest, enrolments)

            val res = await(TestSubmissionOrchestrationService.submitSignUpRequest(testVatNumber, enrolments))

            res.left.value shouldBe EnrolmentFailure
          }
        }
        "it returned a SignUpFailure" should {
          "return a SignUpFailure" in {

            mockGetSignUpRequest(testVatNumber, enrolments)(Future.successful(Right(testSignUpRequest)))
            mockSignUpFailure(testSignUpRequest, enrolments)

            val res = await(TestSubmissionOrchestrationService.submitSignUpRequest(testVatNumber, enrolments))

            res.left.value shouldBe SignUpFailure
          }
        }
        "it returned a RegistrationFailure" should {
          "return a RegistrationFailure" in {

            mockGetSignUpRequest(testVatNumber, enrolments)(Future.successful(Right(testSignUpRequest)))
            mockRegistrationFailure(testSignUpRequest, enrolments)

            val res = await(TestSubmissionOrchestrationService.submitSignUpRequest(testVatNumber, enrolments))

            res.left.value shouldBe RegistrationFailure
          }
        }
      }
    }

    "the get sign up request failed" when {
      "it returned an EmailVerificationFailure" should {
        "return an EmailVerificationFailure" in {

          mockEmailVerificationFailure(testVatNumber, enrolments)

          val res = await(TestSubmissionOrchestrationService.submitSignUpRequest(testVatNumber, enrolments))

          res.left.value shouldBe EmailVerificationFailure
        }
      }
      "it returned an InsufficientData" should {
        "return an InsufficientData" in {

          mockInsufficientData(testVatNumber, enrolments)

          val res = await(TestSubmissionOrchestrationService.submitSignUpRequest(testVatNumber, enrolments))

          res.left.value shouldBe InsufficientData
        }
      }
      "it returned a DatabaseFailure" should {
        "return DatabaseFailure" in {

          mockDatabaseFailure(testVatNumber, enrolments)

          val res = await(TestSubmissionOrchestrationService.submitSignUpRequest(testVatNumber, enrolments))

          res.left.value shouldBe DatabaseFailure
        }
      }
      "it returned a SignUpRequestNotFound" should {
        "return InsufficientData" in {

          mockSignUpRequestNotFound(testVatNumber, enrolments)

          val res = await(TestSubmissionOrchestrationService.submitSignUpRequest(testVatNumber, enrolments))

          res.left.value shouldBe InsufficientData
        }
      }
      "it returned a RequestNotAuthorised" should {
        "return InsufficientData" in {

          mockRequestNotAuthorised(testVatNumber, enrolments)

          val res = await(TestSubmissionOrchestrationService.submitSignUpRequest(testVatNumber, enrolments))

          res.left.value shouldBe InsufficientData
        }
      }
      "it returned a EmailVerificationRequired" should {
        "return EmailVerificationRequired" in {

          mockEmailVerificationRequired(testVatNumber, enrolments)

          val res = await(TestSubmissionOrchestrationService.submitSignUpRequest(testVatNumber, enrolments))

          res.left.value shouldBe EmailVerificationRequired
        }
      }
    }
  }

}
