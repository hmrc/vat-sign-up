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

import play.api.http.Status._
import play.api.libs.json.Json
import uk.gov.hmrc.vatsignup.config.featureswitch.SkipPartnershipKnownFactsMismatch
import uk.gov.hmrc.vatsignup.helpers.IntegrationTestConstants._
import uk.gov.hmrc.vatsignup.helpers._
import uk.gov.hmrc.vatsignup.helpers.servicemocks.AuthStub._
import uk.gov.hmrc.vatsignup.helpers.servicemocks.PartnershipKnownFactsStub.{fullPartnershipKnownFactsBody, stubGetPartnershipKnownFacts}
import uk.gov.hmrc.vatsignup.models._

import scala.concurrent.ExecutionContext.Implicits.global

class StorePartnershipInformationControllerISpec extends ComponentSpecBase with CustomMatchers with TestSubmissionRequestRepository {

  "POST /subscription-request/vat-number/:vatNumber/partnership-information" when {
    "the user has a partnership enrolment and" when {

      val requestBody = Json.obj(
        "partnershipType" -> BusinessEntity.GeneralPartnershipKey,
        "sautr" -> testUtr
      )

      "enrolment matches the utr" should {
        "return NO_CONTENT" in {
          stubAuth(OK, successfulAuthResponse(partnershipEnrolment))

          await(submissionRequestRepo.upsertVatNumber(testVatNumber, isMigratable = true, isDirectDebit = false))

          val res = post(s"/subscription-request/vat-number/$testVatNumber/partnership-information")(requestBody)

          res should have(
            httpStatus(NO_CONTENT),
            emptyBody
          )

          val dbRequest = await(submissionRequestRepo.findById(testVatNumber)).get

          dbRequest.businessEntity shouldBe Some(GeneralPartnership(Some(testUtr)))
        }
      }
      "enrolment matches the utr and a crn is provided" should {
        "return NO_CONTENT" in {
          stubAuth(OK, successfulAuthResponse(partnershipEnrolment))

          await(submissionRequestRepo.upsertVatNumber(testVatNumber, isMigratable = true, isDirectDebit = false))

          val res = post(s"/subscription-request/vat-number/$testVatNumber/partnership-information")(
            Json.obj(
              "partnershipType" -> BusinessEntity.LimitedPartnershipKey,
              "sautr" -> testUtr,
              "crn" -> testCompanyNumber
            )
          )

          res should have(
            httpStatus(NO_CONTENT),
            emptyBody
          )

          val dbRequest = await(submissionRequestRepo.findById(testVatNumber)).get
          dbRequest.businessEntity shouldBe Some(LimitedPartnership(Some(testUtr), testCompanyNumber))
        }
      }
      "enrolment does not match the utr" should {
        "return FORBIDDEN" in {
          stubAuth(OK, successfulAuthResponse(partnershipEnrolment))

          await(submissionRequestRepo.upsertVatNumber(testVatNumber, isMigratable = true, isDirectDebit = false))

          val requestBody = Json.obj(
            "partnershipType" -> BusinessEntity.GeneralPartnershipKey,
            "sautr" -> testUtr.drop(1)
          )

          val res = post(s"/subscription-request/vat-number/$testVatNumber/partnership-information")(requestBody)

          res should have(
            httpStatus(FORBIDDEN),
            emptyBody
          )

          val dbRequest = await(submissionRequestRepo.findById(testVatNumber)).get
          dbRequest.businessEntity shouldBe None
        }
      }

      "the vat number does not already exists" should {
        "return PRECONDITION_FAILED" in {
          stubAuth(OK, successfulAuthResponse(partnershipEnrolment))

          val res = post(s"/subscription-request/vat-number/$testVatNumber/partnership-information")(requestBody)

          res should have(
            httpStatus(PRECONDITION_FAILED),
            emptyBody
          )
        }

      }
    }

    "the user does not have a partnership enrolment and " when {

      "postcode is provided and " when {
        val requestBody = Json.obj(
          "partnershipType" -> BusinessEntity.GeneralPartnershipKey,
          "sautr" -> testUtr,
          "postCode" -> testPostCode
        )

        "the postcode matches one of the postcodes retrieved using the sautr" should {
          "return NO_CONTENT" in {
            stubAuth(OK, successfulAuthResponse())
            stubGetPartnershipKnownFacts(testUtr)(OK, Some(fullPartnershipKnownFactsBody))

            await(submissionRequestRepo.upsertVatNumber(testVatNumber, isMigratable = true, isDirectDebit = false))

            val res = post(s"/subscription-request/vat-number/$testVatNumber/partnership-information")(requestBody)

            res should have(
              httpStatus(NO_CONTENT),
              emptyBody
            )

            val dbRequest = await(submissionRequestRepo.findById(testVatNumber)).get

            dbRequest.businessEntity shouldBe Some(GeneralPartnership(Some(testUtr)))
          }
        }
        "the postcode and a crn is provided and the postcode matches one of the postcodes retrieved using the sautr" should {
          "return NO_CONTENT" in {
            stubAuth(OK, successfulAuthResponse())
            stubGetPartnershipKnownFacts(testUtr)(OK, Some(fullPartnershipKnownFactsBody))

            await(submissionRequestRepo.upsertVatNumber(testVatNumber, isMigratable = true, isDirectDebit = false))

            val res = post(s"/subscription-request/vat-number/$testVatNumber/partnership-information")(
              Json.obj(
                "partnershipType" -> BusinessEntity.LimitedPartnershipKey,
                "sautr" -> testUtr,
                "crn" -> testCompanyNumber,
                "postCode" -> testPostCode
              )
            )

            res should have(
              httpStatus(NO_CONTENT),
              emptyBody
            )

            val dbRequest = await(submissionRequestRepo.findById(testVatNumber)).get
            dbRequest.businessEntity shouldBe Some(LimitedPartnership(Some(testUtr), testCompanyNumber))
          }
        }
        "No utr and no postcode was provided" should {
          "return NO_CONTENT" in {
            stubAuth(OK, successfulAuthResponse())

            await(submissionRequestRepo.upsertVatNumber(testVatNumber, isMigratable = true, isDirectDebit = false))

            val res = post(s"/subscription-request/vat-number/$testVatNumber/partnership-information")(
              Json.obj("partnershipType" -> BusinessEntity.GeneralPartnershipKey)
            )

            res should have(
              httpStatus(NO_CONTENT),
              emptyBody
            )

            val dbRequest = await(submissionRequestRepo.findById(testVatNumber)).get
            dbRequest.businessEntity shouldBe Some(GeneralPartnership(None))
          }
        }
        "the postcode provided does not match any of the postcodes retrieved using the utr" should {
          "return FORBIDDEN" in {
            stubAuth(OK, successfulAuthResponse())

            stubGetPartnershipKnownFacts(testUtr)(OK, Some(Json.obj(
              "postCode" -> "NO MATCH",
              "correspondenceDetails" -> Json.obj(
                "corresPostCode" -> ""
              ),
              "basePostCode" -> "",
              "commsPostCode" -> "",
              "traderPostCode" -> ""
            )))

            await(submissionRequestRepo.upsertVatNumber(testVatNumber, isMigratable = true, isDirectDebit = false))

            val requestBody = Json.obj(
              "partnershipType" -> BusinessEntity.GeneralPartnershipKey,
              "sautr" -> testUtr,
              "postCode" -> testPostCode
            )

            val res = post(s"/subscription-request/vat-number/$testVatNumber/partnership-information")(requestBody)

            res should have(
              httpStatus(FORBIDDEN),
              emptyBody
            )

            val dbRequest = await(submissionRequestRepo.findById(testVatNumber)).get
            dbRequest.businessEntity shouldBe None
          }
        }

        "no records were found the utr" should {
          "return PRECONDITION_FAILED" in {
            val invalidUtr = testUtr.drop(1)

            stubAuth(OK, successfulAuthResponse())
            stubGetPartnershipKnownFacts(invalidUtr)(NOT_FOUND)

            await(submissionRequestRepo.upsertVatNumber(testVatNumber, isMigratable = true, isDirectDebit = false))

            val requestBody = Json.obj(
              "partnershipType" -> BusinessEntity.GeneralPartnershipKey,
              "sautr" -> invalidUtr,
              "postCode" -> testPostCode
            )

            val res = post(s"/subscription-request/vat-number/$testVatNumber/partnership-information")(requestBody)

            res should have(
              httpStatus(PRECONDITION_FAILED),
              jsonBodyAs(Json.obj(
                "statusCode" -> PRECONDITION_FAILED,
                "message" -> StorePartnershipInformationController.invalidSautrKey
              ))
            )

            val dbRequest = await(submissionRequestRepo.findById(testVatNumber)).get
            dbRequest.businessEntity shouldBe None
          }
        }

        "no post codes were retrieved using the utr" when {
          "the SkipPartnershipKnownFactsMismatch feature switch is disabled" should {
            "return INTERNAL_SERVER_ERROR" in {
              stubAuth(OK, successfulAuthResponse())
              stubGetPartnershipKnownFacts(testUtr)(OK, Some(Json.obj()))

              val res = post(s"/subscription-request/vat-number/$testVatNumber/partnership-information")(requestBody)

              res should have(
                httpStatus(INTERNAL_SERVER_ERROR),
                jsonBodyAs(Json.obj(
                  "statusCode" -> INTERNAL_SERVER_ERROR,
                  "message" -> "No postcodes returned for the partnership"
                ))
              )
            }
          }

          "the SkipPartnershipKnownFactsMismatch feature switch is enabled" should {
            "return No Content" in {
              enable(SkipPartnershipKnownFactsMismatch)
              stubAuth(OK, successfulAuthResponse())
              stubGetPartnershipKnownFacts(testUtr)(OK, Some(Json.obj()))

              await(submissionRequestRepo.upsertVatNumber(testVatNumber, isMigratable = true, isDirectDebit = false))

              val res = post(s"/subscription-request/vat-number/$testVatNumber/partnership-information")(requestBody)

              res should have(
                httpStatus(NO_CONTENT),
                emptyBody
              )

              val dbRequest = await(submissionRequestRepo.findById(testVatNumber)).get
              dbRequest.businessEntity shouldBe Some(GeneralPartnership(None))
            }
          }
        }

        "the vat number does not already exists" should {
          "return PRECONDITION_FAILED" in {
            stubAuth(OK, successfulAuthResponse())
            stubGetPartnershipKnownFacts(testUtr)(OK, Some(fullPartnershipKnownFactsBody))

            val res = post(s"/subscription-request/vat-number/$testVatNumber/partnership-information")(requestBody)

            res should have(
              httpStatus(PRECONDITION_FAILED),
              emptyBody
            )
          }
        }
      }
    }
  }

}
