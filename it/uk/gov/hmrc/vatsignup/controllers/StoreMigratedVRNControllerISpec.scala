
package uk.gov.hmrc.vatsignup.controllers

import play.api.http.Status._
import play.api.libs.json.Json
import uk.gov.hmrc.vatsignup.helpers.IntegrationTestConstants._
import uk.gov.hmrc.vatsignup.helpers.servicemocks.AuthStub.{stubAuth, successfulAuthResponse, vatDecEnrolment, vatMtdEnrolment}
import uk.gov.hmrc.vatsignup.helpers.servicemocks.KnownFactsAndControlListInformationStub.stubGetKnownFactsAndControlListInformation34
import uk.gov.hmrc.vatsignup.helpers.{ComponentSpecBase, CustomMatchers, TestSubmissionRequestRepository}

class StoreMigratedVRNControllerISpec extends ComponentSpecBase with CustomMatchers with TestSubmissionRequestRepository {

  "POST /subscription-request/migrated-vat-number" when {
    "no known facts are provided" when {
      "a vat number has been stored" should {
        "return okay" in {
          stubAuth(OK, successfulAuthResponse(vatDecEnrolment()))

          val res = post("/subscription-request/migrated-vat-number")(Json.obj("vatNumber" -> testVatNumber))
          res should have(httpStatus(OK))
        }
      }
    }

    "known facts are provided" when {
      "a vat number has been stored" should {
        "return okay" in {
          stubAuth(OK, successfulAuthResponse())
          stubGetKnownFactsAndControlListInformation34(testVatNumber, testBasePostCode, testDateOfRegistration)

          val res = post("/subscription-request/migrated-vat-number")(Json.obj("vatNumber" -> testVatNumber, "postCode" -> testBasePostCode, "registrationDate" -> testDateOfRegistration))
          res should have(httpStatus(OK))
        }
      }
    }
  }

  "POST /subscription-request/migrated-vat-number" when {
    "no known facts are provided" when {
      "the user has no enrolment" should {
        "return forbidden" in {
          stubAuth(OK, successfulAuthResponse())

          val res = post("/subscription-request/migrated-vat-number")(Json.obj("vatNumber" -> testVatNumber))
          res should have(httpStatus(FORBIDDEN))
        }
      }

      "both enrolments match and provided VAT number does not match" should {
        "return forbidden" in {
          stubAuth(OK, successfulAuthResponse(vatMtdEnrolment("222222227"), vatDecEnrolment("222222227")))

          val res = post("/subscription-request/migrated-vat-number")(Json.obj("vatNumber" -> "222222229"))
          res should have(httpStatus(FORBIDDEN))
        }
      }

      "both enrolments do not match" should {
        "return forbidden" in {
          stubAuth(OK, successfulAuthResponse(vatMtdEnrolment("222222227"), vatDecEnrolment("222222228")))

          val res = post("/subscription-request/migrated-vat-number")(Json.obj("vatNumber" -> "222222229"))
          res should have(httpStatus(FORBIDDEN))
        }
      }
    }

    "known facts are provided" when {
      "both known facts do not match" should {
        "return forbidden" in {
          stubAuth(OK, successfulAuthResponse())
          stubGetKnownFactsAndControlListInformation34(testVatNumber, testBasePostCode, testDateOfRegistration)

          val res = post("/subscription-request/migrated-vat-number")(Json.obj("vatNumber" -> testVatNumber, "postCode" -> "AA11AA", "registrationDate" -> "2018-01-03"))
          res should have(httpStatus(FORBIDDEN))
        }
      }

      "the provided known facts for Postcode do not match" should {
        "return forbidden" in {
          stubAuth(OK, successfulAuthResponse(vatMtdEnrolment("222222227"), vatDecEnrolment("222222227")))
          stubGetKnownFactsAndControlListInformation34(testVatNumber, testBasePostCode, testDateOfRegistration)

          val res = post("/subscription-request/migrated-vat-number")(Json.obj("vatNumber" -> testVatNumber, "postcode" -> "AA11AA", "registrationDate" -> testDateOfRegistration))
          res should have(httpStatus(FORBIDDEN))
        }
      }

      "the provided known facts for Registration Date do not match" should {
        "return forbidden" in {
          stubAuth(OK, successfulAuthResponse(vatMtdEnrolment("222222227"), vatDecEnrolment("222222228")))
          stubGetKnownFactsAndControlListInformation34(testVatNumber, testBasePostCode, testDateOfRegistration)

          val res = post("/subscription-request/migrated-vat-number")(Json.obj("vatNumber" -> testVatNumber, "postcode" -> testBasePostCode, "registrationDate" -> "2018-01-03"))
          res should have(httpStatus(FORBIDDEN))
        }
      }
    }
  }

}
