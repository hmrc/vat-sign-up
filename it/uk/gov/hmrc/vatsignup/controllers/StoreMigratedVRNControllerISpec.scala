
package uk.gov.hmrc.vatsignup.controllers

import play.api.http.Status._
import play.api.libs.json.Json
import uk.gov.hmrc.vatsignup.config.Constants
import uk.gov.hmrc.vatsignup.controllers.StoreMigratedVRNController._
import uk.gov.hmrc.vatsignup.helpers.IntegrationTestConstants._
import uk.gov.hmrc.vatsignup.helpers.servicemocks.AgentClientRelationshipsStub._
import uk.gov.hmrc.vatsignup.helpers.servicemocks.AuthStub._
import uk.gov.hmrc.vatsignup.helpers.servicemocks.KnownFactsStub._
import uk.gov.hmrc.vatsignup.helpers.{ComponentSpecBase, CustomMatchers, TestSubmissionRequestRepository}

class StoreMigratedVRNControllerISpec extends ComponentSpecBase with CustomMatchers with TestSubmissionRequestRepository {

  "POST /subscription-request/migrated-vat-number" should {
    "return OK" when {
      "no known facts are provided" when {
        "a vat number has been stored" in {
          stubAuth(OK, successfulAuthResponse(vatDecEnrolment()))

          val res = post("/subscription-request/migrated-vat-number")(Json.obj("vatNumber" -> testVatNumber))
          res should have(httpStatus(OK))
        }
      }

      "known facts are provided" when {
        "a vat number has been stored" in {
          stubAuth(OK, successfulAuthResponse())
          stubSuccessGetKnownFacts(testVatNumber)

          val res = post("/subscription-request/migrated-vat-number")(Json.obj(
            "vatNumber" -> testVatNumber,
            "registrationDate" -> testDateOfRegistration,
            "postCode" -> testPostCode
          ))
          res should have(httpStatus(OK))
        }
      }

      "agent enrolment is provided" when {
        "a vat number with a matching agent client relationship has been stored" in {
          stubAuth(OK, successfulAuthResponse(agentEnrolment))
          stubCheckAgentClientRelationship(testAgentNumber, testVatNumber, testLegacyRelationship)(NOT_FOUND, Json.obj("code" -> NoRelationshipCode))
          stubCheckAgentClientRelationship(testAgentNumber, testVatNumber, testMtdVatRelationship)(OK, Json.obj())

          val res = post("/subscription-request/migrated-vat-number")(Json.obj("vatNumber" -> testVatNumber)
          )

          res should have(httpStatus(OK))
        }
      }
    }


    "POST /subscription-request/migrated-vat-number" should {
      "return forbidden" when {
        "no known facts are provided" when {
          "the user has no enrolment" in {
            stubAuth(OK, successfulAuthResponse())

            val res = post("/subscription-request/migrated-vat-number")(Json.obj("vatNumber" -> testVatNumber))

            res should have(
              httpStatus(FORBIDDEN),
              jsonBodyAs(Json.obj(Constants.HttpCodeKey -> VatEnrolmentMissingCode))
            )
          }

          "both enrolments match and provided VAT number does not match" in {
            stubAuth(OK, successfulAuthResponse(vatMtdEnrolment("222222227"), vatDecEnrolment("222222227")))

            val res = post("/subscription-request/migrated-vat-number")(Json.obj("vatNumber" -> "222222229"))

            res should have(
              httpStatus(FORBIDDEN),
              jsonBodyAs(Json.obj(Constants.HttpCodeKey -> VatNumberMismatchCode))
            )
          }

          "both enrolments do not match" in {
            stubAuth(OK, successfulAuthResponse(vatMtdEnrolment("222222227"), vatDecEnrolment("222222228")))

            val res = post("/subscription-request/migrated-vat-number")(Json.obj("vatNumber" -> testVatNumber))
            res should have(httpStatus(FORBIDDEN))


            res should have(
              httpStatus(FORBIDDEN),
              jsonBodyAs(Json.obj(Constants.HttpCodeKey -> VatNumberMismatchCode))
            )
          }
        }

        "known facts are provided" when {
          "known facts do not match" in {
            stubAuth(OK, successfulAuthResponse(vatDecEnrolment()))
            stubSuccessGetKnownFacts(testVatNumber)

            val res = post("/subscription-request/migrated-vat-number")(Json.obj(
              "vatNumber" -> testVatNumber,
              "registrationDate" -> testDateOfRegistration,
              "postCode" -> "A11 11A")
            )

            res should have(
              httpStatus(FORBIDDEN),
              jsonBodyAs(Json.obj(Constants.HttpCodeKey -> KnownFactsMismatchCode))
            )
          }
        }

        "agent enrolement is provided" when {
          "agent client relationship is not found" in {
            stubAuth(OK, successfulAuthResponse(agentEnrolment))
            stubCheckAgentClientRelationship(testAgentNumber, testVatNumber, testLegacyRelationship)(NOT_FOUND, Json.obj("code" -> NoRelationshipCode))
            stubCheckAgentClientRelationship(testAgentNumber, testVatNumber, testMtdVatRelationship)(NOT_FOUND, Json.obj("code" -> NoRelationshipCode))

            val res = post("/subscription-request/migrated-vat-number")(Json.obj("vatNumber" -> testVatNumber))

            res should have(
              httpStatus(FORBIDDEN),
              jsonBodyAs(Json.obj(Constants.HttpCodeKey -> NoRelationshipCode))
            )
          }
        }
      }
    }
    "POST /subscription-request/migrated-vat-number" should {
      "return bad gateway" when {
        "agent client relationship check fails" in {
          stubAuth(OK, successfulAuthResponse(agentEnrolment))
          stubCheckAgentClientRelationship(testAgentNumber, testVatNumber, testLegacyRelationship)(NOT_FOUND, Json.obj("code" -> NoRelationshipCode))
          stubCheckAgentClientRelationship(testAgentNumber, testVatNumber, testMtdVatRelationship)(REQUEST_TIMEOUT, Json.obj())

          val res = post("/subscription-request/migrated-vat-number")(Json.obj("vatNumber" -> testVatNumber))

          res should have(
            httpStatus(INTERNAL_SERVER_ERROR),
            jsonBodyAs(Json.obj(Constants.HttpCodeKey -> RelationshipCheckFailure))
          )
        }
      }
    }
  }
}
