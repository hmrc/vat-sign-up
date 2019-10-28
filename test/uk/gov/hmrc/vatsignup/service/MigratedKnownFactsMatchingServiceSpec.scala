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

import java.time.Month

import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.vatsignup.connectors.mocks.MockKnownFactsAndControlListInformationConnector
import uk.gov.hmrc.vatsignup.services.MigratedKnownFactsMatchingService
import uk.gov.hmrc.vatsignup.helpers.TestConstants._
import uk.gov.hmrc.vatsignup.httpparsers.KnownFactsAndControlListInformationHttpParser.KnownFactsInvalidVatNumber
import uk.gov.hmrc.vatsignup.models.controllist.{ControlListInformation, Stagger1}
import uk.gov.hmrc.vatsignup.models.{KnownFactsAndControlListInformation, VatKnownFacts}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class MigratedKnownFactsMatchingServiceSpec extends UnitSpec with MockKnownFactsAndControlListInformationConnector {

  object TestMigratedKnownFactsMatchingService extends MigratedKnownFactsMatchingService(
    mockKnownFactsAndControlListInformationConnector
  )

  implicit val hc = HeaderCarrier()

  private val testControlListInfo = ControlListInformation(Set.empty, Stagger1)
  private val test2KnownFacts = VatKnownFacts(Some(testPostCode), testDateOfRegistration, None, None)
  private val test4KnownFacts = VatKnownFacts(Some(testPostCode), testDateOfRegistration, Some(Month.MARCH), Some(testLastNetDue))

  "checkKnownFacts" when {
    "DES returns a response" when {
      "4 known facts are returned" when {
        "the returned known facts match the entered known facts" should {
          "return true" in {
            val apiResponse = KnownFactsAndControlListInformation(test4KnownFacts, testControlListInfo)
            mockGetKnownFactsAndControlListInformation(testVatNumber)(Future.successful(Right(apiResponse)))

            val res = await(TestMigratedKnownFactsMatchingService.checkKnownFactsMatch(testVatNumber, test4KnownFacts))

            res shouldBe true
          }
        }
        "the returned known facts don't match the entered known facts" should {
          "return false" in {
            val apiResponse = KnownFactsAndControlListInformation(test4KnownFacts, testControlListInfo)
            mockGetKnownFactsAndControlListInformation(testVatNumber)(Future.successful(Right(apiResponse)))

            val res = await(TestMigratedKnownFactsMatchingService.checkKnownFactsMatch(testVatNumber, test4KnownFacts.copy(businessPostcode = Some("1234"))))

            res shouldBe false
          }
        }
      }
      "2 known facts are returned" when {
        "the returned known facts match the entered known facts" should {
          "return true" in {
            val apiResponse = KnownFactsAndControlListInformation(test2KnownFacts, testControlListInfo)
            mockGetKnownFactsAndControlListInformation(testVatNumber)(Future.successful(Right(apiResponse)))

            val res = await(TestMigratedKnownFactsMatchingService.checkKnownFactsMatch(testVatNumber, test2KnownFacts))

            res shouldBe true
          }
        }
        "the returned known facts don't match the entered known facts" should {
          "return false" in {
            val apiResponse = KnownFactsAndControlListInformation(test2KnownFacts, testControlListInfo)
            mockGetKnownFactsAndControlListInformation(testVatNumber)(Future.successful(Right(apiResponse)))

            val res = await(TestMigratedKnownFactsMatchingService.checkKnownFactsMatch(testVatNumber, test2KnownFacts.copy(businessPostcode = Some("1234"))))

            res shouldBe false
          }
        }
      }
      "DES returns no known facts for the VAT number" should {
        "return false" in {
          mockGetKnownFactsAndControlListInformation(testVatNumber)(Future.successful(Left(KnownFactsInvalidVatNumber)))

          val res = await(TestMigratedKnownFactsMatchingService.checkKnownFactsMatch(testVatNumber, test2KnownFacts))

          res shouldBe false
        }
      }
    }
  }
}
