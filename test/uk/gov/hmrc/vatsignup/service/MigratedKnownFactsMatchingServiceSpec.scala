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
import uk.gov.hmrc.http.HeaderCarrier
import org.scalatest.{WordSpec, Matchers}
import uk.gov.hmrc.vatsignup.connectors.mocks.MockVatCustomerDetailsConnector
import uk.gov.hmrc.vatsignup.helpers.TestConstants._
import uk.gov.hmrc.vatsignup.httpparsers.VatCustomerDetailsHttpParser.VatNumberNotFound
import uk.gov.hmrc.vatsignup.models._
import uk.gov.hmrc.vatsignup.services.MigratedKnownFactsMatchingService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class MigratedKnownFactsMatchingServiceSpec extends WordSpec with Matchers with MockVatCustomerDetailsConnector {

  object TestMigratedKnownFactsMatchingService extends MigratedKnownFactsMatchingService(
    mockVatCustomerDetailsConnector
  )

  implicit val hc: HeaderCarrier = HeaderCarrier()

  private val testEnteredKnownFacts = VatKnownFacts(Some(testPostCode), testDateOfRegistration, None, None)
  private val testEnteredOverseasKnownFacts = VatKnownFacts(None, testDateOfRegistration, None, None)
  private val testRetrievedKnownFacts = KnownFacts(Some(testPostCode), testDateOfRegistration)
  private val testVatCustomerDetails = VatCustomerDetails(testRetrievedKnownFacts, isOverseas = false)
  private val testOverseasVatCustomerDetails = VatCustomerDetails(testRetrievedKnownFacts, isOverseas = true)

  "checkKnownFacts" when {
    "DES returns a response" when {
      "2 known facts are returned" when {
        "the returned known facts match the entered known facts" should {
          "return true" in {
            mockGetVatCustomerDetails(testVatNumber)(Future.successful(Right(testVatCustomerDetails)))

            val res = await(TestMigratedKnownFactsMatchingService.checkKnownFactsMatch(
              testVatNumber, testEnteredKnownFacts
            ))

            res shouldBe true
          }
        }
        "the returned known facts match the entered known facts after lowercasing them and removing spacing" should {
          "return true" in {
            mockGetVatCustomerDetails(testVatNumber)(Future.successful(Right(testVatCustomerDetails)))

            val res = await(TestMigratedKnownFactsMatchingService.checkKnownFactsMatch(
              testVatNumber, testEnteredKnownFacts.copy(businessPostcode = Some("zz111zz"))
            ))

            res shouldBe true
          }
        }
        "the returned reg date matches the entered reg date for an overseas VRN" should {
          "return true" in {
            mockGetVatCustomerDetails(testVatNumber)(Future.successful(Right(testOverseasVatCustomerDetails)))

            val res = await(TestMigratedKnownFactsMatchingService.checkKnownFactsMatch(
              testVatNumber, testEnteredOverseasKnownFacts
            ))

            res shouldBe true
          }
        }
        "the returned known facts don't match the entered known facts" should {
          "return false" in {
            mockGetVatCustomerDetails(testVatNumber)(Future.successful(Right(testVatCustomerDetails)))

            val res = await(TestMigratedKnownFactsMatchingService.checkKnownFactsMatch(
              testVatNumber, testEnteredKnownFacts.copy(businessPostcode = Some("1234"))
            ))

            res shouldBe false
          }
        }
      }
      "DES returns no known facts for the VAT number" should {
        "return false" in {
          mockGetVatCustomerDetails(testVatNumber)(Future.successful(Left(VatNumberNotFound)))

          val res = await(TestMigratedKnownFactsMatchingService.checkKnownFactsMatch(
            testVatNumber, testEnteredKnownFacts
          ))

          res shouldBe false
        }
      }
    }
  }
}
