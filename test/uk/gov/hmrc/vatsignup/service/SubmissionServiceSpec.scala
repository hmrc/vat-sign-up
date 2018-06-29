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

package uk.gov.hmrc.vatsignup.service

import org.scalatest.EitherValues
import play.api.http.Status._
import play.api.test.FakeRequest
import reactivemongo.api.commands.{UpdateWriteResult, WriteResult}
import uk.gov.hmrc.auth.core.Enrolments
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.vatsignup.connectors.mocks.{MockCustomerSignUpConnector, MockRegistrationConnector, MockTaxEnrolmentsConnector}
import uk.gov.hmrc.vatsignup.helpers.TestConstants
import uk.gov.hmrc.vatsignup.helpers.TestConstants._
import uk.gov.hmrc.vatsignup.httpparsers.RegisterWithMultipleIdentifiersHttpParser._
import uk.gov.hmrc.vatsignup.httpparsers.TaxEnrolmentsHttpParser._
import uk.gov.hmrc.vatsignup.models._
import uk.gov.hmrc.vatsignup.models.monitoring.RegisterWithMultipleIDsAuditing.RegisterWithMultipleIDsAuditModel
import uk.gov.hmrc.vatsignup.models.monitoring.SignUpAuditing.SignUpAuditModel
import uk.gov.hmrc.vatsignup.repositories.mocks.{MockEmailRequestRepository, MockSubscriptionRequestRepository}
import uk.gov.hmrc.vatsignup.service.mocks.MockEmailRequirementService
import uk.gov.hmrc.vatsignup.service.mocks.monitoring.MockAuditService
import uk.gov.hmrc.vatsignup.services.EmailRequirementService.{Email, GetEmailVerificationFailure, UnVerifiedEmail}
import uk.gov.hmrc.vatsignup.services.SubmissionService._
import uk.gov.hmrc.vatsignup.services._


