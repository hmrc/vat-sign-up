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

package uk.gov.hmrc.vatsignup.connectors

import helpers.WiremockHelper.stubPost
import play.api.http.Status.FORBIDDEN
import play.api.libs.json.Json
import play.api.test.Helpers._
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}
import uk.gov.hmrc.vatsignup.config.featureswitch.StubEmailVerification
import uk.gov.hmrc.vatsignup.helpers.ComponentSpecBase
import uk.gov.hmrc.vatsignup.httpparsers.EmailPasscodeVerificationHttpParser._

class EmailPasscodeVerificationConnectorISpec extends ComponentSpecBase {

  lazy val connector: EmailPasscodeVerificationConnector = app.injector.instanceOf[EmailPasscodeVerificationConnector]
  implicit val headerCarrier: HeaderCarrier = HeaderCarrier()

  lazy val testPasscode: String = "123456"
  lazy val testEmail: String = "test@test.com"

  "verifyEmailPasscode" should {
    "return the correct response" when {
      "the feature switch is enabled" when {
        "the stub returns CREATED" in {
          enable(StubEmailVerification)
          stubPost("/vat-sign-up/test-only/email-verification/verify-passcode", CREATED, "")

          val res = await(connector.verifyEmailPasscode(testEmail, testPasscode))

          res shouldBe EmailVerifiedSuccessfully
        }
      }

      "the feature switch is disabled" when {
        "the email verification API returns CREATED" in {
          disable(StubEmailVerification)
          stubPost("/email-verification/verify-passcode", CREATED, "")

          val res = await(connector.verifyEmailPasscode(testEmail, testPasscode))

          res shouldBe EmailVerifiedSuccessfully
        }

        "the email verification API returns NO_CONTENT" in {
          disable(StubEmailVerification)
          stubPost("/email-verification/verify-passcode", NO_CONTENT, "")

          val res = await(connector.verifyEmailPasscode(testEmail, testPasscode))

          res shouldBe EmailAlreadyVerified
        }

        "the email verification API returns NOT_FOUND with the passcode mismatch key" in {
          disable(StubEmailVerification)
          stubPost("/email-verification/verify-passcode", NOT_FOUND, Json.obj("code" -> passcodeMismatchKey).toString)

          val res = await(connector.verifyEmailPasscode(testEmail, testPasscode))

          res shouldBe PasscodeMismatch
        }

        "the email verification API returns NOT_FOUND with the passcode not found code" in {
          disable(StubEmailVerification)
          stubPost("/email-verification/verify-passcode", NOT_FOUND, Json.obj("code" -> passcodeNotFoundKey).toString)

          val res = await(connector.verifyEmailPasscode(testEmail, testPasscode))

          res shouldBe PasscodeNotFound
        }

        "the email verification API returns FORBIDDEN with the max attempts exceeded code" in {
          disable(StubEmailVerification)
          stubPost("/email-verification/verify-passcode", FORBIDDEN, Json.obj("code" -> maxAttemptsExceededKey).toString)

          val res = await(connector.verifyEmailPasscode(testEmail, testPasscode))

          res shouldBe MaxAttemptsExceeded
        }

        "the email verification API returns an unexpected response" in {
          disable(StubEmailVerification)
          stubPost("/email-verification/verify-passcode", INTERNAL_SERVER_ERROR, "")

          intercept[InternalServerException] {
            await(connector.verifyEmailPasscode(testEmail, testPasscode))
          }
        }
      }
    }
  }
}