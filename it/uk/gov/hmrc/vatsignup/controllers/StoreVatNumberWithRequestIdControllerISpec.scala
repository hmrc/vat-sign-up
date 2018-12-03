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
import uk.gov.hmrc.vatsignup.config.Constants.HttpCodeKey
import uk.gov.hmrc.vatsignup.controllers.StoreVatNumberWithRequestIdController.SubscriptionClaimedCode
import uk.gov.hmrc.vatsignup.helpers.IntegrationTestConstants._
import uk.gov.hmrc.vatsignup.helpers._
import uk.gov.hmrc.vatsignup.helpers.servicemocks.AgentClientRelationshipsStub._
import uk.gov.hmrc.vatsignup.helpers.servicemocks.AuthStub._
import uk.gov.hmrc.vatsignup.helpers.servicemocks.GetMandationStatusStub._
import uk.gov.hmrc.vatsignup.helpers.servicemocks.KnownFactsAndControlListInformationStub._
import uk.gov.hmrc.vatsignup.helpers.servicemocks.EnrolmentStoreProxyStub._
import uk.gov.hmrc.vatsignup.helpers.servicemocks.KnownFactsStub.stubSuccessGetKnownFacts
import uk.gov.hmrc.vatsignup.helpers.servicemocks.TaxEnrolmentsStub.stubAllocateEnrolment
import uk.gov.hmrc.vatsignup.httpparsers.AgentClientRelationshipsHttpParser.NoRelationshipCode
import uk.gov.hmrc.vatsignup.models.{MTDfBVoluntary, NonMTDfB, UnconfirmedSubscriptionRequest}
import uk.gov.hmrc.vatsignup.services.ClaimSubscriptionService._

