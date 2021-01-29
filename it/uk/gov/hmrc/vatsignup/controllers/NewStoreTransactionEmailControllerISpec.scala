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

import play.api.libs.json.Json
import play.api.test.Helpers._
import uk.gov.hmrc.vatsignup.controllers.NewStoreTransactionEmailController.reasonKey
import uk.gov.hmrc.vatsignup.helpers.IntegrationTestConstants.{testEmail, testVatNumber}
import uk.gov.hmrc.vatsignup.helpers.servicemocks.AuthStub.{stubAuth, successfulAuthResponse}
import uk.gov.hmrc.vatsignup.helpers.servicemocks.EmailVerificationStub.stubVerifyEmailPasscode
import uk.gov.hmrc.vatsignup.helpers.{ComponentSpecBase, CustomMatchers, TestSubmissionRequestRepository}
import uk.gov.hmrc.vatsignup.httpparsers.EmailPasscodeVerificationHttpParser.{codeKey, maxAttemptsExceededKey, passcodeMismatchKey, passcodeNotFoundKey}

class NewStoreTransactionEmailControllerISpec extends ComponentSpecBase with CustomMatchers with TestSubmissionRequestRepository {

  val testPasscode = "testPasscode"

  "PUT /subscription-request/vat-number/:vatNumber/store-transaction-email" when {
    "the vat number exists" when {
      "the call is principal" should {
        "return OK if the passcode is valid" in {
          stubAuth(OK, successfulAuthResponse())

          await(submissionRequestRepo.upsertVatNumber(testVatNumber, isMigratable = true, isDirectDebit = false))
          stubVerifyEmailPasscode(testEmail, testPasscode)(CREATED)

          val res = put(s"/subscription-request/vat-number/$testVatNumber/store-transaction-email")(
            Json.obj(
              "transactionEmail" -> testEmail,
              "passCode" -> testPasscode
            ))

          res should have(
            httpStatus(CREATED)
          )
        }

        "return OK if the email is already verified" in {
          stubAuth(OK, successfulAuthResponse())

          await(submissionRequestRepo.upsertVatNumber(testVatNumber, isMigratable = true, isDirectDebit = false))
          stubVerifyEmailPasscode(testEmail, testPasscode)(NO_CONTENT)

          val res = put(s"/subscription-request/vat-number/$testVatNumber/store-transaction-email")(
            Json.obj(
              "transactionEmail" -> testEmail,
              "passCode" -> testPasscode
            ))

          res should have(
            httpStatus(CREATED)
          )
        }

        "return BAD_GATEWAY with correct reason if the passcode is not matched" in {
          stubAuth(OK, successfulAuthResponse())

          await(submissionRequestRepo.upsertVatNumber(testVatNumber, isMigratable = true, isDirectDebit = false))
          stubVerifyEmailPasscode(testEmail, testPasscode)(NOT_FOUND, Json.obj(codeKey -> passcodeMismatchKey))

          val res = put(s"/subscription-request/vat-number/$testVatNumber/store-transaction-email")(
            Json.obj(
              "transactionEmail" -> testEmail,
              "passCode" -> testPasscode
            ))

          res should have(
            httpStatus(BAD_GATEWAY),
            jsonBodyAs(Json.obj(reasonKey -> passcodeMismatchKey))
          )
        }

        "return BAD_GATEWAY with correct reason if the passcode is not found" in {
          stubAuth(OK, successfulAuthResponse())

          await(submissionRequestRepo.upsertVatNumber(testVatNumber, isMigratable = true, isDirectDebit = false))
          stubVerifyEmailPasscode(testEmail, testPasscode)(NOT_FOUND, Json.obj(codeKey -> passcodeNotFoundKey))

          val res = put(s"/subscription-request/vat-number/$testVatNumber/store-transaction-email")(
            Json.obj(
              "transactionEmail" -> testEmail,
              "passCode" -> testPasscode
            ))

          res should have(
            httpStatus(BAD_GATEWAY),
            jsonBodyAs(Json.obj(reasonKey -> passcodeNotFoundKey))
          )
        }

        "return BAD_GATEWAY with correct reason if the passcode matching is attempted too many times" in {
          stubAuth(OK, successfulAuthResponse())

          await(submissionRequestRepo.upsertVatNumber(testVatNumber, isMigratable = true, isDirectDebit = false))
          stubVerifyEmailPasscode(testEmail, testPasscode)(FORBIDDEN, Json.obj(codeKey -> maxAttemptsExceededKey))

          val res = put(s"/subscription-request/vat-number/$testVatNumber/store-transaction-email")(
            Json.obj(
              "transactionEmail" -> testEmail,
              "passCode" -> testPasscode
            ))

          res should have(
            httpStatus(BAD_GATEWAY),
            jsonBodyAs(Json.obj(reasonKey -> maxAttemptsExceededKey))
          )
        }
      }
    }

    "the vat number does not exist" should {
      "return NOT_FOUND" in {
        stubAuth(OK, successfulAuthResponse())

        stubVerifyEmailPasscode(testEmail, testPasscode)(CREATED)

        val res = put(s"/subscription-request/vat-number/$testVatNumber/store-transaction-email")(
          Json.obj(
            "transactionEmail" -> testEmail,
            "passCode" -> testPasscode
          ))

        res should have(
          httpStatus(NOT_FOUND)
        )
      }
    }

    "the json is invalid" should {
      "return BAD_REQUEST" in {
        stubAuth(OK, successfulAuthResponse())

        val res = put(s"/subscription-request/vat-number/$testVatNumber/store-transaction-email")(Json.obj())

        res should have(
          httpStatus(BAD_REQUEST)
        )
      }
    }
  }
}
