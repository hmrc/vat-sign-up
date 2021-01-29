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

import java.util.NoSuchElementException

import org.scalatest.{EitherValues, Matchers, WordSpec}
import play.api.test.Helpers._
import reactivemongo.api.commands.UpdateWriteResult
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}
import uk.gov.hmrc.vatsignup.config.mocks.MockConfig
import uk.gov.hmrc.vatsignup.connectors.mocks.MockEmailPasscodeVerificationConnector
import uk.gov.hmrc.vatsignup.helpers.TestConstants._
import uk.gov.hmrc.vatsignup.httpparsers.EmailPasscodeVerificationHttpParser._
import uk.gov.hmrc.vatsignup.models.StoreTransactionEmailRequest
import uk.gov.hmrc.vatsignup.repositories.mocks.MockSubscriptionRequestRepository
import uk.gov.hmrc.vatsignup.services.NewStoreEmailService
import uk.gov.hmrc.vatsignup.services.NewStoreEmailService._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class NewStoreEmailServiceSpec extends WordSpec with Matchers with MockSubscriptionRequestRepository
  with MockEmailPasscodeVerificationConnector with MockConfig with EitherValues {

  object TestService extends NewStoreEmailService(
    mockSubscriptionRequestRepository,
    mockEmailPasscodeVerificationConnector
  )

  private implicit val headerCarrier: HeaderCarrier = HeaderCarrier()

  val testRequest: StoreTransactionEmailRequest = StoreTransactionEmailRequest(
    testEmail,
    testPasscode
  )

  "storeTransactionEmail" should {
    "return StoreEmailSuccess if the email is verified and stored successfully" in {
      mockVerifyEmailPasscode(testEmail, testPasscode)(Future.successful(EmailVerifiedSuccessfully))
      upsertTransactionEmail(testVatNumber, testEmail)(Future.successful(mock[UpdateWriteResult]))

      val res = await(TestService.storeTransactionEmail(testVatNumber, testRequest))

      res.right.value shouldBe StoreEmailSuccess
    }

    "return StoreEmailSuccess if the email is already verified and stored successfully" in {
      mockVerifyEmailPasscode(testEmail, testPasscode)(Future.successful(EmailAlreadyVerified))
      upsertTransactionEmail(testVatNumber, testEmail)(Future.successful(mock[UpdateWriteResult]))

      val res = await(TestService.storeTransactionEmail(testVatNumber, testRequest))

      res.right.value shouldBe StoreEmailSuccess
    }

    "return EmailVerificationFailure with the mismatch key if the passcode can't be matched" in {
      mockVerifyEmailPasscode(testEmail, testPasscode)(Future.successful(PasscodeMismatch))

      val res = await(TestService.storeTransactionEmail(testVatNumber, testRequest))

      res.left.value shouldBe EmailVerificationFailure(passcodeMismatchKey)
    }

    "return EmailVerificationFailure with the not found key if the passcode can't be found" in {
      mockVerifyEmailPasscode(testEmail, testPasscode)(Future.successful(PasscodeNotFound))

      val res = await(TestService.storeTransactionEmail(testVatNumber, testRequest))

      res.left.value shouldBe EmailVerificationFailure(passcodeNotFoundKey)
    }

    "return EmailVerificationFailure with the max attempts key if the email verification has been attempted too many times" in {
      mockVerifyEmailPasscode(testEmail, testPasscode)(Future.successful(MaxAttemptsExceeded))

      val res = await(TestService.storeTransactionEmail(testVatNumber, testRequest))

      res.left.value shouldBe EmailVerificationFailure(maxAttemptsExceededKey)
    }

    "return EmailDatabaseFailureNoVATNumber if the email is verified but the VRN can't be found" in {
      mockVerifyEmailPasscode(testEmail, testPasscode)(Future.successful(EmailAlreadyVerified))
      upsertTransactionEmail(testVatNumber, testEmail)(Future.failed(new NoSuchElementException))

      val res = await(TestService.storeTransactionEmail(testVatNumber, testRequest))

      res.left.value shouldBe EmailDatabaseFailureNoVATNumber
    }

    "return EmailDatabaseFailure if the email is verified but the repository store fails" in {
      mockVerifyEmailPasscode(testEmail, testPasscode)(Future.successful(EmailAlreadyVerified))
      upsertTransactionEmail(testVatNumber, testEmail)(Future.failed(new InternalServerException("")))

      val res = await(TestService.storeTransactionEmail(testVatNumber, testRequest))

      res.left.value shouldBe EmailDatabaseFailure
    }
  }
}