import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class SubmissionServiceSpec extends UnitSpec with EitherValues
  with MockSubscriptionRequestRepository with MockEmailRequirementService
  with MockCustomerSignUpConnector with MockRegistrationConnector
  with MockTaxEnrolmentsConnector with MockAuditService with MockEmailRequestRepository {

  object TestSubmissionService extends SubmissionService(
    mockSubscriptionRequestRepository,
    mockCustomerSignUpConnector,
    mockRegistrationConnector,
    mockTaxEnrolmentsConnector,
    mockAuditService
  )


  private implicit val hc: HeaderCarrier = HeaderCarrier()
  private implicit val request = FakeRequest("POST", "testUrl")

  "submitSignUpRequest" when {
    "the user is a delegate and " when {
      val enrolments = Enrolments(Set(testAgentEnrolment))

      "the registration request is successful" when {
        "the sign up request is successful" when {
          "the enrolment call is successful" should {
            "return a SignUpRequestSubmitted for an individual signup" in {

              val signUpRequest = SignUpRequest(
                vatNumber = testVatNumber,
                businessEntity = testBusinessEntitySole,
                signUpEmail = Some(testSignUpEmail),
                transactionEmail = testSignUpEmail,
                isDelegated = true
              )


              mockRegisterIndividual(testVatNumber, testNino)(Future.successful(Right(RegisterWithMultipleIdsSuccess(testSafeId))))

              mockSignUp(testSafeId, testVatNumber, Some(testEmail), emailVerified = Some(true))(Future.successful(Right(CustomerSignUpResponseSuccess)))
              mockRegisterEnrolment(testVatNumber, testSafeId)(Future.successful(Right(SuccessfulTaxEnrolment)))

              val res = await(TestSubmissionService.submitSignUpRequest(signUpRequest, enrolments))
              res.right.value shouldBe SignUpRequestSubmitted

              verifyAudit(RegisterWithMultipleIDsAuditModel(TestConstants.testVatNumber, None, Some(TestConstants.testNino),
                Some(TestConstants.testAgentReferenceNumber), isSuccess = true))
            }
            "return a SignUpRequestSubmitted for a company sign up" in {
              val signUpRequest = SignUpRequest(
                vatNumber = testVatNumber,
                businessEntity = testBusinessEntityLTD,
                signUpEmail = Some(testSignUpEmail),
                transactionEmail = testSignUpEmail,
                isDelegated = true
              )

              mockRegisterCompany(testVatNumber, testCompanyNumber)(Future.successful(Right(RegisterWithMultipleIdsSuccess(testSafeId))))
              mockSignUp(testSafeId, testVatNumber, Some(testEmail), emailVerified = Some(true))(Future.successful(Right(CustomerSignUpResponseSuccess)))
              mockRegisterEnrolment(testVatNumber, testSafeId)(Future.successful(Right(SuccessfulTaxEnrolment)))

              val res = await(TestSubmissionService.submitSignUpRequest(signUpRequest, enrolments))

              res.right.value shouldBe SignUpRequestSubmitted

              verifyAudit(RegisterWithMultipleIDsAuditModel(TestConstants.testVatNumber, Some(TestConstants.testCompanyNumber), None,
                Some(TestConstants.testAgentReferenceNumber), isSuccess = true))
              verifyAudit(SignUpAuditModel(TestConstants.testSafeId, TestConstants.testVatNumber, Some(TestConstants.testEmail), Some(true),
                Some(TestConstants.testAgentReferenceNumber), isSuccess = true))
            }
          }
          "the enrolment call fails" should {
            "return an EnrolmentFailure" in {
              val signUpRequest = SignUpRequest(
                vatNumber = testVatNumber,
                businessEntity = testBusinessEntityLTD,
                signUpEmail = Some(testSignUpEmail),
                transactionEmail = testSignUpEmail,
                isDelegated = true
              )

              mockRegisterCompany(testVatNumber, testCompanyNumber)(Future.successful(Right(RegisterWithMultipleIdsSuccess(testSafeId))))
              mockSignUp(testSafeId, testVatNumber, Some(testEmail), emailVerified = Some(true))(Future.successful(Right(CustomerSignUpResponseSuccess)))
              mockRegisterEnrolment(testVatNumber, testSafeId)(Future.successful(Left(FailedTaxEnrolment(BAD_REQUEST))))

              val res = await(TestSubmissionService.submitSignUpRequest(signUpRequest, enrolments))

              res.left.value shouldBe EnrolmentFailure

              verifyAudit(RegisterWithMultipleIDsAuditModel(TestConstants.testVatNumber, Some(TestConstants.testCompanyNumber), None,
                Some(TestConstants.testAgentReferenceNumber), isSuccess = true))
              verifyAudit(SignUpAuditModel(TestConstants.testSafeId, TestConstants.testVatNumber, Some(TestConstants.testEmail), Some(true),
                Some(TestConstants.testAgentReferenceNumber), isSuccess = true))
            }
          }
        }
        "the sign up request fails" should {
          "return a SignUpFailure" in {
            val signUpRequest = SignUpRequest(
              vatNumber = testVatNumber,
              businessEntity = testBusinessEntityLTD,
              signUpEmail = Some(testSignUpEmail),
              transactionEmail = testSignUpEmail,
              isDelegated = true
            )

            mockRegisterCompany(testVatNumber, testCompanyNumber)(Future.successful(Right(RegisterWithMultipleIdsSuccess(testSafeId))))
            mockSignUp(testSafeId, testVatNumber, Some(testEmail), emailVerified = Some(true))(Future.successful(Left(CustomerSignUpResponseFailure(BAD_REQUEST))))

            val res = await(TestSubmissionService.submitSignUpRequest(signUpRequest, enrolments))

            res.left.value shouldBe SignUpFailure

            verifyAudit(RegisterWithMultipleIDsAuditModel(TestConstants.testVatNumber, Some(TestConstants.testCompanyNumber), None,
              Some(TestConstants.testAgentReferenceNumber), isSuccess = true))
            verifyAudit(SignUpAuditModel(TestConstants.testSafeId, TestConstants.testVatNumber, Some(TestConstants.testEmail), Some(true),
              Some(TestConstants.testAgentReferenceNumber), isSuccess = false))

          }
        }
      }
      "the registration request fails" should {
        "return a RegistrationFailure" in {
          val signUpRequest = SignUpRequest(
            vatNumber = testVatNumber,
            businessEntity = testBusinessEntityLTD,
            signUpEmail = Some(testSignUpEmail),
            transactionEmail = testSignUpEmail,
            isDelegated = true
          )

          mockRegisterCompany(testVatNumber, testCompanyNumber)(
            Future.successful(Left(RegisterWithMultipleIdsErrorResponse(BAD_REQUEST, "")))
          )

          val res = await(TestSubmissionService.submitSignUpRequest(signUpRequest, enrolments))

          res.left.value shouldBe RegistrationFailure

          verifyAudit(RegisterWithMultipleIDsAuditModel(TestConstants.testVatNumber, Some(TestConstants.testCompanyNumber), None,
            Some(TestConstants.testAgentReferenceNumber), isSuccess = false))

        }
      }
    }
    "the user is principal" when {
      val enrolments = Enrolments(Set(testPrincipalEnrolment))

      "the registration request is successful" when {
        "the sign up request is successful" when {
          "the enrolment call is successful" should {
            "return a SignUpRequestSubmitted for an individual signup" in {
              val signUpRequest = SignUpRequest(
                vatNumber = testVatNumber,
                businessEntity = testBusinessEntitySole,
                signUpEmail = Some(testSignUpEmail),
                transactionEmail = testSignUpEmail,
                isDelegated = false
              )


              mockRegisterIndividual(testVatNumber, testNino)(Future.successful(Right(RegisterWithMultipleIdsSuccess(testSafeId))))
              mockSignUp(testSafeId, testVatNumber, Some(testEmail), emailVerified = Some(true))(Future.successful(Right(CustomerSignUpResponseSuccess)))
              mockRegisterEnrolment(testVatNumber, testSafeId)(Future.successful(Right(SuccessfulTaxEnrolment)))

              val res = await(TestSubmissionService.submitSignUpRequest(signUpRequest, enrolments))

              res.right.value shouldBe SignUpRequestSubmitted

              verifyAudit(RegisterWithMultipleIDsAuditModel(TestConstants.testVatNumber, None, Some(TestConstants.testNino), None, isSuccess = true))
              verifyAudit(SignUpAuditModel(TestConstants.testSafeId, TestConstants.testVatNumber, Some(TestConstants.testEmail), Some(true), None, isSuccess = true))
            }
            "return a SignUpRequestSubmitted for a company sign up" in {
              val signUpRequest = SignUpRequest(
                vatNumber = testVatNumber,
                businessEntity = testBusinessEntityLTD,
                signUpEmail = Some(testSignUpEmail),
                transactionEmail = testSignUpEmail,
                isDelegated = false
              )


              mockRegisterCompany(testVatNumber, testCompanyNumber)(Future.successful(Right(RegisterWithMultipleIdsSuccess(testSafeId))))
              mockSignUp(testSafeId, testVatNumber, Some(testEmail), emailVerified = Some(true))(Future.successful(Right(CustomerSignUpResponseSuccess)))
              mockRegisterEnrolment(testVatNumber, testSafeId)(Future.successful(Right(SuccessfulTaxEnrolment)))

              val res = await(TestSubmissionService.submitSignUpRequest(signUpRequest, enrolments))

              res.right.value shouldBe SignUpRequestSubmitted

              verifyAudit(RegisterWithMultipleIDsAuditModel(TestConstants.testVatNumber, Some(TestConstants.testCompanyNumber), None, None, isSuccess = true))
              verifyAudit(SignUpAuditModel(TestConstants.testSafeId, TestConstants.testVatNumber, Some(TestConstants.testEmail), Some(true), None, isSuccess = true))
            }
          }
          "the enrolment call fails" should {
            "return an EnrolmentFailure" in {
              val signUpRequest = SignUpRequest(
                vatNumber = testVatNumber,
                businessEntity = testBusinessEntityLTD,
                signUpEmail = Some(testSignUpEmail),
                transactionEmail = testSignUpEmail,
                isDelegated = false
              )


              mockRegisterCompany(testVatNumber, testCompanyNumber)(Future.successful(Right(RegisterWithMultipleIdsSuccess(testSafeId))))
              mockSignUp(testSafeId, testVatNumber, Some(testEmail), emailVerified = Some(true))(Future.successful(Right(CustomerSignUpResponseSuccess)))
              mockRegisterEnrolment(testVatNumber, testSafeId)(Future.successful(Left(FailedTaxEnrolment(BAD_REQUEST))))

              val res = await(TestSubmissionService.submitSignUpRequest(signUpRequest, enrolments))

              res.left.value shouldBe EnrolmentFailure

              verifyAudit(RegisterWithMultipleIDsAuditModel(TestConstants.testVatNumber, Some(TestConstants.testCompanyNumber), None, None, isSuccess = true))
              verifyAudit(SignUpAuditModel(TestConstants.testSafeId, TestConstants.testVatNumber, Some(TestConstants.testEmail), Some(true), None, isSuccess = true))
            }
          }
        }
        "the sign up request fails" should {
          "return a SignUpFailure" in {
            val signUpRequest = SignUpRequest(
              vatNumber = testVatNumber,
              businessEntity = testBusinessEntityLTD,
              signUpEmail = Some(testSignUpEmail),
              transactionEmail = testSignUpEmail,
              isDelegated = false
            )


            mockRegisterCompany(testVatNumber, testCompanyNumber)(Future.successful(Right(RegisterWithMultipleIdsSuccess(testSafeId))))
            mockSignUp(testSafeId, testVatNumber, Some(testEmail), emailVerified = Some(true))(Future.successful(Left(CustomerSignUpResponseFailure(BAD_REQUEST))))

            val res = await(TestSubmissionService.submitSignUpRequest(signUpRequest, enrolments))

            res.left.value shouldBe SignUpFailure

            verifyAudit(RegisterWithMultipleIDsAuditModel(TestConstants.testVatNumber, Some(TestConstants.testCompanyNumber), None, None, isSuccess = true))
            verifyAudit(SignUpAuditModel(TestConstants.testSafeId, TestConstants.testVatNumber, Some(TestConstants.testEmail), Some(true), None, isSuccess = false))
          }
        }
      }
      "the registration request fails" should {
        "return a RegistrationFailure" in {
          val signUpRequest = SignUpRequest(
            vatNumber = testVatNumber,
            businessEntity = testBusinessEntityLTD,
            signUpEmail = Some(testSignUpEmail),
            transactionEmail = testSignUpEmail,
            isDelegated = false
          )

          mockRegisterCompany(testVatNumber, testCompanyNumber)(
            Future.successful(Left(RegisterWithMultipleIdsErrorResponse(BAD_REQUEST, "")))
          )

          val res = await(TestSubmissionService.submitSignUpRequest(signUpRequest, enrolments))

          res.left.value shouldBe RegistrationFailure

          verifyAudit(RegisterWithMultipleIDsAuditModel(TestConstants.testVatNumber, Some(TestConstants.testCompanyNumber), None, None, isSuccess = false))
        }
      }
    }
  }
}
