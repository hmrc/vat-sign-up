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

class StoreTransactionEmailWithRequestIdControllerISpec extends ComponentSpecBase with CustomMatchers with TestUnconfirmedSubmissionRequestRepository {

  val agentContinueUrl: String = app.injector.instanceOf[AppConfig].agentVerifyEmailContinueUrl

  override def beforeEach(): Unit = {
    import scala.concurrent.ExecutionContext.Implicits.global
    super.beforeEach()
    await(unconfirmedSubmissionRequestRepo.insert(UnconfirmedSubscriptionRequest(testToken)))
  }

  "POST /sign-up-request/request-id/:requestId/transaction-email" when {
      "the email verification request has been sent successfully" when {
        "return OK with the verification state as false" in {
          stubAuth(OK, successfulAuthResponse())
          stubVerifyEmail(testEmail, agentContinueUrl)(CREATED)

          val res = post(s"/sign-up-request/request-id/$testToken/transaction-email")(Json.obj("transactionEmail" -> testEmail))

          res should have(
            httpStatus(OK),
            jsonBodyAs(Json.obj(EmailVerifiedKey -> false))
          )
        }
      }
      "the email has already been verified" should {
        "return OK with the verification state as true" in {
          stubAuth(OK, successfulAuthResponse())
          stubVerifyEmail(testEmail, agentContinueUrl)(CONFLICT)

          val res = post(s"/sign-up-request/request-id/$testToken/transaction-email")(Json.obj("transactionEmail" -> testEmail))

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

        val res = post(s"/sign-up-request/request-id/$testToken/transaction-email")(Json.obj())

        res should have(
          httpStatus(BAD_REQUEST)
        )
      }
    }

}
