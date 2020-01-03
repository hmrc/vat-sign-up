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

package uk.gov.hmrc.vatsignup.helpers.servicemocks

import play.api.http.Status._
import play.api.libs.json.Json
import uk.gov.hmrc.vatsignup.config.Constants.EmailVerification._
import uk.gov.hmrc.vatsignup.helpers.IntegrationTestConstants.testEmail

object EmailVerificationStub extends WireMockMethods {
  private val emailVerifiedUri = "/email-verification/verified-email-check"

  private val verifyEmailUri = "/email-verification/verification-requests"

  def stubGetEmailVerified(email: String): Unit =
    when(
      method = POST,
      uri = emailVerifiedUri,
      body = Json.obj("email" -> email)
    ) thenReturn(OK)

  def stubVerifyEmail(emailAddress: String, continueUrl: String)(response: Int): Unit =
    when(
      method = POST,
      uri = verifyEmailUri,
      body = verifyEmailBody(emailAddress, continueUrl)
    ) thenReturn response

  private def verifyEmailBody(emailAddress: String, continueUrl: String) =
    Json.obj(
      EmailKey -> emailAddress,
      TemplateIdKey -> "verifyEmailAddress",
      TemplateParametersKey -> Json.obj(),
      LinkExpiryDurationKey -> "P3D",
      ContinueUrlKey -> continueUrl
    )

}
