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

package uk.gov.hmrc.vatsignup.service

import play.api.test.Helpers._
import play.api.mvc.Request
import play.api.test.FakeRequest
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}
import org.scalatest.{WordSpec, Matchers}
import uk.gov.hmrc.vatsignup.connectors.mocks.{MockMandationStatusConnector, MockVatCustomerDetailsConnector}
import uk.gov.hmrc.vatsignup.helpers.TestConstants._
import uk.gov.hmrc.vatsignup.httpparsers.GetMandationStatusHttpParser.{GetMandationStatusHttpFailure, VatNumberNotFound}
import uk.gov.hmrc.vatsignup.httpparsers.KnownFactsHttpParser.KnownFacts
import uk.gov.hmrc.vatsignup.httpparsers.{GetMandationStatusHttpParser, VatCustomerDetailsHttpParser}
import uk.gov.hmrc.vatsignup.models._
import uk.gov.hmrc.vatsignup.service.mocks.MockControlListEligibilityService
import uk.gov.hmrc.vatsignup.services.ControlListEligibilityService.{EligibilitySuccess, IneligibleVatNumber, KnownFactsAndControlListFailure}
import uk.gov.hmrc.vatsignup.services.VatNumberEligibilityService._
import uk.gov.hmrc.vatsignup.services.{ControlListEligibilityService, VatNumberEligibilityService}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class VatNumberEligibilityServiceSpec extends WordSpec with Matchers
  with MockMandationStatusConnector with MockControlListEligibilityService with MockVatCustomerDetailsConnector {

  object TestVatNumberEligibilityService extends VatNumberEligibilityService(
    mockMandationStatusConnector,
    mockControlListEligibilityService,
    mockVatCustomerDetailsConnector
  )

  implicit val hc: HeaderCarrier = HeaderCarrier()
  implicit val req: Request[_] = FakeRequest()

  "getMtdStatus" when {
    "the user is migrated" when {
      "the user is already subscribed" when {
        "the user is mandated to MTD" should {
          s"return $AlreadySubscribed" in {
            mockGetMandationStatus(testVatNumber)(Future.successful(Right(MTDfBMandated)))
            mockGetVatCustomerDetails(testVatNumber)(
              Future.successful(Right(VatCustomerDetails(KnownFacts(testPostCode, testDateOfRegistration), isOverseas = false)))
            )

            await(TestVatNumberEligibilityService.getMtdStatus(testVatNumber)) shouldBe AlreadySubscribed(false)
          }
        }

        "the user is MTDfB and not overseas" should {
          s"return $AlreadySubscribed" in {
            mockGetMandationStatus(testVatNumber)(Future.successful(Right(MTDfB)))
            mockGetVatCustomerDetails(testVatNumber)(
              Future.successful(Right(VatCustomerDetails(KnownFacts(testPostCode, testDateOfRegistration), isOverseas = false)))
            )

            await(TestVatNumberEligibilityService.getMtdStatus(testVatNumber)) shouldBe AlreadySubscribed(false)
          }
        }

        "the user is MTDfB and overseas" should {
          s"return $AlreadySubscribed" in {
            mockGetMandationStatus(testVatNumber)(Future.successful(Right(MTDfB)))
            mockGetVatCustomerDetails(testVatNumber)(
              Future.successful(Right(VatCustomerDetails(KnownFacts(testPostCode, testDateOfRegistration), isOverseas = true)))
            )

            await(TestVatNumberEligibilityService.getMtdStatus(testVatNumber)) shouldBe AlreadySubscribed(true)
          }
        }

        "the user is voluntarily in MTD" should {
          s"return $AlreadySubscribed" in {
            mockGetMandationStatus(testVatNumber)(Future.successful(Right(MTDfBVoluntary)))
            mockGetVatCustomerDetails(testVatNumber)(
              Future.successful(Right(VatCustomerDetails(KnownFacts(testPostCode, testDateOfRegistration), isOverseas = false)))
            )

            await(TestVatNumberEligibilityService.getMtdStatus(testVatNumber)) shouldBe AlreadySubscribed(false)
          }
        }
      }

      "the user is not already subscribed" when {
        "the user is non-MTD" when {
          "the user is overseas" should {
            s"return $Eligible with the overseas flag set to true" in {
              mockGetMandationStatus(testVatNumber)(Future.successful(Right(NonMTDfB)))
              mockGetVatCustomerDetails(testVatNumber)(
                Future.successful(Right(VatCustomerDetails(KnownFacts(testPostCode, testDateOfRegistration), isOverseas = true)))
              )

              await(TestVatNumberEligibilityService.getMtdStatus(testVatNumber)) shouldBe Eligible(migrated = true, overseas = true)
            }
          }

          "the user is not overseas" should {
            s"return $Eligible with the overseas flag set to true" in {
              mockGetMandationStatus(testVatNumber)(Future.successful(Right(NonMTDfB)))
              mockGetVatCustomerDetails(testVatNumber)(
                Future.successful(Right(VatCustomerDetails(KnownFacts(testPostCode, testDateOfRegistration), isOverseas = false)))
              )

              await(TestVatNumberEligibilityService.getMtdStatus(testVatNumber)) shouldBe Eligible(migrated = true, overseas = false)
            }
          }

          "the user is deregistered" should {
            s"return $Deregistered" in {
              mockGetMandationStatus(testVatNumber)(Future.successful(Right(NonMTDfB)))
              mockGetVatCustomerDetails(testVatNumber)(
                Future.successful(Left(VatCustomerDetailsHttpParser.Deregistered))
              )

              await(TestVatNumberEligibilityService.getMtdStatus(testVatNumber)) shouldBe Deregistered
            }
          }
        }

        "the user is non-digital" should {
          s"return $Eligible" in {
            mockGetMandationStatus(testVatNumber)(Future.successful(Right(NonDigital)))
            mockGetVatCustomerDetails(testVatNumber)(
              Future.successful(Right(VatCustomerDetails(KnownFacts(testPostCode, testDateOfRegistration), isOverseas = false)))
            )

            await(TestVatNumberEligibilityService.getMtdStatus(testVatNumber)) shouldBe Eligible(migrated = true, overseas = false)
          }
        }

        "the user is MTDfBExempt" should {
          s"return $Eligible" in {
            mockGetMandationStatus(testVatNumber)(Future.successful(Right(MTDfBExempt)))
            mockGetVatCustomerDetails(testVatNumber)(
              Future.successful(Right(VatCustomerDetails(KnownFacts(testPostCode, testDateOfRegistration), isOverseas = false)))
            )

            await(TestVatNumberEligibilityService.getMtdStatus(testVatNumber)) shouldBe Eligible(migrated = true, overseas = false)
          }
        }
      }
    }

    "the user is not migrated" when {
      "the user is eligible" when {
        "the user is not inhibited" when {
          "the user is overseas" should {
            s"return $Eligible with the overseas flag populated from the control list" in {
              mockGetMandationStatus(testVatNumber)(Future.successful(Left(VatNumberNotFound)))
              mockGetEligibilityStatus(testVatNumber)(Future.successful(
                Right(EligibilitySuccess(
                  testFourKnownFacts,
                  isMigratable = true,
                  isOverseas = true,
                  isDirectDebit = false
                ))
              ))

              await(TestVatNumberEligibilityService.getMtdStatus(testVatNumber)) shouldBe Eligible(migrated = false, overseas = true)
            }
          }

          "the user is not overseas" should {
            s"return $Eligible with the overseas flag populated from the control list" in {
              mockGetMandationStatus(testVatNumber)(Future.successful(Left(VatNumberNotFound)))
              mockGetEligibilityStatus(testVatNumber)(Future.successful(
                Right(EligibilitySuccess(
                  testFourKnownFacts,
                  isMigratable = true,
                  isOverseas = false,
                  isDirectDebit = false
                ))
              ))

              await(TestVatNumberEligibilityService.getMtdStatus(testVatNumber)) shouldBe Eligible(migrated = false, overseas = false)
            }
          }
        }

        "the user is inhibited" should {
          s"return ${Inhibited(testMigratableDates)}" in {
            mockGetMandationStatus(testVatNumber)(Future.successful(Left(VatNumberNotFound)))
            mockGetEligibilityStatus(testVatNumber)(Future.successful(
              Left(IneligibleVatNumber(testMigratableDates))
            ))

            await(TestVatNumberEligibilityService.getMtdStatus(testVatNumber)) shouldBe Inhibited(testMigratableDates)
          }
        }
      }

      "the user is ineligible" should {
        s"return $Ineligible" in {
          mockGetMandationStatus(testVatNumber)(Future.successful(Left(VatNumberNotFound)))
          mockGetEligibilityStatus(testVatNumber)(Future.successful(Left(IneligibleVatNumber(MigratableDates.empty))))

          await(TestVatNumberEligibilityService.getMtdStatus(testVatNumber)) shouldBe Ineligible
        }
      }

      "the user is deregistered" should {
        s"return $Deregistered" in {
          mockGetMandationStatus(testVatNumber)(Future.successful(Left(VatNumberNotFound)))
          mockGetEligibilityStatus(testVatNumber)(Future.successful(Left(ControlListEligibilityService.Deregistered)))

          await(TestVatNumberEligibilityService.getMtdStatus(testVatNumber)) shouldBe Deregistered
        }
      }

      "the vat number is not found" should {
        s"return $VatNumberNotFound" in {
          mockGetMandationStatus(testVatNumber)(Future.successful(Left(VatNumberNotFound)))
          mockGetEligibilityStatus(testVatNumber)(Future.successful(Left(ControlListEligibilityService.VatNumberNotFound)))

          await(TestVatNumberEligibilityService.getMtdStatus(testVatNumber)) shouldBe VatNumberEligibilityService.VatNumberNotFound
        }
      }

      "the user's eligibility cannot be determined" should {
        "throw an InternalServerException detailing the error" in {
          mockGetMandationStatus(testVatNumber)(Future.successful(Left(VatNumberNotFound)))
          mockGetEligibilityStatus(testVatNumber)(Future.successful(Left(KnownFactsAndControlListFailure)))

          intercept[InternalServerException](await(TestVatNumberEligibilityService.getMtdStatus(testVatNumber)))
        }
      }
    }

    "the user is currently being migrated" should {
      s"return $MigrationInProgress" in {
        mockGetMandationStatus(testVatNumber)(Future.successful(Left(GetMandationStatusHttpParser.MigrationInProgress)))

        await(TestVatNumberEligibilityService.getMtdStatus(testVatNumber)) shouldBe MigrationInProgress
      }
    }

    "the user's migration status cannot be determined" should {
      "throw an InternalServerException detailing the error" in {
        mockGetMandationStatus(testVatNumber)(Future.successful(Left(GetMandationStatusHttpFailure(BAD_REQUEST, ""))))

        intercept[InternalServerException](await(TestVatNumberEligibilityService.getMtdStatus(testVatNumber)))
      }
    }

  }
}
