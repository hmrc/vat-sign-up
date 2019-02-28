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

import play.api.http.Status.BAD_GATEWAY
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.vatsignup.config.featureswitch.{AdditionalKnownFacts, FeatureSwitching}
import uk.gov.hmrc.vatsignup.connectors.mocks.MockKnownFactsAndControlListInformationConnector
import uk.gov.hmrc.vatsignup.helpers.TestConstants._
import uk.gov.hmrc.vatsignup.httpparsers.KnownFactsAndControlListInformationHttpParser.{ControlListInformationVatNumberNotFound, KnownFactsInvalidVatNumber, UnexpectedKnownFactsAndControlListInformationFailure}
import uk.gov.hmrc.vatsignup.services.KnownFactsMatchingService
import uk.gov.hmrc.vatsignup.services.KnownFactsMatchingService.{InvalidVatNumber, KnownFactsDoNotMatch, KnownFactsMatch, UnexpectedError, VatNumberNotFound}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class MatchKnownFactsServiceSpec
  extends UnitSpec
    with MockKnownFactsAndControlListInformationConnector
    with FeatureSwitching {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  object TestKnownFactsMatchingService extends KnownFactsMatchingService (
    mockKnownFactsAndControlListInformationConnector
  )

  override def beforeEach(): Unit = {
    enable(AdditionalKnownFacts)
  }

  "the feature switch is enabled" when {
    "4 valid known facts are provided" should {
      "return KnownFactsMatch" in {
        mockGetKnownFactsAndControlListInformation(testVatNumber)(Future.successful(Right(testKnownFactsAndControlListInformation)))
        val res = await(TestKnownFactsMatchingService.checkVatKnownFactsMatch(
          vatNumber = testVatNumber,
          vatRegistrationDate = testKnownFactsAndControlListInformation.vatKnownFacts.vatRegistrationDate,
          businessPostcode = testKnownFactsAndControlListInformation.vatKnownFacts.businessPostcode,
          lastNetDue = testKnownFactsAndControlListInformation.vatKnownFacts.lastNetDue,
          lastReturnMonthPeriod = testKnownFactsAndControlListInformation.vatKnownFacts.lastReturnMonthPeriod
        ))
        res shouldBe Right(KnownFactsMatch)
      }
    }
    "4 invalid known facts are provided" should {
      "return KnownFactsDoNotMatch" in {
        mockGetKnownFactsAndControlListInformation(testVatNumber)(Future.successful(Right(testKnownFactsAndControlListInformation)))
        val res = await(TestKnownFactsMatchingService.checkVatKnownFactsMatch(
          vatNumber = testVatNumber,
          vatRegistrationDate = testKnownFactsAndControlListInformation.vatKnownFacts.vatRegistrationDate,
          businessPostcode = testKnownFactsAndControlListInformation.vatKnownFacts.businessPostcode,
          lastNetDue = Some("12345.50d"),
          lastReturnMonthPeriod = testKnownFactsAndControlListInformation.vatKnownFacts.lastReturnMonthPeriod
        ))
        res shouldBe Left(KnownFactsDoNotMatch)
      }
    }
  }

  "the feature switch is disabled" when {
    "2 valid known facts are provided" should {
      "return KnownFactsMatch" in {
        disable(AdditionalKnownFacts)
        mockGetKnownFactsAndControlListInformation(testVatNumber)(Future.successful(Right(testKnownFactsAndControlListInformation)))
        val res = await(TestKnownFactsMatchingService.checkVatKnownFactsMatch(
          vatNumber = testVatNumber,
          vatRegistrationDate = testKnownFactsAndControlListInformation.vatKnownFacts.vatRegistrationDate,
          businessPostcode = testKnownFactsAndControlListInformation.vatKnownFacts.businessPostcode,
          lastNetDue = None,
          lastReturnMonthPeriod = None
        ))
        res shouldBe Right(KnownFactsMatch)
      }
    }
    "2 invalid known facts are provided" should {
      "return a KnownFactsDoNotMatch" in {
        disable(AdditionalKnownFacts)
        mockGetKnownFactsAndControlListInformation(testVatNumber)(Future.successful(Right(testKnownFactsAndControlListInformation)))
        val res = await(TestKnownFactsMatchingService.checkVatKnownFactsMatch(
          vatNumber = testVatNumber,
          vatRegistrationDate = testKnownFactsAndControlListInformation.vatKnownFacts.vatRegistrationDate,
          businessPostcode = "ST11 1ZZ",
          lastNetDue = None,
          lastReturnMonthPeriod = None
        ))
        res shouldBe Left(KnownFactsDoNotMatch)
      }
    }
  }

  "when an invalid VAT number is specified" should {
    "return InvalidVatNumber" in {
      mockGetKnownFactsAndControlListInformation("123456789")(Future.successful(Left(KnownFactsInvalidVatNumber)))
      val res = await(TestKnownFactsMatchingService.checkVatKnownFactsMatch(
        vatNumber = "123456789",
        vatRegistrationDate = testKnownFactsAndControlListInformation.vatKnownFacts.vatRegistrationDate,
        businessPostcode = testKnownFactsAndControlListInformation.vatKnownFacts.businessPostcode,
        lastNetDue = testKnownFactsAndControlListInformation.vatKnownFacts.lastNetDue,
        lastReturnMonthPeriod = testKnownFactsAndControlListInformation.vatKnownFacts.lastReturnMonthPeriod
      ))
      res shouldBe Left(InvalidVatNumber)
    }
  }

  "when the VAT number cannot be found" should {
    "return VatNumberNotFound" in {
      mockGetKnownFactsAndControlListInformation(testVatNumber)(Future.successful(Left(ControlListInformationVatNumberNotFound)))

      val res = await(TestKnownFactsMatchingService.checkVatKnownFactsMatch(
        vatNumber = testVatNumber,
        vatRegistrationDate = testKnownFactsAndControlListInformation.vatKnownFacts.vatRegistrationDate,
        businessPostcode = testKnownFactsAndControlListInformation.vatKnownFacts.businessPostcode,
        lastNetDue = testKnownFactsAndControlListInformation.vatKnownFacts.lastNetDue,
        lastReturnMonthPeriod = testKnownFactsAndControlListInformation.vatKnownFacts.lastReturnMonthPeriod
      ))
      res shouldBe Left(VatNumberNotFound)
    }
  }

  "when an unexpected error is received" should {
    "return UnexpectedError" in {
      mockGetKnownFactsAndControlListInformation(testVatNumber)(Future.successful(
        Left(UnexpectedKnownFactsAndControlListInformationFailure(BAD_GATEWAY, "Unexpected error"))
      ))

      val res = await(TestKnownFactsMatchingService.checkVatKnownFactsMatch(
        vatNumber = testVatNumber,
        vatRegistrationDate = testKnownFactsAndControlListInformation.vatKnownFacts.vatRegistrationDate,
        businessPostcode = testKnownFactsAndControlListInformation.vatKnownFacts.businessPostcode,
        lastNetDue = testKnownFactsAndControlListInformation.vatKnownFacts.lastNetDue,
        lastReturnMonthPeriod = testKnownFactsAndControlListInformation.vatKnownFacts.lastReturnMonthPeriod
      ))
      res shouldBe Left(UnexpectedError(BAD_GATEWAY, "Unexpected error"))
    }
  }

}
