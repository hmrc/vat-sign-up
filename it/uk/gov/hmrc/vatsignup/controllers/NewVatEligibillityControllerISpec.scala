
package uk.gov.hmrc.vatsignup.controllers

import java.time.LocalDate

import play.api.http.Status._
import play.api.libs.json.Json
import uk.gov.hmrc.vatsignup.controllers.NewVatEligibillityController._
import uk.gov.hmrc.vatsignup.helpers.IntegrationTestConstants._
import uk.gov.hmrc.vatsignup.helpers.servicemocks.AuthStub.{stubAuth, successfulAuthResponse}
import uk.gov.hmrc.vatsignup.helpers.servicemocks.GetMandationStatusStub.stubGetMandationStatus
import uk.gov.hmrc.vatsignup.helpers.servicemocks.KnownFactsAndControlListInformationStub._
import uk.gov.hmrc.vatsignup.helpers.{ComponentSpecBase, CustomMatchers, TestSubmissionRequestRepository}
import uk.gov.hmrc.vatsignup.models._
import uk.gov.hmrc.vatsignup.helpers.servicemocks.GetMandationStatusStub._
import uk.gov.hmrc.vatsignup.helpers.servicemocks.KnownFactsStub
import uk.gov.hmrc.vatsignup.helpers.servicemocks.KnownFactsStub._
import uk.gov.hmrc.vatsignup.httpparsers.GetMandationStatusHttpParser
import uk.gov.hmrc.vatsignup.utils.CurrentDateProvider

class NewVatEligibillityControllerISpec extends ComponentSpecBase with CustomMatchers with TestSubmissionRequestRepository {
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
          jsonBodyAs(Json.obj(MtdStatusKey -> MigraitonInProgressValue))
        )
      }
    }
    "return Internal Server Error" when {
      "called unable to retrieve the control list" in {
        stubAuth(OK, successfulAuthResponse())
        stubFailureControlListVatNumberNotFound(testVatNumber)

        val res = await(get(s"/subscription-request/vat-number/$testVatNumber/new-mtdfb-eligibility"))

        res should have(
          httpStatus(INTERNAL_SERVER_ERROR)
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