class StoreVatNumberWithRequestIdControllerISpec extends ComponentSpecBase
  with CustomMatchers with TestUnconfirmedSubmissionRequestRepository {

  val jsonVatRequest = Json.obj(
    "requestId" -> testToken,
    "vatNumber" -> testVatNumber
  )

  override def beforeEach(): Unit = {
    import scala.concurrent.ExecutionContext.Implicits.global
    super.beforeEach()
    await(unconfirmedSubmissionRequestRepo.insert(UnconfirmedSubscriptionRequest(testToken)))
  }

  "POST /sign-up-request/request-id/:requestId/vat-number" when {
    "the user is an agent" should {
      "return CREATED when the vat number has been stored successfully" in {
        stubAuth(OK, successfulAuthResponse(agentEnrolment))
        stubCheckAgentClientRelationship(testAgentNumber, testVatNumber)(OK, Json.obj())
        stubGetMandationStatus(testVatNumber)(OK, mandationStatusBody(NonMTDfB))
        stubSuccessGetKnownFactsAndControlListInformation(testVatNumber)

        val res = post(s"/sign-up-request/request-id/$testToken/vat-number")(jsonVatRequest)

        res should have(
          httpStatus(CREATED),
          emptyBody
        )
      }

      "return CONFLICT when the client is already subscribed" in {
        stubAuth(OK, successfulAuthResponse(agentEnrolment))
        stubCheckAgentClientRelationship(testAgentNumber, testVatNumber)(OK, Json.obj())
        stubGetMandationStatus(testVatNumber)(OK, mandationStatusBody(MTDfBVoluntary))

        val res = post(s"/sign-up-request/request-id/$testToken/vat-number")(jsonVatRequest)

        res should have(
          httpStatus(CONFLICT),
          emptyBody
        )
      }

      "return FORBIDDEN when there is no relationship" in {
        stubAuth(OK, successfulAuthResponse(agentEnrolment))
        stubCheckAgentClientRelationship(testAgentNumber, testVatNumber)(NOT_FOUND, Json.obj(
          "code" -> NoRelationshipCode
        ))

        val res = post(s"/sign-up-request/request-id/$testToken/vat-number")(jsonVatRequest)

        res should have(
          httpStatus(FORBIDDEN),
          jsonBodyAs(Json.obj(Constants.HttpCodeKey -> NoRelationshipCode))
        )
      }

      "return BAD_REQUEST when the json is invalid" in {
        stubAuth(OK, successfulAuthResponse())

        val res = post(s"/sign-up-request/request-id/$testToken/vat-number")(Json.obj())

        res should have(
          httpStatus(BAD_REQUEST)
        )
      }
    }

    "the user is a principal user" when {
      "the user has a HMCE-VAT enrolment" should {
        "return CREATED when the vat number has been stored successfully" in {
          stubAuth(OK, successfulAuthResponse(vatDecEnrolment))
          stubGetMandationStatus(testVatNumber)(OK, mandationStatusBody(NonMTDfB))
          stubSuccessGetKnownFactsAndControlListInformation(testVatNumber)

          val res = post(s"/sign-up-request/request-id/$testToken/vat-number")(jsonVatRequest)

          res should have(
            httpStatus(CREATED),
            emptyBody
          )
        }

        "claim the enrolment when the user is already subscribed" in {
          stubAuth(OK, successfulAuthResponse(vatDecEnrolment))
          stubCheckAgentClientRelationship(testAgentNumber, testVatNumber)(OK, Json.obj())
          stubGetMandationStatus(testVatNumber)(OK, mandationStatusBody(MTDfBVoluntary))
          stubSuccessGetKnownFacts(testVatNumber)
          stubGetAllocatedEnrolmentStatus(testVatNumber)(NO_CONTENT)
          stubAllocateEnrolment(
            vatNumber = testVatNumber,
            groupId = testGroupId,
            credentialId = testCredentialId,
            postcode = testPostCode,
            vatRegistrationDate = testDateOfRegistration.toTaxEnrolmentsFormat
          )(CREATED)

          val res = post(s"/sign-up-request/request-id/$testToken/vat-number")(jsonVatRequest.++(Json.obj("isFromBta" -> true)))

          res should have(
            httpStatus(OK),
            jsonBodyAs(Json.obj(HttpCodeKey -> SubscriptionClaimedCode))
          )
        }

        "return FORBIDDEN when vat number does not match the one in enrolment" in {
          stubAuth(OK, successfulAuthResponse(vatDecEnrolment))

          val res = post(s"/sign-up-request/request-id/$testToken/vat-number")(Json.obj(
            "requestId" -> testToken,
            "vatNumber" -> UUID.randomUUID().toString
          ))

          res should have(
            httpStatus(FORBIDDEN),
            jsonBodyAs(Json.obj(Constants.HttpCodeKey -> "DoesNotMatchEnrolment"))
          )
        }

        "return BAD_REQUEST when the json is invalid" in {
          stubAuth(OK, successfulAuthResponse())

          val res = post(s"/sign-up-request/request-id/$testToken/vat-number")(Json.obj())

          res should have(
            httpStatus(BAD_REQUEST)
          )
        }
      }
    }

    "does not have a HMCE-VAT enrolment but has provided known facts" should {
      "return CREATED when the vat number has been stored successfully" in {
        stubAuth(OK, successfulAuthResponse())
        stubGetMandationStatus(testVatNumber)(OK, mandationStatusBody(NonMTDfB))
        stubSuccessGetKnownFactsAndControlListInformation(testVatNumber)

        val res = post(s"/sign-up-request/request-id/$testToken/vat-number")(Json.obj(
          "requestId" -> testToken,
          "vatNumber" -> testVatNumber,
          "postCode" -> testPostCode,
          "registrationDate" -> testDateOfRegistration
        ))

        res should have(
          httpStatus(CREATED),
          emptyBody
        )
      }
      "return FORBIDDEN when known facts mismatch" in {
        stubAuth(OK, successfulAuthResponse())
        stubGetMandationStatus(testVatNumber)(OK, mandationStatusBody(NonMTDfB))
        stubSuccessGetKnownFactsAndControlListInformation(testVatNumber)

        val res = post(s"/sign-up-request/request-id/$testToken/vat-number")(Json.obj(
          "requestId" -> testToken,
          "vatNumber" -> testVatNumber,
          "postCode" -> testVatNumber,
          "registrationDate" -> testVatNumber
        ))

        res should have(
          httpStatus(FORBIDDEN),
          jsonBodyAs(Json.obj(Constants.HttpCodeKey -> "KNOWN_FACTS_MISMATCH"))
        )
      }
      "return PRECONDITION_FAILED when vat number is not found" in {
        stubAuth(OK, successfulAuthResponse())
        stubGetMandationStatus(testVatNumber)(OK, mandationStatusBody(NonMTDfB))
        stubFailureControlListVatNumberNotFound(testVatNumber)

        val res = post(s"/sign-up-request/request-id/$testToken/vat-number")(Json.obj(
          "requestId" -> testToken,
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

        val res = post(s"/sign-up-request/request-id/$testToken/vat-number")(Json.obj(
          "requestId" -> testToken,
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

        val res = post(s"/sign-up-request/request-id/$testToken/vat-number")(jsonVatRequest)

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

      val res = post(s"/sign-up-request/request-id/$testToken/vat-number")(jsonVatRequest)

      res should have(
        httpStatus(FORBIDDEN),
        jsonBodyAs(Json.obj(Constants.HttpCodeKey -> "InsufficientEnrolments"))
      )
    }
  }

}
