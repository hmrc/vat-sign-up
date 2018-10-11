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

import play.api.http.Status._
import play.api.libs.json.Json
import uk.gov.hmrc.vatsignup.helpers.IntegrationTestConstants.{testVatNumber, _}
import uk.gov.hmrc.vatsignup.helpers.servicemocks.AgentClientRelationshipsStub._
import uk.gov.hmrc.vatsignup.helpers.servicemocks.AuthStub.{stubAuth, successfulAuthResponse, _}
import uk.gov.hmrc.vatsignup.helpers.servicemocks.GetMandationStatusStub.{stubGetMandationStatus, _}
import uk.gov.hmrc.vatsignup.helpers.servicemocks.KnownFactsAndControlListInformationStub._
import uk.gov.hmrc.vatsignup.helpers.{ComponentSpecBase, CustomMatchers, TestSubmissionRequestRepository}
import uk.gov.hmrc.vatsignup.models.{MigratableDates, NonMTDfB}
import uk.gov.hmrc.vatsignup.utils.CurrentDateProvider
import MigratableDates._

class StoreVatNumberControllerDirectDebitsISpec extends ComponentSpecBase with CustomMatchers with TestSubmissionRequestRepository {
  val testDate: LocalDate = LocalDate.of(2018, 10, 18)

  override lazy val currentDateProvider:CurrentDateProvider = new CurrentDateProvider() {
    override def getCurrentDate(): LocalDate = testDate
  }

  "PUT /subscription-request/vat-number" when {
    "the user pays by direct debits, and is attempting to sign up in a restricted period" should {
      "return UNPROCESSABLE_ENTITY with dates included" in {
        stubAuth(OK, successfulAuthResponse(agentEnrolment))
        stubCheckAgentClientRelationship(testAgentNumber, testVatNumber)(OK, Json.obj())
        stubGetMandationStatus(testVatNumber)(OK, mandationStatusBody(NonMTDfB))
        stubDirectDebitControlListInformation(testVatNumber)

        val res = post("/subscription-request/vat-number")(Json.obj("vatNumber" -> testVatNumber))

        val nonRestrictedPeriodStart = LocalDate.of(2018, 11, 14) //1 day after restricted period end date
        val nonRestrictedPeriodEnd = LocalDate.of(2019, 1, 17) //1 day before start of next restricted period

        res should have(
          httpStatus(UNPROCESSABLE_ENTITY),
          jsonBodyAs(MigratableDates(Some(nonRestrictedPeriodStart), Some(nonRestrictedPeriodEnd)))
        )
      }
    }
  }
}
