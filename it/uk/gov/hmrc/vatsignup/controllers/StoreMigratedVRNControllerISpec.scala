
package uk.gov.hmrc.vatsignup.controllers

import java.util.UUID

import play.api.http.Status._
import play.api.libs.json.Json
import uk.gov.hmrc.vatsignup.helpers.IntegrationTestConstants._
import uk.gov.hmrc.vatsignup.helpers.servicemocks.AuthStub.{stubAuth, successfulAuthResponse, vatDecEnrolment}
import uk.gov.hmrc.vatsignup.helpers.{ComponentSpecBase, CustomMatchers, TestSubmissionRequestRepository}

class StoreMigratedVRNControllerISpec extends ComponentSpecBase with CustomMatchers with TestSubmissionRequestRepository {

  "POST /subscription-request/migrated-vat-number" should {
    "return okay" when {
      "a vat number has been stored" in {
        stubAuth(OK, successfulAuthResponse(vatDecEnrolment))

        val res = post("/subscription-request/migrated-vat-number")(Json.obj("vatNumber" -> testVatNumber))

        res should have(httpStatus(OK))

      }
    }
  }

  "POST /subscription-request/migrated-vat-number" should {
    "return forbidden" when {

      "the user has no enrolment" in {
        stubAuth(OK, successfulAuthResponse())


        val res = post("/subscription-request/migrated-vat-number")(Json.obj("vatNumber" -> testVatNumber))

        res should have(httpStatus(FORBIDDEN))

      }

      "the enrolment and provided VAT number do not match" in {
        stubAuth(OK, successfulAuthResponse(vatDecEnrolment))

        val res = post("/subscription-request/migrated-vat-number")(Json.obj("vatNumber" -> UUID.randomUUID().toString))

        res should have(httpStatus(FORBIDDEN))

      }
    }
  }

}
