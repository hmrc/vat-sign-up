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
import uk.gov.hmrc.vatsignup.helpers.IntegrationTestConstants._
import uk.gov.hmrc.vatsignup.helpers._
import uk.gov.hmrc.vatsignup.helpers.servicemocks.AuthStub._
import uk.gov.hmrc.vatsignup.helpers.servicemocks.GetCtReferenceStub.{ctReferenceBody, stubGetCtReference}

class StoreCompanyNumberControllerISpec extends ComponentSpecBase with CustomMatchers with TestSubmissionRequestRepository {

  "PUT /subscription-request/:vrn/company-number" should {
    "if vat number exists return no content when the company number has been stored successfully" in {
      stubAuth(OK, successfulAuthResponse())

      await(submissionRequestRepo.upsertVatNumber(testVatNumber, isMigratable = true))

      val res = put(s"/subscription-request/vat-number/$testVatNumber/company-number")(Json.obj("companyNumber" -> testCompanyNumber))

      res should have(
        httpStatus(NO_CONTENT),
        emptyBody
      )
    }

    "if the vat number does not already exist then return NOT_FOUND" in {
      stubAuth(OK, successfulAuthResponse())

      val res = put(s"/subscription-request/vat-number/$testVatNumber/company-number")(Json.obj("companyNumber" -> testCompanyNumber))

      res should have(
        httpStatus(NOT_FOUND)
      )
    }

    "return BAD_REQUEST when the json is invalid" in {
      stubAuth(OK, successfulAuthResponse())

      val res = put(s"/subscription-request/vat-number/$testVatNumber/company-number")(Json.obj())

      res should have(
        httpStatus(BAD_REQUEST)
      )
    }

    "return NO_CONTENT when the provided CT reference matches the one returned by DES" in {
      stubAuth(OK, successfulAuthResponse())
      await(submissionRequestRepo.upsertVatNumber(testVatNumber, isMigratable = true))
      stubGetCtReference(testCompanyNumber)(OK, ctReferenceBody(testCtReference))

      val res = put(s"/subscription-request/vat-number/$testVatNumber/company-number")(Json.obj(
        "companyNumber" -> testCompanyNumber,
        "ctReference" -> testCtReference
      ))

      res should have(
        httpStatus(NO_CONTENT)
      )

    }
  }

}
