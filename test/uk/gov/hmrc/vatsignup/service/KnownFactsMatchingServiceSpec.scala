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

import java.time.Month

import uk.gov.hmrc.http.HeaderCarrier
import org.scalatest.{Matchers, WordSpec}
import play.api.test.Helpers._
import play.api.test.FakeRequest
import uk.gov.hmrc.vatsignup.config.featureswitch.{AdditionalKnownFacts, FeatureSwitching}
import uk.gov.hmrc.vatsignup.helpers.TestConstants._
import uk.gov.hmrc.vatsignup.models.VatKnownFacts
import uk.gov.hmrc.vatsignup.models.monitoring.KnownFactsAuditing.KnownFactsAuditModel
import uk.gov.hmrc.vatsignup.service.mocks.monitoring.MockAuditService
import uk.gov.hmrc.vatsignup.services.KnownFactsMatchingService
import uk.gov.hmrc.vatsignup.services.KnownFactsMatchingService._

import scala.concurrent.ExecutionContext.Implicits.global

class KnownFactsMatchingServiceSpec extends WordSpec with Matchers with FeatureSwitching
 with MockAuditService {

  implicit val hc: HeaderCarrier = HeaderCarrier()
  implicit val request = FakeRequest()

  object TestKnownFactsMatchingService extends KnownFactsMatchingService(mockAuditService)

  val testEnteredFourKnownFacts = VatKnownFacts(
    businessPostcode = Some((testPostCode filterNot (_.isWhitespace)).toLowerCase()),
    vatRegistrationDate = testDateOfRegistration,
    lastReturnMonthPeriod = Some(Month.MARCH),
    lastNetDue = Some(testLastNetDue)
  )

  val testLastNetDueNegative = (testLastNetDue.toDouble * -1).toString

  val testEnteredKnownFactsNegativeBox5 = VatKnownFacts(
    businessPostcode = Some(testPostCode),
    vatRegistrationDate = testDateOfRegistration,
    lastReturnMonthPeriod = Some(Month.MARCH),
    lastNetDue = Some(testLastNetDueNegative)
  )

  "the feature switch is enabled" when {
    "4 valid known facts are provided" should {
      "return KnownFactsMatch" in {
        enable(AdditionalKnownFacts)

        val res = TestKnownFactsMatchingService.checkKnownFactsMatch(
          vatNumber = testVatNumber,
          enteredKfs = testEnteredFourKnownFacts,
          retrievedKfs = testFourKnownFacts,
          isOverseas = false
        )

        verifyAudit(KnownFactsAuditModel(
          testVatNumber,
          testEnteredFourKnownFacts,
          testFourKnownFacts,
          matched = true
        ))

        res shouldBe Right(KnownFactsMatch)
      }
    }
    "4 valid known facts are provided, but the Box 5 figure value sent from the front end is negative" should {
      "return KnownFactsMatch" in {
        enable(AdditionalKnownFacts)
        val res = TestKnownFactsMatchingService.checkKnownFactsMatch(
          vatNumber = testVatNumber,
          enteredKfs = testEnteredKnownFactsNegativeBox5,
          retrievedKfs = testFourKnownFacts,
          isOverseas = false
        )

        verifyAudit(KnownFactsAuditModel(
          testVatNumber,
          testEnteredKnownFactsNegativeBox5,
          testFourKnownFacts,
          matched = true
        ))

        res shouldBe Right(KnownFactsMatch)
      }
    }
    "4 invalid known facts are provided" should {
      "return KnownFactsMismatch" in {
        val testInvalidKnownFacts = VatKnownFacts(
          businessPostcode = Some(""),
          vatRegistrationDate = "",
          lastReturnMonthPeriod = Some(Month.MARCH),
          lastNetDue = Some("")
        )

        val res = TestKnownFactsMatchingService.checkKnownFactsMatch(
          vatNumber = testVatNumber,
          enteredKfs = testInvalidKnownFacts,
          retrievedKfs = testFourKnownFacts,
          isOverseas = false
        )

        verifyAudit(KnownFactsAuditModel(
          testVatNumber,
          testInvalidKnownFacts,
          testFourKnownFacts,
          matched = false
        ))

        res shouldBe Left(KnownFactsMismatch)
      }
    }
    "4 invalid known facts are provided and the Box 5 figure value sent from the front end is negative" should {
      "return KnownFactsMismatch" in {
        val testInvalidKnownFacts = VatKnownFacts(
          businessPostcode = Some(""),
          vatRegistrationDate = "",
          lastReturnMonthPeriod = Some(Month.MARCH),
          lastNetDue = Some("-12345.01")
        )

        val res = TestKnownFactsMatchingService.checkKnownFactsMatch(
          vatNumber = testVatNumber,
          enteredKfs = testInvalidKnownFacts,
          retrievedKfs = testFourKnownFacts,
          isOverseas = false
        )

        verifyAudit(KnownFactsAuditModel(
          testVatNumber,
          testInvalidKnownFacts,
          testFourKnownFacts,
          matched = false
        ))

        res shouldBe Left(KnownFactsMismatch)
      }
    }
    "2 valid known facts are provided" should {
      "return KnownFactsMismatch" in {
        val res = TestKnownFactsMatchingService.checkKnownFactsMatch(
          vatNumber = testVatNumber,
          enteredKfs = testTwoKnownFacts,
          retrievedKfs = testFourKnownFacts,
          isOverseas = false
        )

        verifyAudit(KnownFactsAuditModel(
          testVatNumber,
          testTwoKnownFacts,
          testFourKnownFacts,
          matched = false
        ))

        res shouldBe Left(KnownFactsMismatch)
      }
    }
    "is overseas and postcode isn't provided" should {
      "return KnownFactsMatch" in {
        enable(AdditionalKnownFacts)

        val res = TestKnownFactsMatchingService.checkKnownFactsMatch(
          vatNumber = testVatNumber,
          enteredKfs = testEnteredFourKnownFacts.copy(businessPostcode = None),
          retrievedKfs = testFourKnownFacts,
          isOverseas = true
        )

        verifyAudit(KnownFactsAuditModel(
          testVatNumber,
          testEnteredFourKnownFacts.copy(businessPostcode = None),
          testFourKnownFacts,
          matched = true
        ))

        res shouldBe Right(KnownFactsMatch)
      }
    }
  }
  "the feature switch is disabled" when {
    "2 valid known facts are provided" should {
      "return KnownFactsMatch" in {
        disable(AdditionalKnownFacts)

        val res = TestKnownFactsMatchingService.checkKnownFactsMatch(
          vatNumber = testVatNumber,
          enteredKfs = testTwoKnownFacts,
          retrievedKfs = testTwoKnownFacts,
          isOverseas = false
        )

        verifyAudit(KnownFactsAuditModel(
          testVatNumber,
          testTwoKnownFacts,
          testTwoKnownFacts,
          matched = true
        ))

        res shouldBe Right(KnownFactsMatch)
      }
    }
    "2 invalid known facts are provided" should {
      "return a KnownFactsMismatch" in {
        disable(AdditionalKnownFacts)

        val testEnteredKnownFacts = VatKnownFacts(
          businessPostcode = Some(""),
          vatRegistrationDate = "",
          lastReturnMonthPeriod = None,
          lastNetDue = None
        )

        val res = TestKnownFactsMatchingService.checkKnownFactsMatch(
          vatNumber = testVatNumber,
          enteredKfs = testEnteredKnownFacts,
          retrievedKfs = testTwoKnownFacts,
          isOverseas = false
        )

        verifyAudit(KnownFactsAuditModel(
          testVatNumber,
          testEnteredKnownFacts,
          testTwoKnownFacts,
          matched = false
        ))

        res shouldBe Left(KnownFactsMismatch)
      }
    }
  }

}
