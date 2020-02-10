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

package uk.gov.hmrc.vatsignup.service

import org.scalatest.EitherValues
import play.api.test.Helpers._
import reactivemongo.api.commands.UpdateWriteResult
import uk.gov.hmrc.auth.core.Enrolments
import uk.gov.hmrc.http.HeaderCarrier
import org.scalatest.{WordSpec, Matchers}
import uk.gov.hmrc.vatsignup.config.mocks.MockConfig
import uk.gov.hmrc.vatsignup.connectors.mocks.MockEmailVerificationConnector
import uk.gov.hmrc.vatsignup.helpers.TestConstants._
import uk.gov.hmrc.vatsignup.httpparsers.CreateEmailVerificationRequestHttpParser._
import uk.gov.hmrc.vatsignup.repositories.mocks.MockSubscriptionRequestRepository
import uk.gov.hmrc.vatsignup.services.StoreEmailService
import uk.gov.hmrc.vatsignup.services.StoreEmailService._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class StoreEmailServiceSpec extends WordSpec with Matchers with MockSubscriptionRequestRepository
  with MockEmailVerificationConnector with MockConfig with EitherValues {

  object TestStoreEmailService extends StoreEmailService(
    mockSubscriptionRequestRepository,
    mockEmailVerificationConnector,
    mockConfig
  )

  private implicit val headerCarrier: HeaderCarrier = HeaderCarrier()

  private lazy val principalContinueUrl = mockConfig.principalVerifyEmailContinueUrl
  private lazy val delegatedContinueUrl = mockConfig.delegatedVerifyEmailContinueUrl
  private lazy val agentContinueUrl = mockConfig.agentVerifyEmailContinueUrl

  private val agentEnrolments = Enrolments(Set(testAgentEnrolment))
  private val individualEnrolments = Enrolments(Set.empty)


  "storeEmail" when {
    "the email stores successfully" when {
      "it is a delegated request" when {
        "the email verification is sent successfully" should {
          "return a StoreEmailSuccess with an emailVerified of false" in {
            mockUpsertEmail(testVatNumber, testEmail)(Future.successful(mock[UpdateWriteResult]))
            mockCreateEmailVerificationRequest(testEmail, delegatedContinueUrl)(Future.successful(Right(EmailVerificationRequestSent)))

            val res = await(TestStoreEmailService.storeEmail(testVatNumber, testEmail, agentEnrolments))

            res.right.value shouldBe StoreEmailSuccess(emailVerified = false)
          }
        }

        "the email address has already been verified" should {
          "return a StoreEmailSuccess with an emailVerified of true" in {
            mockUpsertEmail(testVatNumber, testEmail)(Future.successful(mock[UpdateWriteResult]))
            mockCreateEmailVerificationRequest(testEmail, delegatedContinueUrl)(Future.successful(Right(EmailAlreadyVerified)))

            val res = await(TestStoreEmailService.storeEmail(testVatNumber, testEmail, agentEnrolments))

            res.right.value shouldBe StoreEmailSuccess(emailVerified = true)
          }
        }

        "the email address verification request failed" should {
          "return an EmailVerificationFailure" in {
            mockUpsertEmail(testVatNumber, testEmail)(Future.successful(mock[UpdateWriteResult]))
            mockCreateEmailVerificationRequest(testEmail, delegatedContinueUrl)(Future.successful(Left(EmailVerificationRequestFailure(BAD_REQUEST, ""))))

            val res = await(TestStoreEmailService.storeEmail(testVatNumber, testEmail, agentEnrolments))

            res.left.value shouldBe EmailVerificationFailure
          }
        }
      }
      "it is a principal request" when {
        "the email verification is sent successfully" should {
          "return a StoreEmailSuccess with an emailVerified of false" in {
            mockUpsertEmail(testVatNumber, testEmail)(Future.successful(mock[UpdateWriteResult]))
            mockCreateEmailVerificationRequest(testEmail, principalContinueUrl)(Future.successful(Right(EmailVerificationRequestSent)))

            val res = await(TestStoreEmailService.storeEmail(testVatNumber, testEmail, individualEnrolments))

            res.right.value shouldBe StoreEmailSuccess(emailVerified = false)
          }
        }
      }
    }

    "the vat number has not previously been stored" should {
      "return an EmailDatabaseFailureNoVATNumber" in {
        mockUpsertEmail(testVatNumber, testEmail)(Future.failed(new NoSuchElementException))

        val res = await(TestStoreEmailService.storeEmail(testVatNumber, testEmail, individualEnrolments))

        res.left.value shouldBe EmailDatabaseFailureNoVATNumber
      }
    }

    "the email fails to store" should {
      "return an EmailDatabaseFailure" in {
        mockUpsertEmail(testVatNumber, testEmail)(Future.failed(new Exception))

        val res = await(TestStoreEmailService.storeEmail(testVatNumber, testEmail, individualEnrolments))

        res.left.value shouldBe EmailDatabaseFailure
      }
    }

  }

  "storeTransactionEmail" when {
    "the email stores successfully" when {
      "the call is delegated" when {
        "the email verification is sent successfully" should {
          "return a StoreEmailSuccess with an emailVerified of false" in {
            upsertTransactionEmail(testVatNumber, testEmail)(Future.successful(mock[UpdateWriteResult]))
            mockCreateEmailVerificationRequest(testEmail, agentContinueUrl)(Future.successful(Right(EmailVerificationRequestSent)))

            val res = await(TestStoreEmailService.storeTransactionEmail(testVatNumber, testEmail, Enrolments(Set(testAgentEnrolment))))

            res.right.value shouldBe StoreEmailSuccess(emailVerified = false)
          }
        }

        "the email address has already been verified" should {
          "return a StoreEmailSuccess with an emailVerified of true" in {
            upsertTransactionEmail(testVatNumber, testEmail)(Future.successful(mock[UpdateWriteResult]))
            mockCreateEmailVerificationRequest(testEmail, agentContinueUrl)(Future.successful(Right(EmailAlreadyVerified)))

            val res = await(TestStoreEmailService.storeTransactionEmail(testVatNumber, testEmail, Enrolments(Set(testAgentEnrolment))))

            res.right.value shouldBe StoreEmailSuccess(emailVerified = true)
          }
        }

        "the email address verification request failed" should {
          "return an EmailVerificationFailure" in {
            upsertTransactionEmail(testVatNumber, testEmail)(Future.successful(mock[UpdateWriteResult]))
            mockCreateEmailVerificationRequest(testEmail, agentContinueUrl)(Future.successful(Left(EmailVerificationRequestFailure(BAD_REQUEST, ""))))

            val res = await(TestStoreEmailService.storeTransactionEmail(testVatNumber, testEmail, Enrolments(Set(testAgentEnrolment))))

            res.left.value shouldBe EmailVerificationFailure
          }
        }
      }
      "the call is principal" when {
        "the email verification is sent successfully" should {
          "return a StoreEmailSuccess with an emailVerified of false" in {
            upsertTransactionEmail(testVatNumber, testEmail)(Future.successful(mock[UpdateWriteResult]))
            mockCreateEmailVerificationRequest(testEmail, principalContinueUrl)(Future.successful(Right(EmailVerificationRequestSent)))

            val res = await(TestStoreEmailService.storeTransactionEmail(testVatNumber, testEmail, Enrolments(Set.empty)))

            res.right.value shouldBe StoreEmailSuccess(emailVerified = false)
          }
        }

        "the email address has already been verified" should {
          "return a StoreEmailSuccess with an emailVerified of true" in {
            upsertTransactionEmail(testVatNumber, testEmail)(Future.successful(mock[UpdateWriteResult]))
            mockCreateEmailVerificationRequest(testEmail, principalContinueUrl)(Future.successful(Right(EmailAlreadyVerified)))

            val res = await(TestStoreEmailService.storeTransactionEmail(testVatNumber, testEmail, Enrolments(Set.empty)))

            res.right.value shouldBe StoreEmailSuccess(emailVerified = true)
          }
        }

        "the email address verification request failed" should {
          "return an EmailVerificationFailure" in {
            upsertTransactionEmail(testVatNumber, testEmail)(Future.successful(mock[UpdateWriteResult]))
            mockCreateEmailVerificationRequest(testEmail, principalContinueUrl)(Future.successful(Left(EmailVerificationRequestFailure(BAD_REQUEST, ""))))

            val res = await(TestStoreEmailService.storeTransactionEmail(testVatNumber, testEmail, Enrolments(Set.empty)))

            res.left.value shouldBe EmailVerificationFailure
          }
        }
      }
    }

    "the vat number has not previously been stored" should {
      "return an EmailDatabaseFailureNoVATNumber" in {
        upsertTransactionEmail(testVatNumber, testEmail)(Future.failed(new NoSuchElementException))

        val res = await(TestStoreEmailService.storeTransactionEmail(testVatNumber, testEmail, Enrolments(Set(testAgentEnrolment))))

        res.left.value shouldBe EmailDatabaseFailureNoVATNumber
      }
    }

    "the email fails to store" should {
      "return an EmailDatabaseFailure" in {
        upsertTransactionEmail(testVatNumber, testEmail)(Future.failed(new Exception))

        val res = await(TestStoreEmailService.storeTransactionEmail(testVatNumber, testEmail, Enrolments(Set(testAgentEnrolment))))

        res.left.value shouldBe EmailDatabaseFailure
      }
    }

  }

}
