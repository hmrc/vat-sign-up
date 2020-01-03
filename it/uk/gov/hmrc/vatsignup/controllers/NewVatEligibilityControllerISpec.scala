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

import java.time.LocalDate

import play.api.http.Status._
import play.api.libs.json.Json
import uk.gov.hmrc.vatsignup.controllers.NewVatEligibillityController._
import uk.gov.hmrc.vatsignup.helpers.IntegrationTestConstants._
import uk.gov.hmrc.vatsignup.helpers.servicemocks.AuthStub.{stubAuth, successfulAuthResponse}
import uk.gov.hmrc.vatsignup.helpers.servicemocks.GetMandationStatusStub.{stubGetMandationStatus, _}
import uk.gov.hmrc.vatsignup.helpers.servicemocks.KnownFactsAndControlListInformationStub._
import uk.gov.hmrc.vatsignup.helpers.servicemocks.KnownFactsStub._
import uk.gov.hmrc.vatsignup.helpers.{ComponentSpecBase, CustomMatchers, TestSubmissionRequestRepository}
import uk.gov.hmrc.vatsignup.models._
import uk.gov.hmrc.vatsignup.utils.CurrentDateProvider

class NewVatEligibilityControllerISpec extends ComponentSpecBase with CustomMatchers with TestSubmissionRequestRepository {
  val testDate: LocalDate = LocalDate.of(2018, 10, 18)

  override lazy val currentDateProvider: CurrentDateProvider = new CurrentDateProvider() {
    override def getCurrentDate(): LocalDate = testDate
  }

  "/subscription-request/vat-number/:vatNumber/new-mtdfb-eligibility" should {
    "return OK" when {
      "called with a mtdState of AlreadySubscribed" in {
        stubAuth(OK, successfulAuthResponse())
        stubGetMandationStatus(testVatNumber)(OK, mandationStatusBody(MTDfBMandated))

        val res = await(get(s"/subscription-request/vat-number/$testVatNumber/new-mtdfb-eligibility"))

        res should have(
          httpStatus(OK),
          jsonBodyAs(Json.obj(MtdStatusKey -> AlreadySubscribedValue))
        )
      }
    }
    "return OK with a JSON body" when {
      "called with a mtdState of Eligible" in {
        stubAuth(OK, successfulAuthResponse())
        stubGetMandationStatus(testVatNumber)(OK, mandationStatusBody(NonMTDfB))
        stubEligibleControlListInformation(testVatNumber)
        stubSuccessGetKnownFacts(testVatNumber)

        val res = await(get(s"/subscription-request/vat-number/$testVatNumber/new-mtdfb-eligibility"))

        res should have(
          httpStatus(OK),
          jsonBodyAs(Json.obj(MtdStatusKey -> EligibleValue,
            EligiblityDetailsKey -> Json.obj(IsMigratedKey -> true, IsOverseasKey -> false)))
        )
      }
    }

    "return OK with a JSON body" when {
      "called with a mtdState of Eligible and the user is overseas" in {
        stubAuth(OK, successfulAuthResponse())
        stubGetMandationStatus(testVatNumber)(OK, mandationStatusBody(NonMTDfB))
        stubEligibleControlListInformation(testVatNumber)
        stubSuccessGetKnownFactsOverseas(testVatNumber)

        val res = await(get(s"/subscription-request/vat-number/$testVatNumber/new-mtdfb-eligibility"))

        res should have(
          httpStatus(OK),
          jsonBodyAs(Json.obj(MtdStatusKey -> EligibleValue,
            EligiblityDetailsKey -> Json.obj(IsMigratedKey -> true, IsOverseasKey -> true)))
        )
      }
    }

    "return OK" when {
      "called with a mtdState of Ineligible" in {
        stubAuth(OK, successfulAuthResponse())
        stubIneligibleControlListInformation(testVatNumber)

        val res = await(get(s"/subscription-request/vat-number/$testVatNumber/new-mtdfb-eligibility"))

        res should have(
          httpStatus(OK),
          jsonBodyAs(Json.obj(MtdStatusKey -> IneligibleValue))
        )
      }
    }
    "return OK with a JSON body" when {
      "called with an mtdState of Inhibited" in {
        stubAuth(OK, successfulAuthResponse())
        stubGetMandationStatus(testVatNumber)(NOT_FOUND, Json.obj())
        stubDirectDebitControlListInformation(testVatNumber)

        val res = await(get(s"/subscription-request/vat-number/$testVatNumber/new-mtdfb-eligibility"))

        res should have(
          httpStatus(OK),
          jsonBodyAs(Json.obj(MtdStatusKey -> InhibitedValue, MigratableDatesKey -> Json.toJson(testMigratableDates)))
        )
      }
    }
    "return OK with a JSON body" when {
      "called with an mtdState of MigrationInProgress" in {
        stubAuth(OK, successfulAuthResponse())
        stubGetMandationStatus(testVatNumber)(PRECONDITION_FAILED, Json.obj())

        val res = await(get(s"/subscription-request/vat-number/$testVatNumber/new-mtdfb-eligibility"))

        res should have(
          httpStatus(OK),
          jsonBodyAs(Json.obj(MtdStatusKey -> MigrationInProgressValue))
        )
      }
    }
    "return OK with a JSON body" when {
      "called with an mtdState of Deregistered" in {
        stubAuth(OK, successfulAuthResponse())
        stubGetMandationStatus(testVatNumber)(OK, mandationStatusBody(NonMTDfB))
        stubDeregisteredVatNumber(testVatNumber)

        val res = await(get(s"/subscription-request/vat-number/$testVatNumber/new-mtdfb-eligibility"))

        res should have(
          httpStatus(OK),
          jsonBodyAs(Json.obj(MtdStatusKey -> DeregisteredValue))
        )
      }
    }
    s"return OK with a JSON body of $DeregisteredValue" when {
      "the user is not migrated and is deregistered" in {
        stubAuth(OK, successfulAuthResponse())
        stubGetMandationStatus(testVatNumber)(NOT_FOUND, Json.obj())
        stubDeregisteredControlListInformation(testVatNumber)

        val res = await(get(s"/subscription-request/vat-number/$testVatNumber/new-mtdfb-eligibility"))

        res should have(
          httpStatus(OK),
          jsonBodyAs(Json.obj(MtdStatusKey -> DeregisteredValue))
        )
      }
    }
    "return NOT_FOUND" when {
      "called with an mtdState of VatNumberNotFound" in {
        stubAuth(OK, successfulAuthResponse())
        stubGetMandationStatus(testVatNumber)(NOT_FOUND, Json.obj())
        stubFailureControlListVatNumberNotFound(testVatNumber)

        val res = await(get(s"/subscription-request/vat-number/$testVatNumber/new-mtdfb-eligibility"))

        res should have(
          httpStatus(NOT_FOUND)
        )
      }
    }
    "return Internal Server Error" when {
      "called unable to retrieve the mandation status" in {
        stubAuth(OK, successfulAuthResponse())
        stubGetMandationStatus(testVatNumber)(INTERNAL_SERVER_ERROR, Json.obj())

        val res = await(get(s"/subscription-request/vat-number/$testVatNumber/new-mtdfb-eligibility"))

        res should have(
          httpStatus(INTERNAL_SERVER_ERROR)
        )
      }
    }
  }


}
