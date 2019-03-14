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
import play.api.http.Status._
import play.api.test.FakeRequest
import uk.gov.hmrc.auth.core.Enrolments
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.vatsignup.config.featureswitch.{EtmpEntityType, FeatureSwitching, HybridSolution}
import uk.gov.hmrc.vatsignup.config.mocks.MockConfig
import uk.gov.hmrc.vatsignup.connectors.mocks.{MockCustomerSignUpConnector, MockEntityTypeRegistrationConnector, MockRegistrationConnector, MockTaxEnrolmentsConnector}
import uk.gov.hmrc.vatsignup.connectors.utils.EtmpEntityKeys._
import uk.gov.hmrc.vatsignup.helpers.TestConstants._
import uk.gov.hmrc.vatsignup.httpparsers.RegisterWithMultipleIdentifiersHttpParser._
import uk.gov.hmrc.vatsignup.httpparsers.TaxEnrolmentsHttpParser._
import uk.gov.hmrc.vatsignup.models._
import uk.gov.hmrc.vatsignup.models.monitoring.RegisterWithMultipleIDsAuditing.RegisterWithMultipleIDsAuditModel
import uk.gov.hmrc.vatsignup.models.monitoring.SignUpAuditing.SignUpAuditModel
import uk.gov.hmrc.vatsignup.repositories.mocks.{MockEmailRequestRepository, MockSubscriptionRequestRepository}
import uk.gov.hmrc.vatsignup.service.mocks.monitoring.MockAuditService
import uk.gov.hmrc.vatsignup.services.SubmissionService._
import uk.gov.hmrc.vatsignup.services._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class SubmissionServiceWithEntityTypeFSEnabledSpec extends UnitSpec with EitherValues
  with MockSubscriptionRequestRepository
  with MockCustomerSignUpConnector with MockRegistrationConnector with MockEntityTypeRegistrationConnector
  with MockTaxEnrolmentsConnector with MockAuditService with MockEmailRequestRepository
  with MockConfig
  with FeatureSwitching {

  object TestSubmissionService extends SubmissionService(
    mockSubscriptionRequestRepository,
    mockCustomerSignUpConnector,
    mockRegistrationConnector,
    mockEntityTypeRegistrationConnector,
    mockTaxEnrolmentsConnector,
    mockAuditService,
    mockConfig
  )


  private implicit val hc: HeaderCarrier = HeaderCarrier()
  private implicit val request = FakeRequest("POST", "testUrl")

  override def beforeEach(): Unit = {
    super.beforeEach()
    enable(HybridSolution)
    enable(EtmpEntityType)
  }

  val testIsMigratable = true

  "submitSignUpRequest" when {
    "the user is a delegate and " when {
      val enrolments = Enrolments(Set(testAgentEnrolment))

      "the registration request is successful" when {
        "the sign up request is successful" when {
          "the enrolment call is successful" when {
            "HybridSolution is disabled" should {
              "return a SignUpRequestSubmitted for an individual signup" in {
                disable(HybridSolution)

                val signUpRequest = SignUpRequest(
                  vatNumber = testVatNumber,
                  businessEntity = testSoleTrader,
                  signUpEmail = Some(testSignUpEmail),
                  transactionEmail = testSignUpEmail,
                  isDelegated = true,
                  isMigratable = testIsMigratable,
                  contactPreference = Some(testContactPreference)
                )

                mockRegisterBusinessEntity(testVatNumber, testSoleTrader)(Future.successful(Right(RegisterWithMultipleIdsSuccess(testSafeId))))

                mockSignUp(testSafeId,
                  testVatNumber,
                  Some(testEmail),
                  emailVerified = Some(true),
                  optIsPartialMigration = None,
                  optContactPreference = Some(testContactPreference)
                )(Future.successful(Right(CustomerSignUpResponseSuccess)))
                mockRegisterEnrolment(testVatNumber, testSafeId)(Future.successful(Right(SuccessfulTaxEnrolment)))

                val res = await(TestSubmissionService.submitSignUpRequest(signUpRequest, enrolments))
                res.right.value shouldBe SignUpRequestSubmitted

                verifyAudit(RegisterWithMultipleIDsAuditModel(
                  vatNumber = testVatNumber,
                  nino = Some(testNino),
                  businessEntity = SoleTraderKey,
                  agentReferenceNumber = Some(testAgentReferenceNumber),
                  isSuccess = true)
                )
              }

              "return a SignUpRequestSubmitted for a company sign up" in {
                disable(HybridSolution)

                val signUpRequest = SignUpRequest(
                  vatNumber = testVatNumber,
                  businessEntity = testLimitedCompany,
                  signUpEmail = Some(testSignUpEmail),
                  transactionEmail = testSignUpEmail,
                  isDelegated = true,
                  isMigratable = testIsMigratable,
                  contactPreference = Some(testContactPreference)
                )

                mockRegisterBusinessEntity(testVatNumber, testLimitedCompany)(Future.successful(Right(RegisterWithMultipleIdsSuccess(testSafeId))))
                mockSignUp(testSafeId,
                  testVatNumber,
                  Some(testEmail),
                  emailVerified = Some(true),
                  optIsPartialMigration = None,
                  optContactPreference = Some(testContactPreference)
                )(Future.successful(Right(CustomerSignUpResponseSuccess)))
                mockRegisterEnrolment(testVatNumber, testSafeId)(Future.successful(Right(SuccessfulTaxEnrolment)))

                val res = await(TestSubmissionService.submitSignUpRequest(signUpRequest, enrolments))

                res.right.value shouldBe SignUpRequestSubmitted

                verifyAudit(RegisterWithMultipleIDsAuditModel(
                  vatNumber = testVatNumber,
                  companyNumber = Some(testCompanyNumber),
                  businessEntity = LimitedCompanyKey,
                  agentReferenceNumber = Some(testAgentReferenceNumber),
                  isSuccess = true)
                )
                verifyAudit(SignUpAuditModel(testSafeId, testVatNumber, Some(testEmail), Some(true),
                  Some(testAgentReferenceNumber), isSuccess = true, contactPreference = Some(testContactPreference)
                ))
              }

              "return a SignUpRequestSubmitted for a general partnership sign up" in {
                disable(HybridSolution)

                val signUpRequest = SignUpRequest(
                  vatNumber = testVatNumber,
                  businessEntity = testGeneralPartnership,
                  signUpEmail = Some(testSignUpEmail),
                  transactionEmail = testSignUpEmail,
                  isDelegated = true,
                  isMigratable = testIsMigratable,
                  contactPreference = Some(testContactPreference)
                )

                mockRegisterBusinessEntity(testVatNumber, testGeneralPartnership)(Future.successful(Right(RegisterWithMultipleIdsSuccess(testSafeId))))
                mockSignUp(testSafeId,
                  testVatNumber,
                  Some(testEmail),
                  emailVerified = Some(true),
                  optIsPartialMigration = None,
                  optContactPreference = Some(testContactPreference)
                )(Future.successful(Right(CustomerSignUpResponseSuccess)))
                mockRegisterEnrolment(testVatNumber, testSafeId)(Future.successful(Right(SuccessfulTaxEnrolment)))

                val res = await(TestSubmissionService.submitSignUpRequest(signUpRequest, enrolments))

                res.right.value shouldBe SignUpRequestSubmitted

                verifyAudit(RegisterWithMultipleIDsAuditModel(
                  vatNumber = testVatNumber,
                  sautr = Some(testUtr),
                  businessEntity = GeneralPartnershipKey,
                  agentReferenceNumber = Some(testAgentReferenceNumber),
                  isSuccess = true)
                )
                verifyAudit(SignUpAuditModel(testSafeId, testVatNumber, Some(testEmail), Some(true),
                  Some(testAgentReferenceNumber), isSuccess = true, contactPreference = Some(testContactPreference)
                ))
              }
            }
            "HybridSolution is enabled" should {

              "return a SignUpRequestSubmitted for an individual signup" in {

                val signUpRequest = SignUpRequest(
                  vatNumber = testVatNumber,
                  businessEntity = testSoleTrader,
                  signUpEmail = Some(testSignUpEmail),
                  transactionEmail = testSignUpEmail,
                  isDelegated = true,
                  isMigratable = testIsMigratable,
                  contactPreference = Some(testContactPreference)
                )

                mockRegisterBusinessEntity(testVatNumber, testSoleTrader)(Future.successful(Right(RegisterWithMultipleIdsSuccess(testSafeId))))

                mockSignUp(testSafeId,
                  testVatNumber,
                  Some(testEmail),
                  emailVerified = Some(true),
                  optIsPartialMigration = Some(!testIsMigratable),
                  optContactPreference = Some(testContactPreference)
                )(Future.successful(Right(CustomerSignUpResponseSuccess)))
                mockRegisterEnrolment(testVatNumber, testSafeId)(Future.successful(Right(SuccessfulTaxEnrolment)))

                val res = await(TestSubmissionService.submitSignUpRequest(signUpRequest, enrolments))
                res.right.value shouldBe SignUpRequestSubmitted

                verifyAudit(RegisterWithMultipleIDsAuditModel(
                  vatNumber = testVatNumber,
                  nino = Some(testNino),
                  businessEntity = SoleTraderKey,
                  agentReferenceNumber = Some(testAgentReferenceNumber),
                  isSuccess = true)
                )
              }

              "return a SignUpRequestSubmitted for a company sign up" in {
                val signUpRequest = SignUpRequest(
                  vatNumber = testVatNumber,
                  businessEntity = testLimitedCompany,
                  signUpEmail = Some(testSignUpEmail),
                  transactionEmail = testSignUpEmail,
                  isDelegated = true,
                  isMigratable = testIsMigratable,
                  contactPreference = Some(testContactPreference)
                )

                mockRegisterBusinessEntity(testVatNumber, testLimitedCompany)(Future.successful(Right(RegisterWithMultipleIdsSuccess(testSafeId))))
                mockSignUp(testSafeId,
                  testVatNumber,
                  Some(testEmail),
                  emailVerified = Some(true),
                  optIsPartialMigration = Some(!testIsMigratable),
                  optContactPreference = Some(testContactPreference)
                )(Future.successful(Right(CustomerSignUpResponseSuccess)))
                mockRegisterEnrolment(testVatNumber, testSafeId)(Future.successful(Right(SuccessfulTaxEnrolment)))

                val res = await(TestSubmissionService.submitSignUpRequest(signUpRequest, enrolments))

                res.right.value shouldBe SignUpRequestSubmitted

                verifyAudit(RegisterWithMultipleIDsAuditModel(
                  vatNumber = testVatNumber,
                  companyNumber = Some(testCompanyNumber),
                  businessEntity = LimitedCompanyKey,
                  agentReferenceNumber = Some(testAgentReferenceNumber),
                  isSuccess = true)
                )
                verifyAudit(SignUpAuditModel(testSafeId, testVatNumber, Some(testEmail), Some(true),
                  Some(testAgentReferenceNumber), isSuccess = true, contactPreference = Some(testContactPreference)
                ))
              }
            }
          }
          "the enrolment call fails" should {
            "return an EnrolmentFailure" in {
              val signUpRequest = SignUpRequest(
                vatNumber = testVatNumber,
                businessEntity = testLimitedCompany,
                signUpEmail = Some(testSignUpEmail),
                transactionEmail = testSignUpEmail,
                isDelegated = true,
                isMigratable = testIsMigratable,
                contactPreference = Some(testContactPreference)
              )

              mockRegisterBusinessEntity(testVatNumber, testLimitedCompany)(Future.successful(Right(RegisterWithMultipleIdsSuccess(testSafeId))))
              mockSignUp(testSafeId,
                testVatNumber,
                Some(testEmail),
                emailVerified = Some(true),
                optIsPartialMigration = Some(!testIsMigratable),
                optContactPreference = Some(testContactPreference)
              )(Future.successful(Right(CustomerSignUpResponseSuccess)))
              mockRegisterEnrolment(testVatNumber, testSafeId)(Future.successful(Left(FailedTaxEnrolment(BAD_REQUEST))))

              val res = await(TestSubmissionService.submitSignUpRequest(signUpRequest, enrolments))

              res.left.value shouldBe EnrolmentFailure

              verifyAudit(RegisterWithMultipleIDsAuditModel(
                vatNumber = testVatNumber,
                companyNumber = Some(testCompanyNumber),
                businessEntity = LimitedCompanyKey,
                agentReferenceNumber = Some(testAgentReferenceNumber),
                isSuccess = true)
              )
              verifyAudit(SignUpAuditModel(testSafeId, testVatNumber, Some(testEmail), Some(true),
                Some(testAgentReferenceNumber), isSuccess = true, contactPreference = Some(testContactPreference)
              ))
            }
          }
        }
        "the sign up request fails" should {
          "return a SignUpFailure" in {
            val signUpRequest = SignUpRequest(
              vatNumber = testVatNumber,
              businessEntity = testLimitedCompany,
              signUpEmail = Some(testSignUpEmail),
              transactionEmail = testSignUpEmail,
              isDelegated = true,
              isMigratable = testIsMigratable,
              contactPreference = Some(testContactPreference)
            )

            mockRegisterBusinessEntity(testVatNumber, testLimitedCompany)(Future.successful(Right(RegisterWithMultipleIdsSuccess(testSafeId))))
            mockSignUp(testSafeId,
              testVatNumber,
              Some(testEmail),
              emailVerified = Some(true),
              optIsPartialMigration = Some(!testIsMigratable),
              optContactPreference = Some(testContactPreference)
            )(Future.successful(Left(CustomerSignUpResponseFailure(BAD_REQUEST))))

            val res = await(TestSubmissionService.submitSignUpRequest(signUpRequest, enrolments))

            res.left.value shouldBe SignUpFailure

            verifyAudit(RegisterWithMultipleIDsAuditModel(
              vatNumber = testVatNumber,
              companyNumber = Some(testCompanyNumber),
              businessEntity = LimitedCompanyKey,
              agentReferenceNumber = Some(testAgentReferenceNumber),
              isSuccess = true)
            )
            verifyAudit(SignUpAuditModel(testSafeId, testVatNumber, Some(testEmail), Some(true),
              Some(testAgentReferenceNumber), isSuccess = false, contactPreference = Some(testContactPreference)
            ))
          }
        }
      }
      "the registration request fails" should {
        "return a RegistrationFailure" in {
          val signUpRequest = SignUpRequest(
            vatNumber = testVatNumber,
            businessEntity = testLimitedCompany,
            signUpEmail = Some(testSignUpEmail),
            transactionEmail = testSignUpEmail,
            isDelegated = true,
            isMigratable = testIsMigratable,
            contactPreference = Some(testContactPreference)
          )

          mockRegisterBusinessEntity(testVatNumber, testLimitedCompany)(
            Future.successful(Left(RegisterWithMultipleIdsErrorResponse(BAD_REQUEST, "")))
          )

          val res = await(TestSubmissionService.submitSignUpRequest(signUpRequest, enrolments))

          res.left.value shouldBe RegistrationFailure

          verifyAudit(RegisterWithMultipleIDsAuditModel(
            vatNumber = testVatNumber,
            companyNumber = Some(testCompanyNumber),
            businessEntity = LimitedCompanyKey,
            agentReferenceNumber = Some(testAgentReferenceNumber),
            isSuccess = false)
          )

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
                businessEntity = testSoleTrader,
                signUpEmail = Some(testSignUpEmail),
                transactionEmail = testSignUpEmail,
                isDelegated = false,
                isMigratable = testIsMigratable,
                contactPreference = Some(testContactPreference)
              )


              mockRegisterBusinessEntity(testVatNumber, testSoleTrader)(Future.successful(Right(RegisterWithMultipleIdsSuccess(testSafeId))))
              mockSignUp(testSafeId,
                testVatNumber,
                Some(testEmail),
                emailVerified = Some(true),
                optIsPartialMigration = Some(!testIsMigratable),
                optContactPreference = Some(testContactPreference)
              )(Future.successful(Right(CustomerSignUpResponseSuccess)))
              mockRegisterEnrolment(testVatNumber, testSafeId)(Future.successful(Right(SuccessfulTaxEnrolment)))

              val res = await(TestSubmissionService.submitSignUpRequest(signUpRequest, enrolments))

              res.right.value shouldBe SignUpRequestSubmitted

              verifyAudit(RegisterWithMultipleIDsAuditModel(
                vatNumber = testVatNumber,
                nino = Some(testNino),
                businessEntity = SoleTraderKey,
                agentReferenceNumber = None,
                isSuccess = true)
              )
              verifyAudit(SignUpAuditModel(testSafeId, testVatNumber, Some(testEmail),
                Some(true), None, isSuccess = true, contactPreference = Some(testContactPreference)
              ))
            }
            "return a SignUpRequestSubmitted for a company sign up" in {
              val signUpRequest = SignUpRequest(
                vatNumber = testVatNumber,
                businessEntity = testLimitedCompany,
                signUpEmail = Some(testSignUpEmail),
                transactionEmail = testSignUpEmail,
                isDelegated = false,
                isMigratable = testIsMigratable,
                contactPreference = Some(testContactPreference)
              )


              mockRegisterBusinessEntity(testVatNumber, testLimitedCompany)(Future.successful(Right(RegisterWithMultipleIdsSuccess(testSafeId))))
              mockSignUp(testSafeId,
                testVatNumber,
                Some(testEmail),
                emailVerified = Some(true),
                optIsPartialMigration = Some(!testIsMigratable),
                optContactPreference = Some(testContactPreference)
              )(Future.successful(Right(CustomerSignUpResponseSuccess)))
              mockRegisterEnrolment(testVatNumber, testSafeId)(Future.successful(Right(SuccessfulTaxEnrolment)))

              val res = await(TestSubmissionService.submitSignUpRequest(signUpRequest, enrolments))

              res.right.value shouldBe SignUpRequestSubmitted

              verifyAudit(RegisterWithMultipleIDsAuditModel(
                vatNumber = testVatNumber,
                companyNumber = Some(testCompanyNumber),
                businessEntity = LimitedCompanyKey,
                agentReferenceNumber = None,
                isSuccess = true
              ))
              verifyAudit(SignUpAuditModel(testSafeId, testVatNumber, Some(testEmail),
                Some(true), None, isSuccess = true, contactPreference = Some(testContactPreference)
              ))
            }
          }
          "the enrolment call fails" should {
            "return an EnrolmentFailure" in {
              val signUpRequest = SignUpRequest(
                vatNumber = testVatNumber,
                businessEntity = testLimitedCompany,
                signUpEmail = Some(testSignUpEmail),
                transactionEmail = testSignUpEmail,
                isDelegated = false,
                isMigratable = testIsMigratable,
                contactPreference = Some(testContactPreference)
              )


              mockRegisterBusinessEntity(testVatNumber, testLimitedCompany)(Future.successful(Right(RegisterWithMultipleIdsSuccess(testSafeId))))
              mockSignUp(testSafeId,
                testVatNumber,
                Some(testEmail),
                emailVerified = Some(true),
                optIsPartialMigration = Some(!testIsMigratable),
                optContactPreference = Some(testContactPreference)
              )(Future.successful(Right(CustomerSignUpResponseSuccess)))
              mockRegisterEnrolment(testVatNumber, testSafeId)(Future.successful(Left(FailedTaxEnrolment(BAD_REQUEST))))

              val res = await(TestSubmissionService.submitSignUpRequest(signUpRequest, enrolments))

              res.left.value shouldBe EnrolmentFailure

              verifyAudit(RegisterWithMultipleIDsAuditModel(
                vatNumber = testVatNumber,
                companyNumber = Some(testCompanyNumber),
                businessEntity = LimitedCompanyKey,
                agentReferenceNumber = None,
                isSuccess = true
              ))
              verifyAudit(SignUpAuditModel(testSafeId, testVatNumber, Some(testEmail),
                Some(true), None, isSuccess = true, contactPreference = Some(testContactPreference)
              ))
            }
          }
        }
        "the sign up request fails" should {
          "return a SignUpFailure" in {
            val signUpRequest = SignUpRequest(
              vatNumber = testVatNumber,
              businessEntity = testLimitedCompany,
              signUpEmail = Some(testSignUpEmail),
              transactionEmail = testSignUpEmail,
              isDelegated = false,
              isMigratable = testIsMigratable,
              contactPreference = Some(testContactPreference)
            )


            mockRegisterBusinessEntity(testVatNumber, testLimitedCompany)(Future.successful(Right(RegisterWithMultipleIdsSuccess(testSafeId))))
            mockSignUp(testSafeId,
              testVatNumber,
              Some(testEmail),
              emailVerified = Some(true),
              optIsPartialMigration = Some(!testIsMigratable),
              optContactPreference = Some(testContactPreference)
            )(Future.successful(Left(CustomerSignUpResponseFailure(BAD_REQUEST))))

            val res = await(TestSubmissionService.submitSignUpRequest(signUpRequest, enrolments))

            res.left.value shouldBe SignUpFailure

            verifyAudit(RegisterWithMultipleIDsAuditModel(
              vatNumber = testVatNumber,
              companyNumber = Some(testCompanyNumber),
              businessEntity = LimitedCompanyKey,
              agentReferenceNumber = None,
              isSuccess = true
            ))
            verifyAudit(SignUpAuditModel(testSafeId, testVatNumber, Some(testEmail),
              Some(true), None, isSuccess = false, contactPreference = Some(testContactPreference)
            ))
          }
        }
      }
      "the registration request fails" should {
        "return a RegistrationFailure" in {
          val signUpRequest = SignUpRequest(
            vatNumber = testVatNumber,
            businessEntity = testLimitedCompany,
            signUpEmail = Some(testSignUpEmail),
            transactionEmail = testSignUpEmail,
            isDelegated = false,
            isMigratable = testIsMigratable,
            contactPreference = Some(testContactPreference)
          )

          mockRegisterBusinessEntity(testVatNumber, testLimitedCompany)(
            Future.successful(Left(RegisterWithMultipleIdsErrorResponse(BAD_REQUEST, "")))
          )

          val res = await(TestSubmissionService.submitSignUpRequest(signUpRequest, enrolments))

          res.left.value shouldBe RegistrationFailure

          verifyAudit(RegisterWithMultipleIDsAuditModel(
            vatNumber = testVatNumber,
            companyNumber = Some(testCompanyNumber),
            businessEntity = LimitedCompanyKey,
            agentReferenceNumber = None,
            isSuccess = false
          ))
        }
      }
    }
  }

}
