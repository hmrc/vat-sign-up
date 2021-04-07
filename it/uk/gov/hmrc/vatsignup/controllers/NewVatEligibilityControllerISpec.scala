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

import play.api.libs.json.Json
import play.api.test.Helpers._
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
    "return OK with an AlreadySubscribed status" when {
      "the user is MTDfBMandated" in {
        stubAuth(OK, successfulAuthResponse())
        stubGetMandationStatus(testVatNumber)(OK, mandationStatusBody(MTDfBMandated))
        stubSuccessGetKnownFacts(testVatNumber)

        val res = get(s"/subscription-request/vat-number/$testVatNumber/new-mtdfb-eligibility")

        res should have(
          httpStatus(OK),
          jsonBodyAs(Json.obj(
            MtdStatusKey -> AlreadySubscribedValue,
            IsOverseasKey -> false
          ))
        )
      }

      "the user is MTDfB" in {
        stubAuth(OK, successfulAuthResponse())
        stubGetMandationStatus(testVatNumber)(OK, mandationStatusBody(MTDfB))
        stubSuccessGetKnownFacts(testVatNumber)

        val res = get(s"/subscription-request/vat-number/$testVatNumber/new-mtdfb-eligibility")

        res should have(
          httpStatus(OK),
          jsonBodyAs(Json.obj(
            MtdStatusKey -> AlreadySubscribedValue,
            IsOverseasKey -> false
          ))
        )
      }

      "the user is overseas and MTDfB" in {
        stubAuth(OK, successfulAuthResponse())
        stubGetMandationStatus(testVatNumber)(OK, mandationStatusBody(MTDfB))
        stubSuccessGetKnownFactsOverseas(testVatNumber)

        val res = get(s"/subscription-request/vat-number/$testVatNumber/new-mtdfb-eligibility")

        res should have(
          httpStatus(OK),
          jsonBodyAs(Json.obj(
            MtdStatusKey -> AlreadySubscribedValue,
            IsOverseasKey -> true
          ))
        )
      }
    }

    "return OK with an Eligible status" when {
      "the user is Non-MTDfB" in {
        stubAuth(OK, successfulAuthResponse())
        stubGetMandationStatus(testVatNumber)(OK, mandationStatusBody(NonMTDfB))
        stubSuccessGetKnownFacts(testVatNumber)

        val res = get(s"/subscription-request/vat-number/$testVatNumber/new-mtdfb-eligibility")

        res should have(
          httpStatus(OK),
          jsonBodyAs(Json.obj(MtdStatusKey -> EligibleValue,
            EligiblityDetailsKey -> Json.obj(IsMigratedKey -> true, IsOverseasKey -> false, IsNewKey -> false)))
        )
      }

      "the user is Non-MTDfB and newly registered" in {
        stubAuth(OK, successfulAuthResponse())
        stubGetMandationStatus(testVatNumber)(OK, mandationStatusBody(NonMTDfB))
        stubSuccessGetKnownFactsRecentDate(testVatNumber)

        val res = get(s"/subscription-request/vat-number/$testVatNumber/new-mtdfb-eligibility")

        res should have(
          httpStatus(OK),
          jsonBodyAs(Json.obj(MtdStatusKey -> EligibleValue,
            EligiblityDetailsKey -> Json.obj(IsMigratedKey -> true, IsOverseasKey -> false, IsNewKey -> true)))
        )
      }

      "the user is MTDfBExempt" in {
        stubAuth(OK, successfulAuthResponse())
        stubGetMandationStatus(testVatNumber)(OK, mandationStatusBody(MTDfBExempt))
        stubSuccessGetKnownFacts(testVatNumber)

        val res = get(s"/subscription-request/vat-number/$testVatNumber/new-mtdfb-eligibility")

        res should have(
          httpStatus(OK),
          jsonBodyAs(Json.obj(MtdStatusKey -> EligibleValue,
            EligiblityDetailsKey -> Json.obj(IsMigratedKey -> true, IsOverseasKey -> false, IsNewKey -> false)))
        )
      }
    }

    "return OK with an Eligible status" when {
      "the user is overseas and Non-MTDfB" in {
        stubAuth(OK, successfulAuthResponse())
        stubGetMandationStatus(testVatNumber)(OK, mandationStatusBody(NonMTDfB))
        stubSuccessGetKnownFactsOverseas(testVatNumber)

        val res = get(s"/subscription-request/vat-number/$testVatNumber/new-mtdfb-eligibility")

        res should have(
          httpStatus(OK),
          jsonBodyAs(Json.obj(MtdStatusKey -> EligibleValue,
            EligiblityDetailsKey -> Json.obj(IsMigratedKey -> true, IsOverseasKey -> true, IsNewKey -> false)))
        )
      }

      "the user is overseas and MTDfBExempt" in {
        stubAuth(OK, successfulAuthResponse())
        stubGetMandationStatus(testVatNumber)(OK, mandationStatusBody(MTDfBExempt))
        stubSuccessGetKnownFactsOverseas(testVatNumber)

        val res = get(s"/subscription-request/vat-number/$testVatNumber/new-mtdfb-eligibility")

        res should have(
          httpStatus(OK),
          jsonBodyAs(Json.obj(MtdStatusKey -> EligibleValue,
            EligiblityDetailsKey -> Json.obj(IsMigratedKey -> true, IsOverseasKey -> true, IsNewKey -> false)))
        )
      }
    }

    "return OK with an Ineligible status" when {
      "the user is not found" in {
        stubAuth(OK, successfulAuthResponse())
        stubGetMandationStatus(testVatNumber)(NOT_FOUND, Json.obj())
        stubIneligibleControlListInformation(testVatNumber)

        val res = get(s"/subscription-request/vat-number/$testVatNumber/new-mtdfb-eligibility")

        res should have(
          httpStatus(OK),
          jsonBodyAs(Json.obj(MtdStatusKey -> IneligibleValue))
        )
      }
    }

    "return OK with an Inhibited status" when {
      "the user is within the filing dates inhibition period" in {
        stubAuth(OK, successfulAuthResponse())
        stubGetMandationStatus(testVatNumber)(NOT_FOUND, Json.obj())
        stubDirectDebitControlListInformation(testVatNumber)

        val res = get(s"/subscription-request/vat-number/$testVatNumber/new-mtdfb-eligibility")

        res should have(
          httpStatus(OK),
          jsonBodyAs(Json.obj(MtdStatusKey -> InhibitedValue, MigratableDatesKey -> Json.toJson(testMigratableDates)))
        )
      }
    }

    "return OK with a MigrationInProgress status" when {
      "the user is already migrating their data" in {
        stubAuth(OK, successfulAuthResponse())
        stubGetMandationStatus(testVatNumber)(PRECONDITION_FAILED, Json.obj())

        val res = get(s"/subscription-request/vat-number/$testVatNumber/new-mtdfb-eligibility")

        res should have(
          httpStatus(OK),
          jsonBodyAs(Json.obj(MtdStatusKey -> MigrationInProgressValue))
        )
      }
    }

    "return OK with a Deregistered status" when {
      "the user is no longer registered for MTD" in {
        stubAuth(OK, successfulAuthResponse())
        stubGetMandationStatus(testVatNumber)(OK, mandationStatusBody(NonMTDfB))
        stubDeregisteredVatNumber(testVatNumber)

        val res = get(s"/subscription-request/vat-number/$testVatNumber/new-mtdfb-eligibility")

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

        val res = get(s"/subscription-request/vat-number/$testVatNumber/new-mtdfb-eligibility")

        res should have(
          httpStatus(OK),
          jsonBodyAs(Json.obj(MtdStatusKey -> DeregisteredValue))
        )
      }
    }

    "return NOT_FOUND" when {
      "the vat number is not found" in {
        stubAuth(OK, successfulAuthResponse())
        stubGetMandationStatus(testVatNumber)(NOT_FOUND, Json.obj())
        stubFailureControlListVatNumberNotFound(testVatNumber)

        val res = get(s"/subscription-request/vat-number/$testVatNumber/new-mtdfb-eligibility")

        res should have(
          httpStatus(NOT_FOUND)
        )
      }
    }

    "return Internal Server Error" when {
      "unable to retrieve the mandation status" in {
        stubAuth(OK, successfulAuthResponse())
        stubGetMandationStatus(testVatNumber)(INTERNAL_SERVER_ERROR, Json.obj())

        val res = get(s"/subscription-request/vat-number/$testVatNumber/new-mtdfb-eligibility")

        res should have(
          httpStatus(INTERNAL_SERVER_ERROR)
        )
      }
    }
  }

}
