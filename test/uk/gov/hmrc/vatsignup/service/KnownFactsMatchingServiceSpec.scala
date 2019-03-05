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
import uk.gov.hmrc.vatsignup.config.featureswitch.{AdditionalKnownFacts, FeatureSwitching}
import uk.gov.hmrc.vatsignup.helpers.TestConstants._
import uk.gov.hmrc.vatsignup.models.VatKnownFacts
import uk.gov.hmrc.vatsignup.services.KnownFactsMatchingService
import uk.gov.hmrc.vatsignup.services.KnownFactsMatchingService._

class KnownFactsMatchingServiceSpec extends UnitSpec with FeatureSwitching {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  object TestKnownFactsMatchingService extends KnownFactsMatchingService()

  val testEnteredFourKnownFacts = VatKnownFacts(
    businessPostcode = (testPostCode filterNot (_.isWhitespace)).toLowerCase(),
    vatRegistrationDate = testDateOfRegistration,
    lastReturnMonthPeriod = Some(Month.MARCH),
    lastNetDue = Some(testLastNetDue)
  )

  "the feature switch is enabled" when {
    "4 valid known facts are provided" should {
      "return FourKnownFactsMatch" in {
        enable(AdditionalKnownFacts)
        val res = await(TestKnownFactsMatchingService.checkKnownFactsMatch(
          enteredKfs = testEnteredFourKnownFacts,
          retrievedKfs = testFourKnownFacts
        ))
        res shouldBe Right(FourKnownFactsMatch)
      }
    }
    "4 invalid known facts are provided" should {
      "return KnownFactsDoNotMatch" in {
        val testInvalidKnownFacts = VatKnownFacts(
          businessPostcode = "",
          vatRegistrationDate = "",
          lastReturnMonthPeriod = Some(Month.MARCH),
          lastNetDue = Some("")
        )

        val res = await(TestKnownFactsMatchingService.checkKnownFactsMatch(
          enteredKfs = testInvalidKnownFacts,
          retrievedKfs = testFourKnownFacts
        ))
        res shouldBe Left(KnownFactsDoNotMatch)
      }
    }
    "2 valid known facts are provided" should {
      "return KnownFactsDoNotMatch" in {
        val res = await(TestKnownFactsMatchingService.checkKnownFactsMatch(
          enteredKfs = testTwoKnownFacts,
          retrievedKfs = testFourKnownFacts
        ))
        res shouldBe Left(KnownFactsDoNotMatch)
      }
    }
  }

  "the feature switch is disabled" when {
    "2 valid known facts are provided" should {
      "return TwoKnownFactsMatch" in {
        disable(AdditionalKnownFacts)
        val res = await(TestKnownFactsMatchingService.checkKnownFactsMatch(
          enteredKfs = testTwoKnownFacts,
          retrievedKfs = testTwoKnownFacts
        ))
        res shouldBe Right(TwoKnownFactsMatch)
      }
    }
    "2 invalid known facts are provided" should {
      "return a KnownFactsDoNotMatch" in {
        disable(AdditionalKnownFacts)

        val testInvalidKnownFacts = VatKnownFacts(
          businessPostcode = "",
          vatRegistrationDate = "",
          lastReturnMonthPeriod = None,
          lastNetDue = None
        )

        val res = await(TestKnownFactsMatchingService.checkKnownFactsMatch(
          enteredKfs = testInvalidKnownFacts,
          retrievedKfs = testTwoKnownFacts
        ))
        res shouldBe Left(KnownFactsDoNotMatch)
      }
    }
  }

}
