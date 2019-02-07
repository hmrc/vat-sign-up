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

import java.util.UUID

import play.api.http.Status._
import play.api.libs.json.Json
import uk.gov.hmrc.vatsignup.config.Constants
import uk.gov.hmrc.vatsignup.config.Constants.ControlList.OverseasKey
import uk.gov.hmrc.vatsignup.helpers.IntegrationTestConstants._
import uk.gov.hmrc.vatsignup.helpers._
import uk.gov.hmrc.vatsignup.helpers.servicemocks.AgentClientRelationshipsStub._
import uk.gov.hmrc.vatsignup.helpers.servicemocks.AuthStub._
import uk.gov.hmrc.vatsignup.helpers.servicemocks.GetMandationStatusStub._
import uk.gov.hmrc.vatsignup.helpers.servicemocks.KnownFactsAndControlListInformationStub._
import uk.gov.hmrc.vatsignup.helpers.servicemocks.KnownFactsStub.stubSuccessGetKnownFacts
import uk.gov.hmrc.vatsignup.httpparsers.AgentClientRelationshipsHttpParser.NoRelationshipCode
import uk.gov.hmrc.vatsignup.models.{MTDfBVoluntary, NonMTDfB}

class StoreVatNumberControllerISpec extends ComponentSpecBase with CustomMatchers with TestSubmissionRequestRepository {

