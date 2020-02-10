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

class StoreEmailControllerISpec extends ComponentSpecBase with CustomMatchers with TestSubmissionRequestRepository {

  val delegatedContinueUrl = app.injector.instanceOf[AppConfig].delegatedVerifyEmailContinueUrl
  val principalContinueUrl = app.injector.instanceOf[AppConfig].principalVerifyEmailContinueUrl

  "PUT /subscription-request/vat-number/:vrn/email" when {
    "the vat number exists" when {
      "the company number has been stored successfully" when {
        "the email verification request has been sent successfully" when {
          "the user is an agent" should {
            "return OK with the verification state" in {
              stubAuth(OK, successfulAuthResponse(agentEnrolment))

              await(submissionRequestRepo.upsertVatNumber(testVatNumber, isMigratable = true, isDirectDebit = false))
              stubVerifyEmail(testEmail, delegatedContinueUrl)(CREATED)

              val res = put(s"/subscription-request/vat-number/$testVatNumber/email")(Json.obj("email" -> testEmail))

              res should have(
                httpStatus(OK),
                jsonBodyAs(Json.obj(EmailVerifiedKey -> false))
              )
            }
          }
          "the user is on the principal journey" should {
            "return OK with the verification state" in {
              stubAuth(OK, successfulAuthResponse(vatDecEnrolment()))

              await(submissionRequestRepo.upsertVatNumber(testVatNumber, isMigratable = true, isDirectDebit = false))
              stubVerifyEmail(testEmail, principalContinueUrl)(CREATED)

              val res = put(s"/subscription-request/vat-number/$testVatNumber/email")(Json.obj("email" -> testEmail))

              res should have(
                httpStatus(OK),
                jsonBodyAs(Json.obj(EmailVerifiedKey -> false))
              )
            }
          }
        }
        "the email has already been verified" should {
          "return OK with the verification state as true" in {
            stubAuth(OK, successfulAuthResponse(vatDecEnrolment()))

            await(submissionRequestRepo.upsertVatNumber(testVatNumber, isMigratable = true, isDirectDebit = false))
            stubVerifyEmail(testEmail, principalContinueUrl)(CONFLICT)

            val res = put(s"/subscription-request/vat-number/$testVatNumber/email")(Json.obj("email" -> testEmail))

            res should have(
              httpStatus(OK),
              jsonBodyAs(Json.obj(EmailVerifiedKey -> true))
            )
          }
        }
      }
    }
    "the vat number does not exist" should {
      "return NOT_FOUND" in {
        stubAuth(OK, successfulAuthResponse())

        val res = put(s"/subscription-request/vat-number/$testVatNumber/email")(Json.obj("email" -> testEmail))

        res should have(
          httpStatus(NOT_FOUND)
        )
      }
    }

    "the json is invalid" should {
      "return BAD_REQUEST" in {
        stubAuth(OK, successfulAuthResponse())

        val res = put(s"/subscription-request/vat-number/$testVatNumber/email")(Json.obj())

        res should have(
          httpStatus(BAD_REQUEST)
        )
      }
    }
  }

}
