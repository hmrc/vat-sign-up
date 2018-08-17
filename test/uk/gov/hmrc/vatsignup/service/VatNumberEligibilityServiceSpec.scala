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

package uk.gov.hmrc.vatsignup.service

import org.scalatest.EitherValues
import play.api.http.Status
import play.api.mvc.Request
import play.api.test.FakeRequest
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.vatsignup.config.featureswitch.ClaimSubscription
import uk.gov.hmrc.vatsignup.config.mocks.MockConfig
import uk.gov.hmrc.vatsignup.connectors.mocks.MockMandationStatusConnector
import uk.gov.hmrc.vatsignup.helpers.TestConstants._
import uk.gov.hmrc.vatsignup.httpparsers.GetMandationStatusHttpParser.GetMandationStatusHttpFailure
import uk.gov.hmrc.vatsignup.httpparsers._
import uk.gov.hmrc.vatsignup.models._
import uk.gov.hmrc.vatsignup.service.mocks.MockControlListEligibilityService
import uk.gov.hmrc.vatsignup.services.ControlListEligibilityService.EligibilitySuccess
import uk.gov.hmrc.vatsignup.services.VatNumberEligibilityService._
import uk.gov.hmrc.vatsignup.services.{ControlListEligibilityService, VatNumberEligibilityService}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class VatNumberEligibilityServiceSpec extends UnitSpec with EitherValues
  with MockMandationStatusConnector with MockControlListEligibilityService with MockConfig {

  object TestVatNumberEligibilityService extends VatNumberEligibilityService(
    mockMandationStatusConnector,
    mockControlListEligibilityService,
    mockConfig
  )

  implicit val hc: HeaderCarrier = HeaderCarrier()
  implicit val request: Request[_] = FakeRequest()

  "checkVatNumberEligibility" when {
    "the AlreadySubscribedCheck feature switch is enabled" when {
      "the mandation status service returns NonMTDfB" when {
        "the MTDEligibilityCheck feature switch is enabled" when {
          "the known facts and control list service returns Migratable" should {
            "return VatNumberEligible" in {
              mockGetMandationStatus(testVatNumber)(Future.successful(Right(NonMTDfB)))
              mockGetEligibilityStatus(testVatNumber)(Future.successful(Right(EligibilitySuccess(testPostCode, testDateOfRegistration, isMigratable = true))))

              await(TestVatNumberEligibilityService.checkVatNumberEligibility(testVatNumber)).right.value shouldBe VatNumberEligible
            }
          }
          "the known facts and control list service returns NonMigratable" should {
            "return VatNumberEligible" in {
              mockGetMandationStatus(testVatNumber)(Future.successful(Right(NonMTDfB)))
              mockGetEligibilityStatus(testVatNumber)(Future.successful(Right(EligibilitySuccess(testPostCode, testDateOfRegistration, isMigratable = false))))

              await(TestVatNumberEligibilityService.checkVatNumberEligibility(testVatNumber)).right.value shouldBe VatNumberEligible
            }
          }
          "the known facts and control list service returns a control list information that is ineligible" should {
            "return VatNumberIneligible" in {
              mockGetMandationStatus(testVatNumber)(Future.successful(Right(NonMTDfB)))
              mockGetEligibilityStatus(testVatNumber)(Future.successful(Left(ControlListEligibilityService.IneligibleVatNumber)))

              await(TestVatNumberEligibilityService.checkVatNumberEligibility(testVatNumber)).left.value shouldBe VatNumberIneligible
            }
          }
          "the known facts and control list service returns KnownFactsInvalidVatNumber" should {
            "return InvalidVatNumber" in {
              mockGetMandationStatus(testVatNumber)(Future.successful(Right(NonMTDfB)))
              mockGetEligibilityStatus(testVatNumber)(Future.successful(Left(ControlListEligibilityService.InvalidVatNumber)))

              await(TestVatNumberEligibilityService.checkVatNumberEligibility(testVatNumber)).left.value shouldBe InvalidVatNumber
            }
          }
          "the known facts and control list service returns ControlListInformationVatNumberNotFound" should {
            "return VatNumberNotFound" in {
              mockGetMandationStatus(testVatNumber)(Future.successful(Right(NonMTDfB)))
              mockGetEligibilityStatus(testVatNumber)(Future.successful(Left(ControlListEligibilityService.VatNumberNotFound)))

              await(TestVatNumberEligibilityService.checkVatNumberEligibility(testVatNumber)).left.value shouldBe VatNumberNotFound
            }
          }
          "the known facts and control list service returns any other error" should {
            "return KnownFactsAndControlListFailure" in {
              mockGetMandationStatus(testVatNumber)(Future.successful(Right(NonMTDfB)))
              mockGetEligibilityStatus(testVatNumber)(Future.successful(Left(ControlListEligibilityService.KnownFactsAndControlListFailure)))

              await(TestVatNumberEligibilityService.checkVatNumberEligibility(testVatNumber)).left.value shouldBe KnownFactsAndControlListFailure
            }
          }
        }
      }
      "the mandation status service returns NonDigital" when {
        "the known facts and control list service returns MtdEligible" should {
          "return VatNumberEligible" in {
            mockGetMandationStatus(testVatNumber)(Future.successful(Right(NonDigital)))
            mockGetEligibilityStatus(testVatNumber)(Future.successful(Right(EligibilitySuccess(testPostCode, testDateOfRegistration, isMigratable = true))))

            await(TestVatNumberEligibilityService.checkVatNumberEligibility(testVatNumber)).right.value shouldBe VatNumberEligible
          }
        }

      }
      "the mandation status service returns VatNumberNotFound" should {
        "the known facts and control list service returns MtdEligible" should {
          "return VatNumberEligible" in {
            mockGetMandationStatus(testVatNumber)(Future.successful(Left(GetMandationStatusHttpParser.VatNumberNotFound)))
            mockGetEligibilityStatus(testVatNumber)(Future.successful(Right(EligibilitySuccess(testPostCode, testDateOfRegistration, isMigratable = true))))

            await(TestVatNumberEligibilityService.checkVatNumberEligibility(testVatNumber)).right.value shouldBe VatNumberEligible
          }
        }
      }
      "the mandation status service returns MTDfBMandated" should {
        "return AlreadySubscribed" in {
          mockGetMandationStatus(testVatNumber)(Future.successful(Right(MTDfBMandated)))
          await(TestVatNumberEligibilityService.checkVatNumberEligibility(testVatNumber)).left.value shouldBe AlreadySubscribed
        }
      }
      "the mandation status service returns MTDfBVoluntary" should {
        "return AlreadySubscribed" in {
          mockGetMandationStatus(testVatNumber)(Future.successful(Right(MTDfBVoluntary)))
          await(TestVatNumberEligibilityService.checkVatNumberEligibility(testVatNumber)).left.value shouldBe AlreadySubscribed
        }
      }
      "the mandation status service returns any other error" should {
        "return GetVatCustomerInformationFailure" in {
          mockGetMandationStatus(testVatNumber)(Future.successful(Left(GetMandationStatusHttpFailure(Status.INTERNAL_SERVER_ERROR, ""))))
          await(TestVatNumberEligibilityService.checkVatNumberEligibility(testVatNumber)).left.value shouldBe GetVatCustomerInformationFailure
        }
      }
    }
    "the claim subscription feature switch is enabled" should {
      "skip the already subscribed check" in {
        enable(ClaimSubscription)

        mockGetEligibilityStatus(testVatNumber)(Future.successful(Right(EligibilitySuccess(testPostCode, testDateOfRegistration, isMigratable = true))))

        val res = await(TestVatNumberEligibilityService.checkVatNumberEligibility(testVatNumber))
        res shouldBe Right(VatNumberEligible)

      }

    }
  }

}
