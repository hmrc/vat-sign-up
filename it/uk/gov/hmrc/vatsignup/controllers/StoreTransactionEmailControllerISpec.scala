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

import play.api.libs.json.Json
import play.api.test.Helpers._
import uk.gov.hmrc.vatsignup.config.AppConfig
import uk.gov.hmrc.vatsignup.config.Constants.EmailVerification.EmailVerifiedKey
import uk.gov.hmrc.vatsignup.helpers.IntegrationTestConstants._
import uk.gov.hmrc.vatsignup.helpers._
import uk.gov.hmrc.vatsignup.helpers.servicemocks.AuthStub._
import uk.gov.hmrc.vatsignup.helpers.servicemocks.EmailVerificationStub.stubVerifyEmail

class StoreTransactionEmailControllerISpec extends ComponentSpecBase with CustomMatchers with TestSubmissionRequestRepository {

  val agentContinueUrl: String = app.injector.instanceOf[AppConfig].agentVerifyEmailContinueUrl
  val principalContinueUrl: String = app.injector.instanceOf[AppConfig].principalVerifyEmailContinueUrl

  "PUT /subscription-request/vat-number/:vatNumber/transaction-email" when {
    "the vat number exists" when {
      "the email verification request has been sent successfully" when {
        "the call is delegated" should {
          "return OK with the verification state as false" in {
            stubAuth(OK, successfulAuthResponse(agentEnrolment))

            await(submissionRequestRepo.upsertVatNumber(testVatNumber, isMigratable = true, isDirectDebit = false))
            stubVerifyEmail(testEmail, agentContinueUrl)(CREATED)

            val res = put(s"/subscription-request/vat-number/$testVatNumber/transaction-email")(Json.obj("transactionEmail" -> testEmail))

            res should have(
              httpStatus(OK),
              jsonBodyAs(Json.obj(EmailVerifiedKey -> false))
            )
          }
        }
        "the call is principal" should {
          "return OK with the verification state as false" in {
            stubAuth(OK, successfulAuthResponse())

            await(submissionRequestRepo.upsertVatNumber(testVatNumber, isMigratable = true, isDirectDebit = false))
            stubVerifyEmail(testEmail, principalContinueUrl)(CREATED)

            val res = put(s"/subscription-request/vat-number/$testVatNumber/transaction-email")(Json.obj("transactionEmail" -> testEmail))

            res should have(
              httpStatus(OK),
              jsonBodyAs(Json.obj(EmailVerifiedKey -> false))
            )
          }
        }
      }
      "the email has already been verified" should {
        "return OK with the verification state as true" in {
          stubAuth(OK, successfulAuthResponse(agentEnrolment))

          await(submissionRequestRepo.upsertVatNumber(testVatNumber, isMigratable = true, isDirectDebit = false))
          stubVerifyEmail(testEmail, agentContinueUrl)(CONFLICT)

          val res = put(s"/subscription-request/vat-number/$testVatNumber/transaction-email")(Json.obj("transactionEmail" -> testEmail))

          res should have(
            httpStatus(OK),
            jsonBodyAs(Json.obj(EmailVerifiedKey -> true))
          )
        }
      }
    }
    "the vat number does not exist" should {
      "return NOT_FOUND" in {
        stubAuth(OK, successfulAuthResponse(agentEnrolment))

        val res = put(s"/subscription-request/vat-number/$testVatNumber/transaction-email")(Json.obj("transactionEmail" -> testEmail))

        res should have(
          httpStatus(NOT_FOUND)
        )
      }
    }
    "the json is invalid" should {
      "return BAD_REQUEST" in {
        stubAuth(OK, successfulAuthResponse(agentEnrolment))

        val res = put(s"/subscription-request/vat-number/$testVatNumber/transaction-email")(Json.obj())

        res should have(
          httpStatus(BAD_REQUEST)
        )
      }
    }
  }

  "PUT /subscription-request/vat-number/:vatNumber/store-transaction-email" when {
    "the vat number exists" when {
      "the call is principal" should {
        "return CREATED when email is stored successfully" in {
          stubAuth(OK, successfulAuthResponse())

          await(submissionRequestRepo.upsertVatNumber(testVatNumber, isMigratable = true, isDirectDebit = false))

          val res = put(s"/subscription-request/vat-number/$testVatNumber/store-transaction-email")(
            Json.obj(
              "transactionEmail" -> testEmail
            ))

          res should have(
            httpStatus(CREATED)
          )
        }
      }
    }

    "the vat number does not exist" should {
      "return NOT_FOUND" in {
        stubAuth(OK, successfulAuthResponse())

        val res = put(s"/subscription-request/vat-number/$testVatNumber/store-transaction-email")(
          Json.obj(
            "transactionEmail" -> testEmail
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
