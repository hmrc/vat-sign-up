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
import uk.gov.hmrc.vatsignup.helpers.IntegrationTestConstants._
import uk.gov.hmrc.vatsignup.helpers._
import uk.gov.hmrc.vatsignup.helpers.servicemocks.AuthStub._
import uk.gov.hmrc.vatsignup.models.PartnershipEntityType.GeneralPartnership
import uk.gov.hmrc.vatsignup.models.PartnershipInformation

import scala.concurrent.ExecutionContext.Implicits.global

class StorePartnershipInformationControllerISpec extends ComponentSpecBase with CustomMatchers with TestSubmissionRequestRepository {


  val requestBody = PartnershipInformation(GeneralPartnership, testUtr)

  "POST /subscription-request/vat-number/:vatNumber/partnership-information" when {
    "enrolment matches the utr" should {
      "return NO_CONTENT" in {
        stubAuth(OK, successfulAuthResponse(partnershipEnrolment))

        await(submissionRequestRepo.upsertVatNumber(testVatNumber, isMigratable = true))

        val res = post(s"/subscription-request/vat-number/$testVatNumber/partnership-information")(requestBody)

        res should have(
          httpStatus(NO_CONTENT),
          emptyBody
        )

        val dbRequest = await(submissionRequestRepo.findById(testVatNumber)).get
        dbRequest.partnershipEntity shouldBe Some(GeneralPartnership)
        dbRequest.partnershipUtr shouldBe Some(testUtr)
      }
    }
    "enrolment does not matches the utr" should {
      "return NO_CONTENT" in {
        stubAuth(OK, successfulAuthResponse(partnershipEnrolment))

        await(submissionRequestRepo.upsertVatNumber(testVatNumber, isMigratable = true))

        val requestBody = PartnershipInformation(GeneralPartnership, testUtr.drop(1))

        val res = post(s"/subscription-request/vat-number/$testVatNumber/partnership-information")(requestBody)

        res should have(
          httpStatus(FORBIDDEN),
          emptyBody
        )

        val dbRequest = await(submissionRequestRepo.findById(testVatNumber)).get
        dbRequest.partnershipEntity shouldBe None
        dbRequest.partnershipUtr shouldBe None
      }
    }

    "the vat number does not already exists" should {
      "return NOT_FOUND" in {
        stubAuth(OK, successfulAuthResponse(partnershipEnrolment))

        val res = post(s"/subscription-request/vat-number/$testVatNumber/partnership-information")(requestBody)

        res should have(
          httpStatus(NOT_FOUND),
          emptyBody
        )
      }
    }
  }

}
