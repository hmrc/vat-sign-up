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

package uk.gov.hmrc.vatsignup.controllers

import play.api.http.Status._
import play.api.libs.json.Json
import uk.gov.hmrc.vatsignup.config.AppConfig
import uk.gov.hmrc.vatsignup.config.Constants.EmailVerification.EmailVerifiedKey
import uk.gov.hmrc.vatsignup.helpers.IntegrationTestConstants._
import uk.gov.hmrc.vatsignup.helpers._
import uk.gov.hmrc.vatsignup.helpers.servicemocks.AuthStub._
import uk.gov.hmrc.vatsignup.helpers.servicemocks.EmailVerificationStub.stubVerifyEmail
import uk.gov.hmrc.vatsignup.models.UnconfirmedSubscriptionRequest

class StoreEmailWithRequestIdControllerISpec extends ComponentSpecBase with CustomMatchers with TestUnconfirmedSubmissionRequestRepository {

  val delegatedContinueUrl = app.injector.instanceOf[AppConfig].delegatedVerifyEmailContinueUrl
  val principalContinueUrl = app.injector.instanceOf[AppConfig].principalVerifyEmailContinueUrl

  override def beforeEach(): Unit = {
    import scala.concurrent.ExecutionContext.Implicits.global
    super.beforeEach()
    unconfirmedSubmissionRequestRepo.drop
    await(unconfirmedSubmissionRequestRepo.insert(UnconfirmedSubscriptionRequest(testToken)))
  }

  "POST /sign-up-request/request-id/:requestId/email" when {
    "the email verification request has been sent successfully" when {
      "the user is an agent" should {
        "return OK with the verification state" in {
          stubAuth(OK, successfulAuthResponse(agentEnrolment))
          stubVerifyEmail(testEmail, delegatedContinueUrl)(CREATED)

          val res = post(s"/sign-up-request/request-id/$testToken/email")(Json.obj("email" -> testEmail))

          res should have(
            httpStatus(OK),
            jsonBodyAs(Json.obj(EmailVerifiedKey -> false))
          )
        }
      }
      "the user is on the principal journey" should {
        "return OK with the verification state" in {
          stubAuth(OK, successfulAuthResponse(vatDecEnrolment))
          stubVerifyEmail(testEmail, principalContinueUrl)(CREATED)

          val res = post(s"/sign-up-request/request-id/$testToken/email")(Json.obj("email" -> testEmail))

          res should have(
            httpStatus(OK),
            jsonBodyAs(Json.obj(EmailVerifiedKey -> false))
          )
        }
      }
    }
    "the email has already been verified" should {
      "return OK with the verification state as true" in {
        stubAuth(OK, successfulAuthResponse(vatDecEnrolment))
        stubVerifyEmail(testEmail, principalContinueUrl)(CONFLICT)

        val res = post(s"/sign-up-request/request-id/$testToken/email")(Json.obj("email" -> testEmail))

        res should have(
          httpStatus(OK),
          jsonBodyAs(Json.obj(EmailVerifiedKey -> true))
        )
      }
    }
  }

  "the json is invalid" should {
    "return BAD_REQUEST" in {
      stubAuth(OK, successfulAuthResponse())

      val res = post(s"/sign-up-request/request-id/$testToken/email")(Json.obj())

      res should have(
        httpStatus(BAD_REQUEST)
      )
    }
  }

}