  "PUT /subscription-request/vat-number" when {
    "the user is an agent" should {
      "return OK when the vat number has been stored successfully" in {
        stubAuth(OK, successfulAuthResponse(agentEnrolment))
        stubCheckAgentClientRelationship(testAgentNumber, testVatNumber)(OK, Json.obj())
        stubGetMandationStatus(testVatNumber)(OK, mandationStatusBody(NonMTDfB))
        stubSuccessGetKnownFactsAndControlListInformation(testVatNumber)

        val res = post("/subscription-request/vat-number")(Json.obj("vatNumber" -> testVatNumber))

        res should have(
          httpStatus(OK),
          jsonBodyAs(Json.obj(
            OverseasKey -> false
          ))
        )
      }

      "return CONFLICT when the client is already subscribed" in {
        stubAuth(OK, successfulAuthResponse(agentEnrolment))
        stubCheckAgentClientRelationship(testAgentNumber, testVatNumber)(OK, Json.obj())
        stubGetMandationStatus(testVatNumber)(OK, mandationStatusBody(MTDfBVoluntary))

        val res = post("/subscription-request/vat-number")(Json.obj("vatNumber" -> testVatNumber))

        res should have(
          httpStatus(CONFLICT),
          emptyBody
        )
      }

      "return BAD REQUEST when the client's VAT migration is in progress" in {
        stubAuth(OK, successfulAuthResponse(agentEnrolment))
        stubCheckAgentClientRelationship(testAgentNumber, testVatNumber)(OK, Json.obj())
        stubGetMandationStatus(testVatNumber)(PRECONDITION_FAILED)

        val res = post("/subscription-request/vat-number")(Json.obj("vatNumber" -> testVatNumber))

        res should have(
          httpStatus(BAD_REQUEST),
          jsonBodyAs(Json.obj(Constants.HttpCodeKey -> "VatMigrationInProgress"))
        )
      }

      "return FORBIDDEN when there is no relationship" in {
        stubAuth(OK, successfulAuthResponse(agentEnrolment))
        stubCheckAgentClientRelationship(testAgentNumber, testVatNumber)(NOT_FOUND, Json.obj("code" -> NoRelationshipCode))

        val res = post("/subscription-request/vat-number")(Json.obj("vatNumber" -> testVatNumber))

        res should have(
          httpStatus(FORBIDDEN),
          jsonBodyAs(Json.obj(Constants.HttpCodeKey -> NoRelationshipCode))
        )
      }

      "return BAD_REQUEST when the json is invalid" in {
        stubAuth(OK, successfulAuthResponse())

        val res = post("/subscription-request/vat-number")(Json.obj())

        res should have(
          httpStatus(BAD_REQUEST)
        )
      }
    }

    "the user is a principal user" when {
      "the user has a HMCE-VAT enrolment" should {
        "return OK when the vat number has been stored successfully" in {
          stubAuth(OK, successfulAuthResponse(vatDecEnrolment))
          stubGetMandationStatus(testVatNumber)(OK, mandationStatusBody(NonMTDfB))
          stubSuccessGetKnownFactsAndControlListInformation(testVatNumber)

          val res = post("/subscription-request/vat-number")(Json.obj("vatNumber" -> testVatNumber))

          res should have(
            httpStatus(OK),
            jsonBodyAs(Json.obj(
              OverseasKey -> false
            ))
          )
        }

        "claim the enrolment when the user is already subscribed" in {
          stubAuth(OK, successfulAuthResponse(vatDecEnrolment))
          stubCheckAgentClientRelationship(testAgentNumber, testVatNumber)(OK, Json.obj())
          stubGetMandationStatus(testVatNumber)(OK, mandationStatusBody(MTDfBVoluntary))
          stubSuccessGetKnownFacts(testVatNumber)

          val res = post("/subscription-request/vat-number")(Json.obj("vatNumber" -> testVatNumber))

          res should have(
            httpStatus(CONFLICT)
          )
        }

        "return FORBIDDEN when vat number does not match the one in enrolment" in {
          stubAuth(OK, successfulAuthResponse(vatDecEnrolment))

          val res = post("/subscription-request/vat-number")(Json.obj("vatNumber" -> UUID.randomUUID().toString))

          res should have(
            httpStatus(FORBIDDEN),
            jsonBodyAs(Json.obj(Constants.HttpCodeKey -> "DoesNotMatchEnrolment"))
          )
        }

        "return BAD REQUEST when the user's VAT migration is in progress" in {
          stubAuth(OK, successfulAuthResponse(vatDecEnrolment))
          stubGetMandationStatus(testVatNumber)(PRECONDITION_FAILED)

          val res = post("/subscription-request/vat-number")(Json.obj("vatNumber" -> testVatNumber))

          res should have(
            httpStatus(BAD_REQUEST),
            jsonBodyAs(Json.obj(Constants.HttpCodeKey -> "VatMigrationInProgress"))
          )
        }

        "return BAD_REQUEST when the json is invalid" in {
          stubAuth(OK, successfulAuthResponse())

          val res = post("/subscription-request/vat-number")(Json.obj())

          res should have(
            httpStatus(BAD_REQUEST)
          )
        }
      }
    }

    "does not have a HMCE-VAT enrolment but has provided known facts" should {
      "return OK when the vat number has been stored successfully" in {
        stubAuth(OK, successfulAuthResponse())
        stubGetMandationStatus(testVatNumber)(OK, mandationStatusBody(NonMTDfB))
        stubSuccessGetKnownFactsAndControlListInformation(testVatNumber)

        val res = post("/subscription-request/vat-number")(Json.obj(
          "vatNumber" -> testVatNumber,
          "postCode" -> testPostCode,
          "registrationDate" -> testDateOfRegistration
        ))

        res should have(
          httpStatus(OK),
          jsonBodyAs(Json.obj(
            OverseasKey -> false
          ))
        )
      }

      "return FORBIDDEN when known facts mismatch" in {
        stubAuth(OK, successfulAuthResponse())
        stubGetMandationStatus(testVatNumber)(OK, mandationStatusBody(NonMTDfB))
        stubSuccessGetKnownFactsAndControlListInformation(testVatNumber)

        val res = post("/subscription-request/vat-number")(Json.obj(
          "vatNumber" -> testVatNumber,
          "postCode" -> testVatNumber,
          "registrationDate" -> testVatNumber
        ))

        res should have(
          httpStatus(FORBIDDEN),
          jsonBodyAs(Json.obj(Constants.HttpCodeKey -> "KNOWN_FACTS_MISMATCH"))
        )
      }

      "return BAD REQUEST when the user's VAT migration is in progress" in {
        stubAuth(OK, successfulAuthResponse())
        stubGetMandationStatus(testVatNumber)(PRECONDITION_FAILED)

        val res = post("/subscription-request/vat-number")(Json.obj(
          "vatNumber" -> testVatNumber,
          "postCode" -> testVatNumber,
          "registrationDate" -> testVatNumber
        ))

        res should have(
          httpStatus(BAD_REQUEST),
          jsonBodyAs(Json.obj(Constants.HttpCodeKey -> "VatMigrationInProgress"))
        )
      }

      "return PRECONDITION_FAILED when vat number is not found" in {
        stubAuth(OK, successfulAuthResponse())
        stubGetMandationStatus(testVatNumber)(OK, mandationStatusBody(NonMTDfB))
        stubFailureControlListVatNumberNotFound(testVatNumber)

        val res = post("/subscription-request/vat-number")(Json.obj(
          "vatNumber" -> testVatNumber,
          "postCode" -> testVatNumber,
          "registrationDate" -> testVatNumber
        ))

        res should have(
          httpStatus(PRECONDITION_FAILED)
        )
      }

      "return PRECONDITION_FAILED when vat number is invalid" in {
        stubAuth(OK, successfulAuthResponse())
        stubGetMandationStatus(testVatNumber)(OK, mandationStatusBody(NonMTDfB))
        stubFailureKnownFactsInvalidVatNumber(testVatNumber)

        val res = post("/subscription-request/vat-number")(Json.obj(
          "vatNumber" -> testVatNumber,
          "postCode" -> testVatNumber,
          "registrationDate" -> testVatNumber
        ))

        res should have(
          httpStatus(PRECONDITION_FAILED)
        )
      }
    }

    "does not have a HMCE-VAT enrolment and have not provided known facts" should {
      "return FORBIDDEN" in {
        stubAuth(OK, successfulAuthResponse())
        stubGetMandationStatus(testVatNumber)(OK, mandationStatusBody(NonMTDfB))

        val res = post("/subscription-request/vat-number")(Json.obj("vatNumber" -> testVatNumber))

        res should have(
          httpStatus(FORBIDDEN),
          jsonBodyAs(Json.obj(Constants.HttpCodeKey -> "InsufficientEnrolments"))
        )
      }
    }
  }

  "the user does not have VAT or Agent enrolments" should {
    "return FORBIDDEN" in {
      stubAuth(OK, successfulAuthResponse())

      val res = post("/subscription-request/vat-number")(Json.obj("vatNumber" -> testVatNumber))

      res should have(
        httpStatus(FORBIDDEN),
        jsonBodyAs(Json.obj(Constants.HttpCodeKey -> "InsufficientEnrolments"))
      )
    }
  }

  "storing an overseas trader" should {
    "return OK with overseas flag included" in {
      stubAuth(OK, successfulAuthResponse(agentEnrolment))
      stubCheckAgentClientRelationship(testAgentNumber, testVatNumber)(OK, Json.obj())
      stubGetMandationStatus(testVatNumber)(OK, mandationStatusBody(NonMTDfB))
      stubOverseasControlListInformation(testVatNumber)

      val res = post("/subscription-request/vat-number")(Json.obj("vatNumber" -> testVatNumber))

      res should have(
        httpStatus(OK),
        jsonBodyAs(Json.obj(
          OverseasKey -> true
        ))
      )
    }
  }

}
