/*
 * Copyright 2019 HM Revenue & Customs
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

import play.api.mvc.Request
import play.api.test.FakeRequest
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.vatsignup.config.featureswitch.AdditionalKnownFacts
import uk.gov.hmrc.vatsignup.config.mocks.MockConfig
import uk.gov.hmrc.vatsignup.connectors.mocks.MockKnownFactsAndControlListInformationConnector
import uk.gov.hmrc.vatsignup.helpers.TestConstants._
import uk.gov.hmrc.vatsignup.models.VatKnownFacts
import uk.gov.hmrc.vatsignup.repositories.mocks.MockSubscriptionRequestRepository
import uk.gov.hmrc.vatsignup.services.KnownFactsAndControllistInformationService
import uk.gov.hmrc.vatsignup.services.KnownFactsAndControllistInformationService.KnownFactsMatchingFailure

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class KnownFactsAndControllistServiceSpec extends UnitSpec with MockKnownFactsAndControlListInformationConnector
  with MockSubscriptionRequestRepository with MockConfig {

  object TestKnownFactsAndControllistService extends KnownFactsAndControllistInformationService(
    mockConfig,
    mockKnownFactsAndControlListInformationConnector
  )

  implicit val hc: HeaderCarrier = HeaderCarrier()
  implicit val request: Request[_] = FakeRequest()


  "getKnownFactsAndControllist" when {
    "we have been sent 3 known facts" when {
      val knownFacts = VatKnownFacts(
        businessPostcode = testBasePostCode,
        vatRegistrationDate = testDateOfRegistration,
        lastReturnMonthPeriod = None,
        lastNetDue = None,
        controlListInformation = testControlListInformation
      )
      "Additional KF fs is enabled" when {
        "known facts match and additional known facts are unavailable" should {
          "return VatKnownFacts" in {
            enable(AdditionalKnownFacts)
            mockGetKnownFactsAndControlListInformation(testVatNumber)(Future.successful(Right(knownFacts)))

            val res = TestKnownFactsAndControllistService.getKnownFactsAndControlListInformation(testVatNumber,
              businessPostcode = testBasePostCode,
              vatRegistrationDate = testDateOfRegistration,
              lastReturnMonthPeriod = None,
              lastNetDue = None
            )

            await(res) shouldBe Right(knownFacts)
          }
        }

        "known facts match but additional known facts are available" should {
          "return KnownFactsMatchingFailure" in {
            enable(AdditionalKnownFacts)
            mockGetKnownFactsAndControlListInformation(testVatNumber)(Future.successful(Right(knownFacts.copy(
              lastReturnMonthPeriod = Some(testLastReturnMonthPeriod),
              lastNetDue = Some(testLastNetDue)
            ))))

            val res = TestKnownFactsAndControllistService.getKnownFactsAndControlListInformation(testVatNumber,
              businessPostcode = testBasePostCode,
              vatRegistrationDate = testDateOfRegistration,
              lastReturnMonthPeriod = None,
              lastNetDue = None
            )

            await(res) shouldBe Left(KnownFactsMatchingFailure)
          }
        }

        "known facts don't match" should {
          "return KnownFactsMatchingFailure" in {
            enable(AdditionalKnownFacts)
            mockGetKnownFactsAndControlListInformation(testVatNumber)(
              Future.successful(Right(knownFacts.copy(lastNetDue = Some("0.12"))))
            )

            val res = TestKnownFactsAndControllistService.getKnownFactsAndControlListInformation(
              testVatNumber,
              businessPostcode = testBasePostCode,
              vatRegistrationDate = testDateOfRegistration,
              lastReturnMonthPeriod = Some(testLastReturnMonthPeriod),
              lastNetDue = Some(testLastNetDue)
            )
            await(res) shouldBe Left(KnownFactsMatchingFailure)
          }

        }
      }

      "Additional KF fs is disabled" when {
        "known facts match" should {
          "return VatKnownFacts" in {
            disable(AdditionalKnownFacts)
            mockGetKnownFactsAndControlListInformation(testVatNumber)(Future.successful(Right(knownFacts)))
            val res = TestKnownFactsAndControllistService.getKnownFactsAndControlListInformation(
              testVatNumber,
              businessPostcode = testBasePostCode,
              vatRegistrationDate = testDateOfRegistration,
              lastReturnMonthPeriod = None,
              lastNetDue = None
            )
            await(res) shouldBe Right(knownFacts)
          }
        }

        "known facts don't match" should {
          "return KnownFactsMatchingFailure" in {
            disable(AdditionalKnownFacts)
            mockGetKnownFactsAndControlListInformation(testVatNumber)(Future.successful(Right(knownFacts)))
            val res = TestKnownFactsAndControllistService.getKnownFactsAndControlListInformation(
              testVatNumber,
              businessPostcode = testBasePostCode,
              vatRegistrationDate = "2017-02-01",
              lastReturnMonthPeriod = None,
              lastNetDue = None
            )
            await(res) shouldBe Left(KnownFactsMatchingFailure)

          }
        }
      }
    }
    "we have been sent 5 known facts" when {
      val knownFacts = VatKnownFacts(
        businessPostcode = testBasePostCode,
        vatRegistrationDate = testDateOfRegistration,
        lastReturnMonthPeriod = Some(testLastReturnMonthPeriod),
        lastNetDue = Some(testLastNetDue),
        controlListInformation = testControlListInformation
      )
      "known facts match" should {
        "return VatKnownFacts" in {
          mockGetKnownFactsAndControlListInformation(testVatNumber)(Future.successful(Right(knownFacts)))

          val res = TestKnownFactsAndControllistService.getKnownFactsAndControlListInformation(
              vatNumber = testVatNumber,
              businessPostcode = testBasePostCode,
              vatRegistrationDate = testDateOfRegistration,
              lastReturnMonthPeriod = Some(testLastReturnMonthPeriod),
              lastNetDue = Some(testLastNetDue)
            )

          await(res) shouldBe Right(knownFacts)
        }
      }

      "known facts don't match" should {
        "return KnownFactsMatchingFailure" in {

          mockGetKnownFactsAndControlListInformation(testVatNumber)(
            Future.successful(Right(knownFacts.copy(lastNetDue = Some("error"))))
          )

          val res = TestKnownFactsAndControllistService.getKnownFactsAndControlListInformation(
            testVatNumber,
              businessPostcode = testBasePostCode,
              vatRegistrationDate = testDateOfRegistration,
              lastReturnMonthPeriod = Some(testLastReturnMonthPeriod),
              lastNetDue = Some(testLastNetDue)
            )
          await(res) shouldBe Left(KnownFactsMatchingFailure)
        }

      }

    }
  }
}
