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

import play.api.http.Status._
import play.api.libs.json.Json
import uk.gov.hmrc.vatsignup.helpers.IntegrationTestConstants._
import uk.gov.hmrc.vatsignup.helpers._
import uk.gov.hmrc.vatsignup.helpers.servicemocks.AuthStub._
import uk.gov.hmrc.vatsignup.models._

import scala.concurrent.ExecutionContext.Implicits.global

class StoreOverseasControllerISpec extends ComponentSpecBase with CustomMatchers with TestSubmissionRequestRepository {

  "POST /subscription-request/vat-number/:vatNumber/overseas" should {
    "return NO_CONTENT" in {
      stubAuth(OK, successfulAuthResponse())

      await(submissionRequestRepo.upsertVatNumber(testVatNumber, isMigratable = true, isDirectDebit = false))

      val res = post(s"/subscription-request/vat-number/$testVatNumber/overseas")(Json.obj())

      res should have(
        httpStatus(NO_CONTENT),
        emptyBody
      )

      val dbRequest = await(submissionRequestRepo.findById(testVatNumber)).get
      dbRequest.businessEntity shouldBe Some(Overseas)
    }
  }
}
