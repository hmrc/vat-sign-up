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

import java.util.UUID

import play.api.libs.json.Json
import play.api.test.Helpers._
import uk.gov.hmrc.vatsignup.config.Constants
import uk.gov.hmrc.vatsignup.config.Constants.ControlList._
import uk.gov.hmrc.vatsignup.helpers.IntegrationTestConstants._
import uk.gov.hmrc.vatsignup.helpers._
import uk.gov.hmrc.vatsignup.helpers.servicemocks.AgentClientRelationshipsStub._
import uk.gov.hmrc.vatsignup.helpers.servicemocks.AuthStub._
import uk.gov.hmrc.vatsignup.helpers.servicemocks.KnownFactsAndControlListInformationStub._
import uk.gov.hmrc.vatsignup.httpparsers.AgentClientRelationshipsHttpParser.NoRelationshipCode

class StoreVatNumberControllerISpec extends ComponentSpecBase with CustomMatchers with TestSubmissionRequestRepository {

  "PUT /subscription-request/vat-number" when {
    "the user is an agent" should {
      "return OK when the vat number has been stored successfully based on a legacy relationship" in {
        stubAuth(OK, successfulAuthResponse(agentEnrolment))
        stubCheckAgentClientRelationship(testAgentNumber, testVatNumber, testLegacyRelationship)(OK, Json.obj())
        stubSuccessGetKnownFactsAndControlListInformation(testVatNumber)

        val res = post("/subscription-request/vat-number")(Json.obj("vatNumber" -> testVatNumber))

        res should have(
          httpStatus(OK),
          jsonBodyAs(Json.obj(
            OverseasKey -> false,
            DirectDebitKey -> false
          ))
        )
      }

      "return FORBIDDEN when there is no relationship" in {
        stubAuth(OK, successfulAuthResponse(agentEnrolment))
        stubCheckAgentClientRelationship(testAgentNumber, testVatNumber, testLegacyRelationship)(NOT_FOUND, Json.obj("code" -> NoRelationshipCode))

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
          stubAuth(OK, successfulAuthResponse(vatDecEnrolment()))
          stubSuccessGetKnownFactsAndControlListInformation(testVatNumber)

          val res = post("/subscription-request/vat-number")(Json.obj("vatNumber" -> testVatNumber))

          res should have(
            httpStatus(OK),
            jsonBodyAs(Json.obj(
              OverseasKey -> false,
              DirectDebitKey -> false
            ))
          )
        }

        "return FORBIDDEN when vat number does not match the one in enrolment" in {
          stubAuth(OK, successfulAuthResponse(vatDecEnrolment()))

          val res = post("/subscription-request/vat-number")(Json.obj("vatNumber" -> UUID.randomUUID().toString))

          res should have(
            httpStatus(FORBIDDEN),
            jsonBodyAs(Json.obj(Constants.HttpCodeKey -> "DoesNotMatchEnrolment"))
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

    "does not have a HMCE-VAT enrolment but has provided known facts" when {
      "The vat number has been stored successfully, 4 known facts are passed" should {
        "return Ok" in {
          stubAuth(OK, successfulAuthResponse())
          stubSuccessGetKnownFactsAndControlListInformation(testVatNumber)

          val res = post("/subscription-request/vat-number")(Json.obj(
            "vatNumber" -> testVatNumber,
            "postCode" -> testPostCode,
            "registrationDate" -> testDateOfRegistration,
            "lastReturnMonthPeriod" -> testFrontendLastReturnMonthPeriod,
            "lastNetDue" -> testLastNetDue
          ))

          res should have(
            httpStatus(OK),
            jsonBodyAs(Json.obj(
              OverseasKey -> false,
              DirectDebitKey -> false
            ))
          )
        }
      }
      "The vat number has been stored successfully, is overseas" should {
        "return Ok" in {
          stubAuth(OK, successfulAuthResponse())
          stubOverseasFourKFControlListInformation(testVatNumber)

          val res = post("/subscription-request/vat-number")(Json.obj(
            "vatNumber" -> testVatNumber,
            "postCode" -> "",
            "registrationDate" -> testDateOfRegistration,
            "lastReturnMonthPeriod" -> testFrontendLastReturnMonthPeriod,
            "lastNetDue" -> testLastNetDue
          ))

          res should have(
            httpStatus(OK),
            jsonBodyAs(Json.obj(
              OverseasKey -> true,
              DirectDebitKey -> false
            ))
          )
        }
      }
      "The vat number has been stored successfully, 2 known facts are passed" should {
        "return OK" in {
          stubAuth(OK, successfulAuthResponse())
          stubSuccessNotFiled(testVatNumber)

          val res = post("/subscription-request/vat-number")(Json.obj(
            "vatNumber" -> testVatNumber,
            "postCode" -> testPostCode,
            "registrationDate" -> testDateOfRegistration
          ))

          res should have(
            httpStatus(OK),
            jsonBodyAs(Json.obj(
              OverseasKey -> false,
              DirectDebitKey -> false
            ))
          )
        }
      }
      "2 known facts are passed when the user is not overseas" should {
        "return FORBIDDEN" in {
          stubAuth(OK, successfulAuthResponse())
          stubSuccessGetKnownFactsAndControlListInformation(testVatNumber)

          val res = post("/subscription-request/vat-number")(Json.obj(
            "vatNumber" -> testVatNumber,
            "postCode" -> testPostCode,
            "registrationDate" -> testDateOfRegistration
          ))

          res should have(
            httpStatus(FORBIDDEN),
            jsonBodyAs(Json.obj(Constants.HttpCodeKey -> "KNOWN_FACTS_MISMATCH"))
          )
        }
      }
      "4 known facts are passed in" when {
        "the box 5 value is negative" should {
          "return OK when the KFs match" in {
            stubAuth(OK, successfulAuthResponse())
            stubSuccessGetKnownFactsAndControlListInformation(testVatNumber)

            val res = post("/subscription-request/vat-number")(Json.obj(
              "vatNumber" -> testVatNumber,
              "postCode" -> testPostCode,
              "registrationDate" -> testDateOfRegistration,
              "lastReturnMonthPeriod" -> testFrontendLastReturnMonthPeriod,
              "lastNetDue" -> "-10000.02"
            ))

            res should have(
              httpStatus(OK),
              jsonBodyAs(Json.obj(
                OverseasKey -> false,
                DirectDebitKey -> false
              ))
            )
          }
          "return FORBIDDEN when the KFs don't match" in {
            stubAuth(OK, successfulAuthResponse())
            stubSuccessGetKnownFactsAndControlListInformation(testVatNumber)

            val res = post("/subscription-request/vat-number")(Json.obj(
              "vatNumber" -> testVatNumber,
              "postCode" -> testPostCode,
              "registrationDate" -> testDateOfRegistration,
              "lastReturnMonthPeriod" -> testFrontendLastReturnMonthPeriod,
              "lastNetDue" -> "-12345.02"
            ))

            res should have(
              httpStatus(FORBIDDEN),
              jsonBodyAs(Json.obj(Constants.HttpCodeKey -> "KNOWN_FACTS_MISMATCH"))
            )
          }
        }
        "the box 5 value is positive" should {
          "Return OK" in {
            stubAuth(OK, successfulAuthResponse())
            stubSuccessGetKnownFactsAndControlListInformation(testVatNumber)

            val res = post("/subscription-request/vat-number")(Json.obj(
              "vatNumber" -> testVatNumber,
              "postCode" -> testPostCode,
              "registrationDate" -> testDateOfRegistration,
              "lastReturnMonthPeriod" -> testFrontendLastReturnMonthPeriod,
              "lastNetDue" -> testLastNetDue
            ))

            res should have(
              httpStatus(OK),
              jsonBodyAs(Json.obj(
                OverseasKey -> false,
                DirectDebitKey -> false
              ))
            )
          }
        }
      }

      "When known facts mismatch" should {
        "return FORBIDDEN" in {
          stubAuth(OK, successfulAuthResponse())
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
      }

      "return PRECONDITION_FAILED when vat number is not found" in {
        stubAuth(OK, successfulAuthResponse())
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
      stubCheckAgentClientRelationship(testAgentNumber, testVatNumber, testLegacyRelationship)(OK, Json.obj())
      stubOverseasControlListInformation(testVatNumber)

      val res = post("/subscription-request/vat-number")(Json.obj("vatNumber" -> testVatNumber))

      res should have(
        httpStatus(OK),
        jsonBodyAs(Json.obj(
          OverseasKey -> true,
          DirectDebitKey -> false
        ))
      )
    }
  }

}
