package uk.gov.hmrc.vatsignup.controllers

import uk.gov.hmrc.vatsignup.helpers.{ComponentSpecBase, CustomMatchers, TestSubmissionRequestRepository}
import play.api.http.Status._
import uk.gov.hmrc.vatsignup.helpers.IntegrationTestConstants.testVatNumber
import uk.gov.hmrc.vatsignup.helpers.servicemocks.AuthStub.{stubAuth, successfulAuthResponse}
import uk.gov.hmrc.vatsignup.helpers.servicemocks.KnownFactsAndControlListInformationStub.stubOverseasControlListInformation

class NewVatEligibillityControllerISpec extends ComponentSpecBase with CustomMatchers with TestSubmissionRequestRepository {

  "/subscription-request/vat-number/:vatNumber/new-mtdfb-eligibility" should {
    "return OK" when {
      "called with a valid mtdStatus" in {
        stubAuth(OK, successfulAuthResponse())
        stubOverseasControlListInformation(testVatNumber)

        val res = await(get(s"/subscription-request/vat-number/$testVatNumber/new-mtdfb-eligibility"))

        res should have(
          httpStatus(OK)
        )
      }
    }
    "return Internal Server Error" when {
      "called with an invalid mtdStatus" in {
        stubAuth(OK, successfulAuthResponse())
        stubOverseasControlListInformation(testVatNumber)

        val res = await(get("/subscription-request/vat-number/:vatNumber/new-mtdfb-eligibility"))

        res should have(
          httpStatus(INTERNAL_SERVER_ERROR)
        )
      }
    }
  }


}
