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

import org.scalatest.BeforeAndAfterEach
import play.api.http.Status._
import play.api.libs.json.Json
import uk.gov.hmrc.vatsignup.helpers.IntegrationTestConstants._
import uk.gov.hmrc.vatsignup.helpers.servicemocks.AuthStub._
import uk.gov.hmrc.vatsignup.helpers.servicemocks.GetCtReferenceStub._
import uk.gov.hmrc.vatsignup.helpers.{ComponentSpecBase, CustomMatchers}

class CtReferenceLookupControllerISpec extends ComponentSpecBase with BeforeAndAfterEach with CustomMatchers {

  "/subscription-request/ct-reference-check" when {
    "the ct reference exists" should {
      "return OK" in {
        stubAuth(OK, successfulAuthResponse())
        stubGetCtReference(testCompanyNumber)(OK, ctReferenceBody(testCtReference))

        val res = await(post("/subscription-request/ct-reference-check")(Json.obj("companyNumber" -> testCompanyNumber)))

        res should have(
          httpStatus(OK)
        )
      }
    }

    "the ct reference does not exist" should {
      "return NOT_FOUND" in {
        stubAuth(OK, successfulAuthResponse())
        stubGetCtReference(testCompanyNumber)(status = NOT_FOUND)

        val res = await(post("/subscription-request/ct-reference-check")(Json.obj("companyNumber" -> testCompanyNumber)))

        res should have(
          httpStatus(NOT_FOUND)
        )
      }
    }

    "DES returned a failure" should {
      "throw internal server error" in {
        stubAuth(OK, successfulAuthResponse())
        stubGetCtReference(testCompanyNumber)(status = BAD_REQUEST)

        val res = await(post("/subscription-request/ct-reference-check")(Json.obj("companyNumber" -> testCompanyNumber)))

        res should have(
          httpStatus(INTERNAL_SERVER_ERROR)
        )
      }
    }
  }
}
