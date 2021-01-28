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

import org.scalatest.EitherValues
import play.api.test.Helpers._
import play.api.test.FakeRequest
import uk.gov.hmrc.auth.core.Enrolments
import uk.gov.hmrc.http.HeaderCarrier
import org.scalatest.{WordSpec, Matchers}
import uk.gov.hmrc.vatsignup.config.mocks.MockConfig
import uk.gov.hmrc.vatsignup.connectors.mocks._
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

class SubmissionServiceSpec extends WordSpec with Matchers with EitherValues
  with MockSubscriptionRequestRepository
  with MockCustomerSignUpConnector with MockRegistrationConnector
  with MockTaxEnrolmentsConnector with MockAuditService with MockEmailRequestRepository
  with MockConfig {

  object TestSubmissionService extends SubmissionService(
    mockSubscriptionRequestRepository,
    mockCustomerSignUpConnector,
    mockEntityTypeRegistrationConnector,
    mockTaxEnrolmentsConnector,
    mockAuditService,
    mockConfig
  )

  private implicit val hc: HeaderCarrier = HeaderCarrier()
  private implicit val request = FakeRequest("POST", "testUrl")

  val testIsMigratable = true

  "submitSignUpRequest" when {
    "the user is a delegate and " when {
      val enrolments = Enrolments(Set(testAgentEnrolment))

      "the registration request is successful" when {
        "the sign up request is successful" when {
          "the enrolment call is successful" when {
            "return a SignUpRequestSubmitted for an individual signup" in {

              val signUpRequest = SignUpRequest(
                vatNumber = testVatNumber,
                businessEntity = testSoleTrader,
                signUpEmail = Some(testSignUpEmail),
                transactionEmail = testSignUpEmail,
                isDelegated = true,
                isMigratable = testIsMigratable,
                contactPreference = testContactPreference
              )

              mockRegisterBusinessEntity(testVatNumber, SoleTrader(testNino))(
                Future.successful(Right(RegisterWithMultipleIdsSuccess(testSafeId)))
              )
              mockSignUp(testSafeId,
                testVatNumber,
                Some(testEmail),
                emailVerified = Some(true),
                isPartialMigration = !testIsMigratable,
                contactPreference = testContactPreference
              )(
                Future.successful(Right(CustomerSignUpResponseSuccess))
              )
              mockRegisterEnrolment(testVatNumber, testSafeId)(Future.successful(Right(SuccessfulTaxEnrolment)))

              val res = await(TestSubmissionService.submitSignUpRequest(signUpRequest, enrolments))
              res.right.value shouldBe SignUpRequestSubmitted

              verifyAudit(RegisterWithMultipleIDsAuditModel(
                vatNumber = testVatNumber,
                nino = Some(testNino),
                businessEntity = SoleTraderKey,
                agentReferenceNumber = Some(testAgentReferenceNumber),
                isSuccess = true
              ))
            }
            "return a SignUpRequestSubmitted for a company sign up" in {
              val signUpRequest = SignUpRequest(
                vatNumber = testVatNumber,
                businessEntity = testLimitedCompany,
                signUpEmail = Some(testSignUpEmail),
                transactionEmail = testSignUpEmail,
                isDelegated = true,
                isMigratable = testIsMigratable,
                contactPreference = testContactPreference
              )

              mockRegisterBusinessEntity(testVatNumber, LimitedCompany(testCompanyNumber))(
                Future.successful(Right(RegisterWithMultipleIdsSuccess(testSafeId)))
              )
              mockSignUp(testSafeId,
                testVatNumber,
                Some(testEmail),
                emailVerified = Some(true),
                isPartialMigration = !testIsMigratable,
                contactPreference = testContactPreference
              )(
                Future.successful(Right(CustomerSignUpResponseSuccess))
              )
              mockRegisterEnrolment(testVatNumber, testSafeId)(Future.successful(Right(SuccessfulTaxEnrolment)))

              val res = await(TestSubmissionService.submitSignUpRequest(signUpRequest, enrolments))

              res.right.value shouldBe SignUpRequestSubmitted

              verifyAudit(RegisterWithMultipleIDsAuditModel(
                vatNumber = testVatNumber,
                companyNumber = Some(testCompanyNumber),
                businessEntity = LimitedCompanyKey,
                agentReferenceNumber = Some(testAgentReferenceNumber),
                isSuccess = true
              ))
              verifyAudit(SignUpAuditModel(testSafeId, testVatNumber, Some(testEmail), testEmail, Some(true),
                Some(testAgentReferenceNumber), isSuccess = true, contactPreference = testContactPreference
              ))
            }
            "return a SignUpRequestSubmitted for a general partnership sign up" in {
              val signUpRequest = SignUpRequest(
                vatNumber = testVatNumber,
                businessEntity = testGeneralPartnership,
                signUpEmail = Some(testSignUpEmail),
                transactionEmail = testSignUpEmail,
                isDelegated = true,
                isMigratable = testIsMigratable,
                contactPreference = testContactPreference
              )

              mockRegisterBusinessEntity(testVatNumber, testGeneralPartnership)(
                Future.successful(Right(RegisterWithMultipleIdsSuccess(testSafeId)))
              )
              mockSignUp(testSafeId,
                testVatNumber,
                Some(testEmail),
                emailVerified = Some(true),
                isPartialMigration = !testIsMigratable,
                contactPreference = testContactPreference
              )(
                Future.successful(Right(CustomerSignUpResponseSuccess))
              )
              mockRegisterEnrolment(testVatNumber, testSafeId)(Future.successful(Right(SuccessfulTaxEnrolment)))

              val res = await(TestSubmissionService.submitSignUpRequest(signUpRequest, enrolments))

              res.right.value shouldBe SignUpRequestSubmitted

              verifyAudit(RegisterWithMultipleIDsAuditModel(
                vatNumber = testVatNumber,
                sautr = Some(testUtr),
                businessEntity = GeneralPartnershipKey,
                agentReferenceNumber = Some(testAgentReferenceNumber),
                isSuccess = true
              ))
              verifyAudit(SignUpAuditModel(testSafeId, testVatNumber, Some(testEmail), testEmail, Some(true),
                Some(testAgentReferenceNumber), isSuccess = true, contactPreference = testContactPreference
              ))
            }
            "return a SignUpRequestSubmitted for a limited partnership sign up" in {
              val signUpRequest = SignUpRequest(
                vatNumber = testVatNumber,
                businessEntity = testLimitedPartnership,
                signUpEmail = Some(testSignUpEmail),
                transactionEmail = testSignUpEmail,
                isDelegated = true,
                isMigratable = testIsMigratable,
                contactPreference = testContactPreference
              )

              mockRegisterBusinessEntity(testVatNumber, testLimitedPartnership)(
                Future.successful(Right(RegisterWithMultipleIdsSuccess(testSafeId)))
              )
              mockSignUp(testSafeId,
                testVatNumber,
                Some(testEmail),
                emailVerified = Some(true),
                isPartialMigration = !testIsMigratable,
                contactPreference = testContactPreference
              )(
                Future.successful(Right(CustomerSignUpResponseSuccess))
              )
              mockRegisterEnrolment(testVatNumber, testSafeId)(Future.successful(Right(SuccessfulTaxEnrolment)))

              val res = await(TestSubmissionService.submitSignUpRequest(signUpRequest, enrolments))

              res.right.value shouldBe SignUpRequestSubmitted

              verifyAudit(RegisterWithMultipleIDsAuditModel(
                vatNumber = testVatNumber,
                sautr = Some(testUtr),
                companyNumber = Some(testCompanyNumber),
                businessEntity = LimitedPartnershipKey,
                agentReferenceNumber = Some(testAgentReferenceNumber),
                isSuccess = true
              ))
              verifyAudit(SignUpAuditModel(testSafeId, testVatNumber, Some(testEmail), testEmail, Some(true),
                Some(testAgentReferenceNumber), isSuccess = true, contactPreference = testContactPreference
              ))
            }
            "return a SignUpRequestSubmitted for a limited liability partnership sign up" in {
              val signUpRequest = SignUpRequest(
                vatNumber = testVatNumber,
                businessEntity = testLimitedLiabilityPartnership,
                signUpEmail = Some(testSignUpEmail),
                transactionEmail = testSignUpEmail,
                isDelegated = true,
                isMigratable = testIsMigratable,
                contactPreference = testContactPreference
              )

              mockRegisterBusinessEntity(testVatNumber, testLimitedLiabilityPartnership)(
                Future.successful(Right(RegisterWithMultipleIdsSuccess(testSafeId)))
              )
              mockSignUp(testSafeId,
                testVatNumber,
                Some(testEmail),
                emailVerified = Some(true),
                isPartialMigration = !testIsMigratable,
                contactPreference = testContactPreference
              )(
                Future.successful(Right(CustomerSignUpResponseSuccess))
              )
              mockRegisterEnrolment(testVatNumber, testSafeId)(Future.successful(Right(SuccessfulTaxEnrolment)))

              val res = await(TestSubmissionService.submitSignUpRequest(signUpRequest, enrolments))

              res.right.value shouldBe SignUpRequestSubmitted

              verifyAudit(RegisterWithMultipleIDsAuditModel(
                vatNumber = testVatNumber,
                sautr = Some(testUtr),
                companyNumber = Some(testCompanyNumber),
                businessEntity = LimitedLiabilityPartnershipKey,
                agentReferenceNumber = Some(testAgentReferenceNumber),
                isSuccess = true
              ))
              verifyAudit(SignUpAuditModel(testSafeId, testVatNumber, Some(testEmail), testEmail, Some(true),
                Some(testAgentReferenceNumber), isSuccess = true, contactPreference = testContactPreference
              ))
            }
            "return a SignUpRequestSubmitted for a scottish limited partnership sign up" in {
              val signUpRequest = SignUpRequest(
                vatNumber = testVatNumber,
                businessEntity = testScottishLimitedPartnership,
                signUpEmail = Some(testSignUpEmail),
                transactionEmail = testSignUpEmail,
                isDelegated = true,
                isMigratable = testIsMigratable,
                contactPreference = testContactPreference
              )

              mockRegisterBusinessEntity(testVatNumber, testScottishLimitedPartnership)(
                Future.successful(Right(RegisterWithMultipleIdsSuccess(testSafeId)))
              )
              mockSignUp(testSafeId,
                testVatNumber,
                Some(testEmail),
                emailVerified = Some(true),
                isPartialMigration = !testIsMigratable,
                contactPreference = testContactPreference
              )(
                Future.successful(Right(CustomerSignUpResponseSuccess))
              )
              mockRegisterEnrolment(testVatNumber, testSafeId)(Future.successful(Right(SuccessfulTaxEnrolment)))

              val res = await(TestSubmissionService.submitSignUpRequest(signUpRequest, enrolments))

              res.right.value shouldBe SignUpRequestSubmitted

              verifyAudit(RegisterWithMultipleIDsAuditModel(
                vatNumber = testVatNumber,
                sautr = Some(testUtr),
                companyNumber = Some(testCompanyNumber),
                businessEntity = ScottishLimitedPartnershipKey,
                agentReferenceNumber = Some(testAgentReferenceNumber),
                isSuccess = true
              ))
              verifyAudit(SignUpAuditModel(testSafeId, testVatNumber, Some(testEmail), testEmail, Some(true),
                Some(testAgentReferenceNumber), isSuccess = true, contactPreference = testContactPreference
              ))
            }
            "return a SignUpRequestSubmitted for a VAT group sign up" in {
              val signUpRequest = SignUpRequest(
                vatNumber = testVatNumber,
                businessEntity = VatGroup,
                signUpEmail = Some(testSignUpEmail),
                transactionEmail = testSignUpEmail,
                isDelegated = true,
                isMigratable = testIsMigratable,
                contactPreference = testContactPreference
              )

              mockRegisterBusinessEntity(testVatNumber, VatGroup)(
                Future.successful(Right(RegisterWithMultipleIdsSuccess(testSafeId)))
              )
              mockSignUp(testSafeId,
                testVatNumber,
                Some(testEmail),
                emailVerified = Some(true),
                isPartialMigration = !testIsMigratable,
                contactPreference = testContactPreference
              )(
                Future.successful(Right(CustomerSignUpResponseSuccess))
              )
              mockRegisterEnrolment(testVatNumber, testSafeId)(Future.successful(Right(SuccessfulTaxEnrolment)))

              val res = await(TestSubmissionService.submitSignUpRequest(signUpRequest, enrolments))

              res.right.value shouldBe SignUpRequestSubmitted

              verifyAudit(RegisterWithMultipleIDsAuditModel(
                vatNumber = testVatNumber,
                sautr = None,
                companyNumber = None,
                businessEntity = VatGroupKey,
                agentReferenceNumber = Some(testAgentReferenceNumber),
                isSuccess = true
              ))
              verifyAudit(SignUpAuditModel(testSafeId, testVatNumber, Some(testEmail), testEmail, Some(true),
                Some(testAgentReferenceNumber), isSuccess = true, contactPreference = testContactPreference
              ))
            }
            "return a SignUpRequestSubmitted for a Division sign up" in {
              val signUpRequest = SignUpRequest(
                vatNumber = testVatNumber,
                businessEntity = AdministrativeDivision,
                signUpEmail = Some(testSignUpEmail),
                transactionEmail = testSignUpEmail,
                isDelegated = true,
                isMigratable = testIsMigratable,
                contactPreference = testContactPreference
              )

              mockRegisterBusinessEntity(testVatNumber, AdministrativeDivision)(
                Future.successful(Right(RegisterWithMultipleIdsSuccess(testSafeId)))
              )
              mockSignUp(testSafeId,
                testVatNumber,
                Some(testEmail),
                emailVerified = Some(true),
                isPartialMigration = !testIsMigratable,
                contactPreference = testContactPreference
              )(
                Future.successful(Right(CustomerSignUpResponseSuccess))
              )
              mockRegisterEnrolment(testVatNumber, testSafeId)(Future.successful(Right(SuccessfulTaxEnrolment)))

              val res = await(TestSubmissionService.submitSignUpRequest(signUpRequest, enrolments))

              res.right.value shouldBe SignUpRequestSubmitted

              verifyAudit(RegisterWithMultipleIDsAuditModel(
                vatNumber = testVatNumber,
                sautr = None,
                companyNumber = None,
                businessEntity = AdministrativeDivisionKey,
                agentReferenceNumber = Some(testAgentReferenceNumber),
                isSuccess = true
              ))
              verifyAudit(SignUpAuditModel(testSafeId, testVatNumber, Some(testEmail), testEmail, Some(true),
                Some(testAgentReferenceNumber), isSuccess = true, contactPreference = testContactPreference
              ))
            }
            "return a SignUpRequestSubmitted for an Unincorporated Associations sign up" in {
              val signUpRequest = SignUpRequest(
                vatNumber = testVatNumber,
                businessEntity = UnincorporatedAssociation,
                signUpEmail = Some(testSignUpEmail),
                transactionEmail = testSignUpEmail,
                isDelegated = true,
                isMigratable = testIsMigratable,
                contactPreference = testContactPreference
              )

              mockRegisterBusinessEntity(testVatNumber, UnincorporatedAssociation)(
                Future.successful(Right(RegisterWithMultipleIdsSuccess(testSafeId)))
              )
              mockSignUp(testSafeId,
                testVatNumber,
                Some(testEmail),
                emailVerified = Some(true),
                isPartialMigration = !testIsMigratable,
                contactPreference = testContactPreference
              )(
                Future.successful(Right(CustomerSignUpResponseSuccess))
              )
              mockRegisterEnrolment(testVatNumber, testSafeId)(Future.successful(Right(SuccessfulTaxEnrolment)))

              val res = await(TestSubmissionService.submitSignUpRequest(signUpRequest, enrolments))

              res.right.value shouldBe SignUpRequestSubmitted

              verifyAudit(RegisterWithMultipleIDsAuditModel(
                vatNumber = testVatNumber,
                sautr = None,
                companyNumber = None,
                businessEntity = UnincorporatedAssociationKey,
                agentReferenceNumber = Some(testAgentReferenceNumber),
                isSuccess = true
              ))
              verifyAudit(SignUpAuditModel(testSafeId, testVatNumber, Some(testEmail), testEmail, Some(true),
                Some(testAgentReferenceNumber), isSuccess = true, contactPreference = testContactPreference
              ))
            }
            "return a SignUpRequestSubmitted for a Registered Society sign up" in {
              val signUpRequest = SignUpRequest(
                vatNumber = testVatNumber,
                businessEntity = testRegisteredSociety,
                signUpEmail = Some(testSignUpEmail),
                transactionEmail = testSignUpEmail,
                isDelegated = true,
                isMigratable = testIsMigratable,
                contactPreference = testContactPreference
              )

              mockRegisterBusinessEntity(testVatNumber, testRegisteredSociety)(
                Future.successful(Right(RegisterWithMultipleIdsSuccess(testSafeId)))
              )
              mockSignUp(testSafeId,
                testVatNumber,
                Some(testEmail),
                emailVerified = Some(true),
                isPartialMigration = !testIsMigratable,
                contactPreference = testContactPreference
              )(
                Future.successful(Right(CustomerSignUpResponseSuccess))
              )
              mockRegisterEnrolment(testVatNumber, testSafeId)(Future.successful(Right(SuccessfulTaxEnrolment)))

              val res = await(TestSubmissionService.submitSignUpRequest(signUpRequest, enrolments))

              res.right.value shouldBe SignUpRequestSubmitted

              verifyAudit(RegisterWithMultipleIDsAuditModel(
                vatNumber = testVatNumber,
                sautr = None,
                companyNumber = Some(testCompanyNumber),
                businessEntity = RegisteredSocietyKey,
                agentReferenceNumber = Some(testAgentReferenceNumber),
                isSuccess = true
              ))
              verifyAudit(SignUpAuditModel(testSafeId, testVatNumber, Some(testEmail), testEmail, Some(true),
                Some(testAgentReferenceNumber), isSuccess = true, contactPreference = testContactPreference
              ))
            }
            "return a SignUpRequestSubmitted for a Charity sign up" in {
              val signUpRequest = SignUpRequest(
                vatNumber = testVatNumber,
                businessEntity = Charity,
                signUpEmail = Some(testSignUpEmail),
                transactionEmail = testSignUpEmail,
                isDelegated = true,
                isMigratable = testIsMigratable,
                contactPreference = testContactPreference
              )

              mockRegisterBusinessEntity(testVatNumber, Charity)(
                Future.successful(Right(RegisterWithMultipleIdsSuccess(testSafeId)))
              )
              mockSignUp(testSafeId,
                testVatNumber,
                Some(testEmail),
                emailVerified = Some(true),
                isPartialMigration = !testIsMigratable,
                contactPreference = testContactPreference
              )(
                Future.successful(Right(CustomerSignUpResponseSuccess))
              )
              mockRegisterEnrolment(testVatNumber, testSafeId)(Future.successful(Right(SuccessfulTaxEnrolment)))

              val res = await(TestSubmissionService.submitSignUpRequest(signUpRequest, enrolments))

              res.right.value shouldBe SignUpRequestSubmitted

              verifyAudit(RegisterWithMultipleIDsAuditModel(
                vatNumber = testVatNumber,
                sautr = None,
                companyNumber = None,
                businessEntity = CharityKey,
                agentReferenceNumber = Some(testAgentReferenceNumber),
                isSuccess = true
              ))
              verifyAudit(SignUpAuditModel(testSafeId, testVatNumber, Some(testEmail), testEmail, Some(true),
                Some(testAgentReferenceNumber), isSuccess = true, contactPreference = testContactPreference
              ))
            }
            "return a SignUpRequestSubmitted for a Government Organisation sign up" in {
              val signUpRequest = SignUpRequest(
                vatNumber = testVatNumber,
                businessEntity = GovernmentOrganisation,
                signUpEmail = Some(testSignUpEmail),
                transactionEmail = testSignUpEmail,
                isDelegated = true,
                isMigratable = testIsMigratable,
                contactPreference = testContactPreference
              )

              mockRegisterBusinessEntity(testVatNumber, GovernmentOrganisation)(
                Future.successful(Right(RegisterWithMultipleIdsSuccess(testSafeId)))
              )
              mockSignUp(testSafeId,
                testVatNumber,
                Some(testEmail),
                emailVerified = Some(true),
                isPartialMigration = !testIsMigratable,
                contactPreference = testContactPreference
              )(
                Future.successful(Right(CustomerSignUpResponseSuccess))
              )
              mockRegisterEnrolment(testVatNumber, testSafeId)(Future.successful(Right(SuccessfulTaxEnrolment)))

              val res = await(TestSubmissionService.submitSignUpRequest(signUpRequest, enrolments))

              res.right.value shouldBe SignUpRequestSubmitted

              verifyAudit(RegisterWithMultipleIDsAuditModel(
                vatNumber = testVatNumber,
                sautr = None,
                companyNumber = None,
                businessEntity = GovernmentOrganisationKey,
                agentReferenceNumber = Some(testAgentReferenceNumber),
                isSuccess = true
              ))
              verifyAudit(SignUpAuditModel(testSafeId, testVatNumber, Some(testEmail), testEmail, Some(true),
                Some(testAgentReferenceNumber), isSuccess = true, contactPreference = testContactPreference
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
                isDelegated = true,
                isMigratable = testIsMigratable,
                contactPreference = testContactPreference
              )

              mockRegisterBusinessEntity(testVatNumber, LimitedCompany(testCompanyNumber))(
                Future.successful(Right(RegisterWithMultipleIdsSuccess(testSafeId)))
              )
              mockSignUp(testSafeId,
                testVatNumber,
                Some(testEmail),
                emailVerified = Some(true),
                isPartialMigration = !testIsMigratable,
                contactPreference = testContactPreference
              )(
                Future.successful(Right(CustomerSignUpResponseSuccess))
              )
              mockRegisterEnrolment(testVatNumber, testSafeId)(Future.successful(Left(FailedTaxEnrolment(BAD_REQUEST))))

              val res = await(TestSubmissionService.submitSignUpRequest(signUpRequest, enrolments))

              res.left.value shouldBe EnrolmentFailure

              verifyAudit(RegisterWithMultipleIDsAuditModel(
                vatNumber = testVatNumber,
                companyNumber = Some(testCompanyNumber),
                businessEntity = LimitedCompanyKey,
                agentReferenceNumber = Some(testAgentReferenceNumber),
                isSuccess = true
              ))
              verifyAudit(SignUpAuditModel(testSafeId, testVatNumber, Some(testEmail), testEmail, Some(true),
                Some(testAgentReferenceNumber), isSuccess = true, contactPreference = testContactPreference
              ))
            }
          }
        }
        "the sign up request fails" should {
          "return a SignUpFailure" in {
            val failureStatus = BAD_REQUEST
            val failureBody = "Some response from DES"

            val signUpRequest = SignUpRequest(
              vatNumber = testVatNumber,
              businessEntity = testLimitedCompany,
              signUpEmail = Some(testSignUpEmail),
              transactionEmail = testSignUpEmail,
              isDelegated = true,
              isMigratable = testIsMigratable,
              contactPreference = testContactPreference
            )

            mockRegisterBusinessEntity(testVatNumber, LimitedCompany(testCompanyNumber))(
              Future.successful(Right(RegisterWithMultipleIdsSuccess(testSafeId)))
            )
            mockSignUp(testSafeId,
              testVatNumber,
              Some(testEmail),
              emailVerified = Some(true),
              isPartialMigration = !testIsMigratable,
              contactPreference = testContactPreference
            )(
              Future.successful(Left(CustomerSignUpResponseFailure(failureStatus, failureBody)))
            )

            val res = await(TestSubmissionService.submitSignUpRequest(signUpRequest, enrolments))

            res.left.value shouldBe SignUpFailure(failureStatus, failureBody)

            verifyAudit(RegisterWithMultipleIDsAuditModel(
              vatNumber = testVatNumber,
              companyNumber = Some(testCompanyNumber),
              businessEntity = LimitedCompanyKey,
              agentReferenceNumber = Some(testAgentReferenceNumber),
              isSuccess = true
            ))
            verifyAudit(SignUpAuditModel(testSafeId, testVatNumber, Some(testEmail), testEmail, Some(true),
              Some(testAgentReferenceNumber), isSuccess = false, contactPreference = testContactPreference
            ))
          }
        }
      }
      "the registration request fails" should {
        "return a RegistrationFailure" in {
          val failureStatus = BAD_REQUEST
          val failureBody = "Some response from DES"

          val signUpRequest = SignUpRequest(
            vatNumber = testVatNumber,
            businessEntity = testLimitedCompany,
            signUpEmail = Some(testSignUpEmail),
            transactionEmail = testSignUpEmail,
            isDelegated = true,
            isMigratable = testIsMigratable,
            contactPreference = testContactPreference
          )

          mockRegisterBusinessEntity(testVatNumber, LimitedCompany(testCompanyNumber))(
            Future.successful(Left(RegisterWithMultipleIdsErrorResponse(failureStatus, failureBody)))
          )

          val res = await(TestSubmissionService.submitSignUpRequest(signUpRequest, enrolments))

          res.left.value shouldBe RegistrationFailure(failureStatus, failureBody)

          verifyAudit(RegisterWithMultipleIDsAuditModel(
            vatNumber = testVatNumber,
            companyNumber = Some(testCompanyNumber),
            businessEntity = LimitedCompanyKey,
            agentReferenceNumber = Some(testAgentReferenceNumber),
            isSuccess = false
          ))

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
                contactPreference = testContactPreference
              )

              mockRegisterBusinessEntity(testVatNumber, SoleTrader(testNino))(
                Future.successful(Right(RegisterWithMultipleIdsSuccess(testSafeId)))
              )
              mockSignUp(testSafeId,
                testVatNumber,
                Some(testEmail),
                emailVerified = Some(true),
                isPartialMigration = !testIsMigratable,
                contactPreference = testContactPreference
              )(
                Future.successful(Right(CustomerSignUpResponseSuccess))
              )
              mockRegisterEnrolment(testVatNumber, testSafeId)(Future.successful(Right(SuccessfulTaxEnrolment)))

              val res = await(TestSubmissionService.submitSignUpRequest(signUpRequest, enrolments))

              res.right.value shouldBe SignUpRequestSubmitted

              verifyAudit(RegisterWithMultipleIDsAuditModel(
                vatNumber = testVatNumber,
                nino = Some(testNino),
                businessEntity = SoleTraderKey,
                agentReferenceNumber = None,
                isSuccess = true
              ))
              verifyAudit(SignUpAuditModel(testSafeId, testVatNumber, Some(testEmail), testEmail,
                Some(true), None, isSuccess = true, contactPreference = testContactPreference
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
                contactPreference = testContactPreference
              )

              mockRegisterBusinessEntity(testVatNumber, LimitedCompany(testCompanyNumber))(
                Future.successful(Right(RegisterWithMultipleIdsSuccess(testSafeId)))
              )
              mockSignUp(testSafeId,
                testVatNumber,
                Some(testEmail),
                emailVerified = Some(true),
                isPartialMigration = !testIsMigratable,
                contactPreference = testContactPreference
              )(
                Future.successful(Right(CustomerSignUpResponseSuccess))
              )
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
              verifyAudit(SignUpAuditModel(testSafeId, testVatNumber, Some(testEmail), testEmail,
                Some(true), None, isSuccess = true, contactPreference = testContactPreference
              ))
            }
            "return a SignUpRequestSubmitted for a general partnership sign up" in {
              val signUpRequest = SignUpRequest(
                vatNumber = testVatNumber,
                businessEntity = testGeneralPartnership,
                signUpEmail = Some(testSignUpEmail),
                transactionEmail = testSignUpEmail,
                isDelegated = false,
                isMigratable = testIsMigratable,
                contactPreference = testContactPreference
              )

              mockRegisterBusinessEntity(testVatNumber, testGeneralPartnership)(
                Future.successful(Right(RegisterWithMultipleIdsSuccess(testSafeId)))
              )
              mockSignUp(testSafeId,
                testVatNumber,
                Some(testEmail),
                emailVerified = Some(true),
                isPartialMigration = !testIsMigratable,
                contactPreference = testContactPreference
              )(
                Future.successful(Right(CustomerSignUpResponseSuccess))
              )
              mockRegisterEnrolment(testVatNumber, testSafeId)(Future.successful(Right(SuccessfulTaxEnrolment)))

              val res = await(TestSubmissionService.submitSignUpRequest(signUpRequest, enrolments))

              res.right.value shouldBe SignUpRequestSubmitted

              verifyAudit(RegisterWithMultipleIDsAuditModel(
                vatNumber = testVatNumber,
                sautr = Some(testUtr),
                businessEntity = GeneralPartnershipKey,
                agentReferenceNumber = None,
                isSuccess = true
              ))
              verifyAudit(SignUpAuditModel(testSafeId, testVatNumber, Some(testEmail), testEmail, Some(true),
                None, isSuccess = true, contactPreference = testContactPreference
              ))
            }
            "return a SignUpRequestSubmitted for a limited partnership sign up" in {
              val signUpRequest = SignUpRequest(
                vatNumber = testVatNumber,
                businessEntity = testLimitedPartnership,
                signUpEmail = Some(testSignUpEmail),
                transactionEmail = testSignUpEmail,
                isDelegated = false,
                isMigratable = testIsMigratable,
                contactPreference = testContactPreference
              )

              mockRegisterBusinessEntity(testVatNumber, testLimitedPartnership)(
                Future.successful(Right(RegisterWithMultipleIdsSuccess(testSafeId)))
              )
              mockSignUp(testSafeId,
                testVatNumber,
                Some(testEmail),
                emailVerified = Some(true),
                isPartialMigration = !testIsMigratable,
                contactPreference = testContactPreference
              )(
                Future.successful(Right(CustomerSignUpResponseSuccess))
              )
              mockRegisterEnrolment(testVatNumber, testSafeId)(Future.successful(Right(SuccessfulTaxEnrolment)))

              val res = await(TestSubmissionService.submitSignUpRequest(signUpRequest, enrolments))

              res.right.value shouldBe SignUpRequestSubmitted

              verifyAudit(RegisterWithMultipleIDsAuditModel(
                vatNumber = testVatNumber,
                sautr = Some(testUtr),
                companyNumber = Some(testCompanyNumber),
                businessEntity = LimitedPartnershipKey,
                agentReferenceNumber = None,
                isSuccess = true
              ))
              verifyAudit(SignUpAuditModel(testSafeId, testVatNumber, Some(testEmail), testEmail, Some(true),
                None, isSuccess = true, contactPreference = testContactPreference
              ))
            }
            "return a SignUpRequestSubmitted for a limited liability partnership sign up" in {
              val signUpRequest = SignUpRequest(
                vatNumber = testVatNumber,
                businessEntity = testLimitedLiabilityPartnership,
                signUpEmail = Some(testSignUpEmail),
                transactionEmail = testSignUpEmail,
                isDelegated = false,
                isMigratable = testIsMigratable,
                contactPreference = testContactPreference
              )

              mockRegisterBusinessEntity(testVatNumber, testLimitedLiabilityPartnership)(
                Future.successful(Right(RegisterWithMultipleIdsSuccess(testSafeId)))
              )
              mockSignUp(testSafeId,
                testVatNumber,
                Some(testEmail),
                emailVerified = Some(true),
                isPartialMigration = !testIsMigratable,
                contactPreference = testContactPreference
              )(
                Future.successful(Right(CustomerSignUpResponseSuccess))
              )
              mockRegisterEnrolment(testVatNumber, testSafeId)(Future.successful(Right(SuccessfulTaxEnrolment)))

              val res = await(TestSubmissionService.submitSignUpRequest(signUpRequest, enrolments))

              res.right.value shouldBe SignUpRequestSubmitted

              verifyAudit(RegisterWithMultipleIDsAuditModel(
                vatNumber = testVatNumber,
                sautr = Some(testUtr),
                companyNumber = Some(testCompanyNumber),
                businessEntity = LimitedLiabilityPartnershipKey,
                agentReferenceNumber = None,
                isSuccess = true
              ))
              verifyAudit(SignUpAuditModel(testSafeId, testVatNumber, Some(testEmail), testEmail, Some(true),
                None, isSuccess = true, contactPreference = testContactPreference
              ))
            }
            "return a SignUpRequestSubmitted for a scottish limited partnership sign up" in {
              val signUpRequest = SignUpRequest(
                vatNumber = testVatNumber,
                businessEntity = testScottishLimitedPartnership,
                signUpEmail = Some(testSignUpEmail),
                transactionEmail = testSignUpEmail,
                isDelegated = false,
                isMigratable = testIsMigratable,
                contactPreference = testContactPreference
              )

              mockRegisterBusinessEntity(testVatNumber, testScottishLimitedPartnership)(
                Future.successful(Right(RegisterWithMultipleIdsSuccess(testSafeId)))
              )
              mockSignUp(testSafeId,
                testVatNumber,
                Some(testEmail),
                emailVerified = Some(true),
                isPartialMigration = !testIsMigratable,
                contactPreference = testContactPreference
              )(
                Future.successful(Right(CustomerSignUpResponseSuccess))
              )
              mockRegisterEnrolment(testVatNumber, testSafeId)(Future.successful(Right(SuccessfulTaxEnrolment)))

              val res = await(TestSubmissionService.submitSignUpRequest(signUpRequest, enrolments))

              res.right.value shouldBe SignUpRequestSubmitted

              verifyAudit(RegisterWithMultipleIDsAuditModel(
                vatNumber = testVatNumber,
                sautr = Some(testUtr),
                companyNumber = Some(testCompanyNumber),
                businessEntity = ScottishLimitedPartnershipKey,
                agentReferenceNumber = None,
                isSuccess = true
              ))
              verifyAudit(SignUpAuditModel(testSafeId, testVatNumber, Some(testEmail), testEmail, Some(true),
                None, isSuccess = true, contactPreference = testContactPreference
              ))
            }
            "return a SignUpRequestSubmitted for a VAT group sign up" in {
              val signUpRequest = SignUpRequest(
                vatNumber = testVatNumber,
                businessEntity = VatGroup,
                signUpEmail = Some(testSignUpEmail),
                transactionEmail = testSignUpEmail,
                isDelegated = false,
                isMigratable = testIsMigratable,
                contactPreference = testContactPreference
              )

              mockRegisterBusinessEntity(testVatNumber, VatGroup)(
                Future.successful(Right(RegisterWithMultipleIdsSuccess(testSafeId)))
              )
              mockSignUp(testSafeId,
                testVatNumber,
                Some(testEmail),
                emailVerified = Some(true),
                isPartialMigration = !testIsMigratable,
                contactPreference = testContactPreference
              )(
                Future.successful(Right(CustomerSignUpResponseSuccess))
              )
              mockRegisterEnrolment(testVatNumber, testSafeId)(Future.successful(Right(SuccessfulTaxEnrolment)))

              val res = await(TestSubmissionService.submitSignUpRequest(signUpRequest, enrolments))

              res.right.value shouldBe SignUpRequestSubmitted

              verifyAudit(RegisterWithMultipleIDsAuditModel(
                vatNumber = testVatNumber,
                sautr = None,
                companyNumber = None,
                businessEntity = VatGroupKey,
                agentReferenceNumber = None,
                isSuccess = true
              ))
              verifyAudit(SignUpAuditModel(testSafeId, testVatNumber, Some(testEmail), testEmail, Some(true),
                None, isSuccess = true, contactPreference = testContactPreference
              ))
            }
            "return a SignUpRequestSubmitted for a Division sign up" in {
              val signUpRequest = SignUpRequest(
                vatNumber = testVatNumber,
                businessEntity = AdministrativeDivision,
                signUpEmail = Some(testSignUpEmail),
                transactionEmail = testSignUpEmail,
                isDelegated = false,
                isMigratable = testIsMigratable,
                contactPreference = testContactPreference
              )

              mockRegisterBusinessEntity(testVatNumber, AdministrativeDivision)(
                Future.successful(Right(RegisterWithMultipleIdsSuccess(testSafeId)))
              )
              mockSignUp(testSafeId,
                testVatNumber,
                Some(testEmail),
                emailVerified = Some(true),
                isPartialMigration = !testIsMigratable,
                contactPreference = testContactPreference
              )(
                Future.successful(Right(CustomerSignUpResponseSuccess))
              )
              mockRegisterEnrolment(testVatNumber, testSafeId)(Future.successful(Right(SuccessfulTaxEnrolment)))

              val res = await(TestSubmissionService.submitSignUpRequest(signUpRequest, enrolments))

              res.right.value shouldBe SignUpRequestSubmitted

              verifyAudit(RegisterWithMultipleIDsAuditModel(
                vatNumber = testVatNumber,
                sautr = None,
                companyNumber = None,
                businessEntity = AdministrativeDivisionKey,
                agentReferenceNumber = None,
                isSuccess = true
              ))
              verifyAudit(SignUpAuditModel(testSafeId, testVatNumber, Some(testEmail), testEmail, Some(true),
                None, isSuccess = true, contactPreference = testContactPreference
              ))
            }
            "return a SignUpRequestSubmitted for an Unincorporated Associations sign up" in {
              val signUpRequest = SignUpRequest(
                vatNumber = testVatNumber,
                businessEntity = UnincorporatedAssociation,
                signUpEmail = Some(testSignUpEmail),
                transactionEmail = testSignUpEmail,
                isDelegated = false,
                isMigratable = testIsMigratable,
                contactPreference = testContactPreference
              )

              mockRegisterBusinessEntity(testVatNumber, UnincorporatedAssociation)(
                Future.successful(Right(RegisterWithMultipleIdsSuccess(testSafeId)))
              )
              mockSignUp(testSafeId,
                testVatNumber,
                Some(testEmail),
                emailVerified = Some(true),
                isPartialMigration = !testIsMigratable,
                contactPreference = testContactPreference
              )(
                Future.successful(Right(CustomerSignUpResponseSuccess))
              )
              mockRegisterEnrolment(testVatNumber, testSafeId)(Future.successful(Right(SuccessfulTaxEnrolment)))

              val res = await(TestSubmissionService.submitSignUpRequest(signUpRequest, enrolments))

              res.right.value shouldBe SignUpRequestSubmitted

              verifyAudit(RegisterWithMultipleIDsAuditModel(
                vatNumber = testVatNumber,
                sautr = None,
                companyNumber = None,
                businessEntity = UnincorporatedAssociationKey,
                agentReferenceNumber = None,
                isSuccess = true
              ))
              verifyAudit(SignUpAuditModel(testSafeId, testVatNumber, Some(testEmail), testEmail, Some(true),
                None, isSuccess = true, contactPreference = testContactPreference
              ))
            }
            "return a SignUpRequestSubmitted for a Registered Society sign up" in {
              val signUpRequest = SignUpRequest(
                vatNumber = testVatNumber,
                businessEntity = testRegisteredSociety,
                signUpEmail = Some(testSignUpEmail),
                transactionEmail = testSignUpEmail,
                isDelegated = false,
                isMigratable = testIsMigratable,
                contactPreference = testContactPreference
              )

              mockRegisterBusinessEntity(testVatNumber, testRegisteredSociety)(
                Future.successful(Right(RegisterWithMultipleIdsSuccess(testSafeId)))
              )
              mockSignUp(testSafeId,
                testVatNumber,
                Some(testEmail),
                emailVerified = Some(true),
                isPartialMigration = !testIsMigratable,
                contactPreference = testContactPreference
              )(
                Future.successful(Right(CustomerSignUpResponseSuccess))
              )
              mockRegisterEnrolment(testVatNumber, testSafeId)(Future.successful(Right(SuccessfulTaxEnrolment)))

              val res = await(TestSubmissionService.submitSignUpRequest(signUpRequest, enrolments))

              res.right.value shouldBe SignUpRequestSubmitted

              verifyAudit(RegisterWithMultipleIDsAuditModel(
                vatNumber = testVatNumber,
                sautr = None,
                companyNumber = Some(testCompanyNumber),
                businessEntity = RegisteredSocietyKey,
                agentReferenceNumber = None,
                isSuccess = true
              ))
              verifyAudit(SignUpAuditModel(testSafeId, testVatNumber, Some(testEmail), testEmail, Some(true),
                None, isSuccess = true, contactPreference = testContactPreference
              ))
            }
            "return a SignUpRequestSubmitted for a Charity sign up" in {
              val signUpRequest = SignUpRequest(
                vatNumber = testVatNumber,
                businessEntity = Charity,
                signUpEmail = Some(testSignUpEmail),
                transactionEmail = testSignUpEmail,
                isDelegated = false,
                isMigratable = testIsMigratable,
                contactPreference = testContactPreference
              )

              mockRegisterBusinessEntity(testVatNumber, Charity)(
                Future.successful(Right(RegisterWithMultipleIdsSuccess(testSafeId)))
              )
              mockSignUp(testSafeId,
                testVatNumber,
                Some(testEmail),
                emailVerified = Some(true),
                isPartialMigration = !testIsMigratable,
                contactPreference = testContactPreference
              )(
                Future.successful(Right(CustomerSignUpResponseSuccess))
              )
              mockRegisterEnrolment(testVatNumber, testSafeId)(Future.successful(Right(SuccessfulTaxEnrolment)))

              val res = await(TestSubmissionService.submitSignUpRequest(signUpRequest, enrolments))

              res.right.value shouldBe SignUpRequestSubmitted

              verifyAudit(RegisterWithMultipleIDsAuditModel(
                vatNumber = testVatNumber,
                sautr = None,
                companyNumber = None,
                businessEntity = CharityKey,
                agentReferenceNumber = None,
                isSuccess = true
              ))
              verifyAudit(SignUpAuditModel(testSafeId, testVatNumber, Some(testEmail), testEmail, Some(true),
                None, isSuccess = true, contactPreference = testContactPreference
              ))
            }
            "return a SignUpRequestSubmitted for a Government Organisation sign up" in {
              val signUpRequest = SignUpRequest(
                vatNumber = testVatNumber,
                businessEntity = GovernmentOrganisation,
                signUpEmail = Some(testSignUpEmail),
                transactionEmail = testSignUpEmail,
                isDelegated = false,
                isMigratable = testIsMigratable,
                contactPreference = testContactPreference
              )

              mockRegisterBusinessEntity(testVatNumber, GovernmentOrganisation)(
                Future.successful(Right(RegisterWithMultipleIdsSuccess(testSafeId)))
              )
              mockSignUp(testSafeId,
                testVatNumber,
                Some(testEmail),
                emailVerified = Some(true),
                isPartialMigration = !testIsMigratable,
                contactPreference = testContactPreference
              )(
                Future.successful(Right(CustomerSignUpResponseSuccess))
              )
              mockRegisterEnrolment(testVatNumber, testSafeId)(Future.successful(Right(SuccessfulTaxEnrolment)))

              val res = await(TestSubmissionService.submitSignUpRequest(signUpRequest, enrolments))

              res.right.value shouldBe SignUpRequestSubmitted

              verifyAudit(RegisterWithMultipleIDsAuditModel(
                vatNumber = testVatNumber,
                businessEntity = GovernmentOrganisationKey,
                agentReferenceNumber = None,
                isSuccess = true
              ))
              verifyAudit(SignUpAuditModel(testSafeId, testVatNumber, Some(testEmail), testEmail,
                Some(true), None, isSuccess = true, contactPreference = testContactPreference
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
                contactPreference = testContactPreference
              )


              mockRegisterBusinessEntity(testVatNumber, LimitedCompany(testCompanyNumber))(
                Future.successful(Right(RegisterWithMultipleIdsSuccess(testSafeId)))
              )
              mockSignUp(testSafeId,
                testVatNumber,
                Some(testEmail),
                emailVerified = Some(true),
                isPartialMigration = !testIsMigratable,
                contactPreference = testContactPreference
              )(
                Future.successful(Right(CustomerSignUpResponseSuccess))
              )
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
              verifyAudit(SignUpAuditModel(testSafeId, testVatNumber, Some(testEmail), testEmail,
                Some(true), None, isSuccess = true, contactPreference = testContactPreference
              ))
            }
          }
        }
        "the sign up request fails" should {
          "return a SignUpFailure" in {
            val failureStatus = BAD_REQUEST
            val failureBody = "Some response from DES"

            val signUpRequest = SignUpRequest(
              vatNumber = testVatNumber,
              businessEntity = testLimitedCompany,
              signUpEmail = Some(testSignUpEmail),
              transactionEmail = testSignUpEmail,
              isDelegated = false,
              isMigratable = testIsMigratable,
              contactPreference = testContactPreference
            )


            mockRegisterBusinessEntity(testVatNumber, LimitedCompany(testCompanyNumber))(
              Future.successful(Right(RegisterWithMultipleIdsSuccess(testSafeId)))
            )
            mockSignUp(testSafeId,
              testVatNumber,
              Some(testEmail),
              emailVerified = Some(true),
              isPartialMigration = !testIsMigratable,
              contactPreference = testContactPreference
            )(
              Future.successful(Left(CustomerSignUpResponseFailure(failureStatus, failureBody)))
            )

            val res = await(TestSubmissionService.submitSignUpRequest(signUpRequest, enrolments))

            res.left.value shouldBe SignUpFailure(failureStatus, failureBody)

            verifyAudit(RegisterWithMultipleIDsAuditModel(
              vatNumber = testVatNumber,
              companyNumber = Some(testCompanyNumber),
              businessEntity = LimitedCompanyKey,
              agentReferenceNumber = None,
              isSuccess = true
            ))
            verifyAudit(SignUpAuditModel(testSafeId, testVatNumber, Some(testEmail), testEmail,
              Some(true), None, isSuccess = false, contactPreference = testContactPreference
            ))
          }
        }
      }
      "the registration request fails" should {
        "return a RegistrationFailure" in {
          val failureStatus = BAD_REQUEST
          val failureBody = "Some response from DES"

          val signUpRequest = SignUpRequest(
            vatNumber = testVatNumber,
            businessEntity = testLimitedCompany,
            signUpEmail = Some(testSignUpEmail),
            transactionEmail = testSignUpEmail,
            isDelegated = false,
            isMigratable = testIsMigratable,
            contactPreference = testContactPreference
          )

          mockRegisterBusinessEntity(testVatNumber, LimitedCompany(testCompanyNumber))(
            Future.successful(Left(RegisterWithMultipleIdsErrorResponse(failureStatus, failureBody)))
          )

          val res = await(TestSubmissionService.submitSignUpRequest(signUpRequest, enrolments))

          res.left.value shouldBe RegistrationFailure(failureStatus, failureBody)

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
