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

import org.scalatest.BeforeAndAfterEach
import play.api.http.Status._
import play.api.libs.json.Json
import uk.gov.hmrc.vatsignup.config.Constants.ControlList.OverseasKey
import uk.gov.hmrc.vatsignup.helpers.IntegrationTestConstants._
import uk.gov.hmrc.vatsignup.helpers.servicemocks.AuthStub._
import uk.gov.hmrc.vatsignup.helpers.servicemocks.GetMandationStatusStub.{mandationStatusBody, stubGetMandationStatus}
import uk.gov.hmrc.vatsignup.helpers.servicemocks.KnownFactsAndControlListInformationStub._
import uk.gov.hmrc.vatsignup.helpers.{ComponentSpecBase, CustomMatchers}
import uk.gov.hmrc.vatsignup.models.MTDfBVoluntary

class VatNumberEligibilityControllerISpec extends ComponentSpecBase with BeforeAndAfterEach with CustomMatchers {

  "/subscription-request/vat-number/:vatNumber/mtdfb-eligibility" when {
    "the control list info is 33 bit" when {
      "the user does not exist on ETMP" when {
        "the user is eligible" should {
          "return OK" in {
            stubAuth(OK, successfulAuthResponse())
            stubGetKnownFactsAndControlListInformation33(testVatNumber, testPostCode, testDateOfRegistration)

            val res = await(get(s"/subscription-request/vat-number/$testVatNumber/mtdfb-eligibility"))

            res should have(
              httpStatus(OK)
            )
          }
        }
      }
    }
    "the control list info is 34 bit" when {
      "the user does not exist on ETMP" when {
        "the user is eligible" should {
          "return OK" in {
            stubAuth(OK, successfulAuthResponse())
            stubGetKnownFactsAndControlListInformation34(testVatNumber, testPostCode, testDateOfRegistration)

            val res = await(get(s"/subscription-request/vat-number/$testVatNumber/mtdfb-eligibility"))

            res should have(
              httpStatus(OK),
              jsonBodyAs(Json.obj(
                OverseasKey -> false
              ))
            )
          }
        }

        "the user is eligible and has multiple Business Entities" should {
          "return OK" in {
            stubAuth(OK, successfulAuthResponse())
            stubGetKnownFactsAndControlListInformationMultipleEntities34(testVatNumber, testPostCode, testDateOfRegistration)

            val res = await(get(s"/subscription-request/vat-number/$testVatNumber/mtdfb-eligibility"))

            res should have(
              httpStatus(OK),
              jsonBodyAs(Json.obj(
                OverseasKey -> false
              ))
            )
          }
        }

        "the user is overseas" should {
          "return OK" in {
            stubAuth(OK, successfulAuthResponse())
            stubOverseasControlListInformation(testVatNumber)

            val res = await(get(s"/subscription-request/vat-number/$testVatNumber/mtdfb-eligibility"))

            res should have(
              httpStatus(OK),
              jsonBodyAs(Json.obj(
                OverseasKey -> true
              ))
            )
          }
        }
      }
    }
    "the user does not exist on ETMP" when {
      "the user is ineligible" should {
        "return BAD_REQUEST" in {
          stubAuth(OK, successfulAuthResponse())
          stubIneligibleControlListInformation(testVatNumber)

          val res = await(get(s"/subscription-request/vat-number/$testVatNumber/mtdfb-eligibility"))

          res should have(
            httpStatus(BAD_REQUEST)
          )
        }
      }
    }
  }
}
