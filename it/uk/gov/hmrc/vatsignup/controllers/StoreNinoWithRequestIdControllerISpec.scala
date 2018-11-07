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

import java.time.LocalDate
import java.util.UUID

import play.api.http.Status._
import play.api.libs.json.{JsObject, JsValue, Json}
import uk.gov.hmrc.vatsignup.helpers.IntegrationTestConstants._
import uk.gov.hmrc.vatsignup.helpers._
import uk.gov.hmrc.vatsignup.helpers.servicemocks.AuthStub._
import uk.gov.hmrc.vatsignup.helpers.servicemocks.AuthenticatorStub._
import uk.gov.hmrc.vatsignup.models.NinoSource.ninoSourceFrontEndKey
import uk.gov.hmrc.vatsignup.models.{IRSA, UnconfirmedSubscriptionRequest, UserDetailsModel, UserEntered}

import scala.concurrent.ExecutionContext.Implicits.global

class StoreNinoWithRequestIdControllerISpec extends ComponentSpecBase
  with CustomMatchers with TestUnconfirmedSubmissionRequestRepository {

  override def beforeEach(): Unit = {
    import scala.concurrent.ExecutionContext.Implicits.global
    super.beforeEach()
    await(unconfirmedSubmissionRequestRepo.insert(UnconfirmedSubscriptionRequest(testToken)))
  }

  "POST /sign-up-request/:requestId/nino" when {
    "nino source is not supplied" should {
      val userDetails: UserDetailsModel = UserDetailsModel(
        UUID.randomUUID().toString,
        UUID.randomUUID().toString,
        LocalDate.now(),
        testNino
      )

      lazy val requestBody: JsValue = Json.toJson(userDetails).as[JsObject].deepMerge(Json.obj(ninoSourceFrontEndKey -> IRSA.toString))

      "if requestId exists return no content when the nino has been stored successfully" in {
        stubAuth(OK, successfulAuthResponse())
        stubMatchUser(userDetails)(matched = true)

        val res = post(s"/sign-up-request/request-id/$testToken/nino")(requestBody)

        res should have(
          httpStatus(NO_CONTENT),
          emptyBody
        )

        await(unconfirmedSubmissionRequestRepo.findById(testToken)).get.ninoSource shouldBe Some(IRSA)
      }

      "the nino source is UserEntered" should {
        lazy val requestBody: JsValue = Json.toJson(userDetails).as[JsObject].deepMerge(Json.obj(ninoSourceFrontEndKey -> UserEntered.toString))

        "if the user is not matched in CID then return FORBIDDEN" in {
          stubAuth(OK, successfulAuthResponse())
          stubMatchUser(userDetails)(matched = false)

          val res = post(s"/sign-up-request/request-id/$testToken/nino")(requestBody)

          res should have(
            httpStatus(FORBIDDEN)
          )
        }
      }

      "return BAD_REQUEST when the json is invalid" in {
        stubAuth(OK, successfulAuthResponse())

        val res = post(s"/sign-up-request/request-id/$testToken/nino")(Json.obj())

        res should have(
          httpStatus(BAD_REQUEST)
        )
      }
    }
  }

}
