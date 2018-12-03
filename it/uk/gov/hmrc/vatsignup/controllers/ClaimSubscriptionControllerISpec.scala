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
import uk.gov.hmrc.vatsignup.helpers.servicemocks.AuthStub.{stubAuth, successfulAuthResponse, vatDecEnrolment}
import uk.gov.hmrc.vatsignup.helpers.servicemocks.EnrolmentStoreProxyStub.stubGetAllocatedEnrolmentStatus
import uk.gov.hmrc.vatsignup.helpers.servicemocks.KnownFactsStub.stubSuccessGetKnownFacts
import uk.gov.hmrc.vatsignup.helpers.servicemocks.TaxEnrolmentsStub.stubAllocateEnrolment
import uk.gov.hmrc.vatsignup.helpers.{ComponentSpecBase, CustomMatchers}
import uk.gov.hmrc.vatsignup.models.ClaimSubscriptionRequest
import uk.gov.hmrc.vatsignup.services.ClaimSubscriptionService.KnownFactsDateFormatter

class ClaimSubscriptionControllerISpec extends ComponentSpecBase with CustomMatchers {
  "/claim-subscription/vat-number/:vatNumber" when {
    "the user has an existing VATDEC enrolment" when {
      "the MTD VAT enrolment is claimed successfully" should {
        "return NO_CONTENT" in {
          stubAuth(OK, successfulAuthResponse(vatDecEnrolment))
          stubSuccessGetKnownFacts(testVatNumber)
          stubGetAllocatedEnrolmentStatus(testVatNumber)(NO_CONTENT)
          stubAllocateEnrolment(
            vatNumber = testVatNumber,
            groupId = testGroupId,
            credentialId = testCredentialId,
            postcode = testPostCode,
            vatRegistrationDate = testDateOfRegistration.toTaxEnrolmentsFormat
          )(CREATED)

          val res = post(s"/claim-subscription/vat-number/$testVatNumber")(ClaimSubscriptionRequest(isFromBta = true))

          res should have(
            httpStatus(NO_CONTENT)
          )
        }
      }
      "the enrolment is already allocatedd" should {
        "return Conflict" in {
          stubAuth(OK, successfulAuthResponse(vatDecEnrolment))
          stubSuccessGetKnownFacts(testVatNumber)
          stubGetAllocatedEnrolmentStatus(testVatNumber)(OK)

          val res = post(s"/claim-subscription/vat-number/$testVatNumber")(ClaimSubscriptionRequest(isFromBta = true))

          res should have(
            httpStatus(CONFLICT)
          )
        }
      }
    }
    "the user does not have an existing VATDEC enrolment but provides known facts" when {
      "the MTD VAT enrolment is claimed successfully" should {
        "return NO_CONTENT" in {
          stubAuth(OK, successfulAuthResponse())
          stubSuccessGetKnownFacts(testVatNumber)
          stubGetAllocatedEnrolmentStatus(testVatNumber)(NO_CONTENT)
          stubAllocateEnrolment(
            vatNumber = testVatNumber,
            groupId = testGroupId,
            credentialId = testCredentialId,
            postcode = testPostCode,
            vatRegistrationDate = testDateOfRegistration.toTaxEnrolmentsFormat
          )(CREATED)

          val res = post(s"/claim-subscription/vat-number/$testVatNumber")(
            ClaimSubscriptionRequest(
              postCode = Some(testPostCode),
              registrationDate = Some(testDateOfRegistration),
              isFromBta = true
            )
          )

          res should have(
            httpStatus(NO_CONTENT)
          )
        }
      }
    }
    "the enrolment is already allocated" should {
      "return Conflict" in {
        stubAuth(OK, successfulAuthResponse())
        stubGetAllocatedEnrolmentStatus(testVatNumber)(OK)

        val res = post(s"/claim-subscription/vat-number/$testVatNumber")(
          ClaimSubscriptionRequest(
            postCode = Some(testPostCode),
            registrationDate = Some(testDateOfRegistration),
            isFromBta = true
          )
        )

        res should have(
          httpStatus(CONFLICT)
        )
      }
    }
  }
}
