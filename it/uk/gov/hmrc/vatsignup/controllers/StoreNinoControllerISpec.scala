/*
 * Copyright 2019 HM Revenue & Customs
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
import uk.gov.hmrc.vatsignup.helpers.IntegrationTestConstants._
import uk.gov.hmrc.vatsignup.helpers.servicemocks.AuthStub.{stubAuth, successfulAuthResponse}
import uk.gov.hmrc.vatsignup.helpers.{ComponentSpecBase, CustomMatchers, TestSubmissionRequestRepository}
import uk.gov.hmrc.vatsignup.models.{AuthProfile, IRSA, NinoSource, UserEntered}

import scala.concurrent.ExecutionContext.Implicits.global


class StoreNinoControllerISpec extends ComponentSpecBase with CustomMatchers with TestSubmissionRequestRepository {

  private def requestBody(ninoSource: NinoSource) = Json.obj("nino" -> testNino, "ninoSource" -> ninoSource)

  "PUT /subscription-request/:vrn/national-insurance-number" should {
    "return NoContent" when {
      "Nino is stored successfully in Mongo when Nino Source is User Entered" in {
        stubAuth(OK, successfulAuthResponse())

        await(submissionRequestRepo.upsertVatNumber(testVatNumber, isMigratable = true, isDirectDebit = false))

        val res = put(s"/subscription-request/vat-number/$testVatNumber/national-insurance-number")(requestBody(UserEntered))

        res should have(
          httpStatus(NO_CONTENT),
          emptyBody
        )
        await(submissionRequestRepo.findById(testVatNumber)).get.ninoSource shouldBe Some(UserEntered)

      }
      "Nino is stored successfully in Mongo when Nino Source is IRSA" in {
        stubAuth(OK, successfulAuthResponse())

        await(submissionRequestRepo.upsertVatNumber(testVatNumber, isMigratable = true, isDirectDebit = false))

        val res = put(s"/subscription-request/vat-number/$testVatNumber/national-insurance-number")(requestBody(IRSA))

        res should have(
          httpStatus(NO_CONTENT),
          emptyBody
        )
        await(submissionRequestRepo.findById(testVatNumber)).get.ninoSource shouldBe Some(IRSA)

      }
      "Nino is stored successfully in Mongo when Nino Source is Auth Profile" in {
        stubAuth(OK, successfulAuthResponse())

        await(submissionRequestRepo.upsertVatNumber(testVatNumber, isMigratable = true, isDirectDebit = false))

        val res = put(s"/subscription-request/vat-number/$testVatNumber/national-insurance-number")(requestBody(AuthProfile))

        res should have(
          httpStatus(NO_CONTENT),
          emptyBody
        )
        await(submissionRequestRepo.findById(testVatNumber)).get.ninoSource shouldBe Some(AuthProfile)

      }
    }
    "return Not Found" when {
      "The VAT number is not found in Mongo" in {
        stubAuth(OK, successfulAuthResponse())

        val res = put(s"/subscription-request/vat-number/$testVatNumber/national-insurance-number")(requestBody(UserEntered))

        res should have(
          httpStatus(NOT_FOUND)
        )
      }
    }
    "return Bad Request" when {
      "Empty body or Invalid Json" in {
        stubAuth(OK, successfulAuthResponse())
        val res = put(s"/subscription-request/vat-number/$testVatNumber/national-insurance-number")(Json.obj())

        res should have(
          httpStatus(BAD_REQUEST)
        )
      }
    }
  }
}
