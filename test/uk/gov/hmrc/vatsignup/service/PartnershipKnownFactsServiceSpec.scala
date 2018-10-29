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

import play.api.http.Status.BAD_REQUEST
import play.api.mvc.Request
import play.api.test.FakeRequest
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.vatsignup.connectors.mocks.MockPartnershipKnownFactsConnector
import uk.gov.hmrc.vatsignup.helpers.TestConstants._
import uk.gov.hmrc.vatsignup.httpparsers.GetPartnershipKnownFactsHttpParser.{PartnershipKnownFactsNotFound, UnexpectedGetPartnershipKnownFactsFailure}
import uk.gov.hmrc.vatsignup.models.PartnershipKnownFacts
import uk.gov.hmrc.vatsignup.services.PartnershipKnownFactsService
import uk.gov.hmrc.vatsignup.services.PartnershipKnownFactsService._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class PartnershipKnownFactsServiceSpec extends UnitSpec with MockPartnershipKnownFactsConnector {

  object TestPartnershipKnownFactsService extends PartnershipKnownFactsService(
    mockPartnershipKnownFactsConnector
  )

  implicit val hc: HeaderCarrier = HeaderCarrier()
  implicit val request: Request[_] = FakeRequest()

  "getPartnershipKnownFacts" should {
    "return the known facts when at least one post code from DES matches" in {
      val testPostCodeWithSpace = "AA1 1AA"
      mockGetKnownFacts(saUtr = testUtr)(Future.successful(Right(PartnershipKnownFacts(
        postCode = Some(testPostCodeWithSpace),
        correspondencePostCode = None,
        basePostCode = Some(testPostCode),
        commsPostCode = Some(testPostCodeWithSpace),
        traderPostCode = None
      ))))

      val res = await(TestPartnershipKnownFactsService.checkKnownFactsMatch(testUtr, testPostCode))

      res shouldBe Right(PartnershipPostCodeMatched)
    }
    "return known facts when postcode has spaces or uncapitalized letters but matches a postcode from DES" in {
      val testPostCode = "aa11aa"
      val testCapPostCode = "AA1 1AA"
      mockGetKnownFacts(saUtr = testUtr)(Future.successful(Right(PartnershipKnownFacts(
        postCode = Some(testCapPostCode),
        correspondencePostCode = None,
        basePostCode = None,
        commsPostCode = None,
        traderPostCode = None
      ))))

      val res = await(TestPartnershipKnownFactsService.checkKnownFactsMatch(testUtr, testPostCode))

      res shouldBe Right(PartnershipPostCodeMatched)
    }
    "return PostCodeDoesNotMatch when we no postcode from the KFs matches" in {
      val testUnmatchedPostCode = "AA1 1AA"
      mockGetKnownFacts(saUtr = testUtr)(Future.successful(Right(PartnershipKnownFacts(
        postCode = Some(testUnmatchedPostCode),
        correspondencePostCode = None,
        basePostCode = Some(testUnmatchedPostCode),
        commsPostCode = Some(testUnmatchedPostCode),
        traderPostCode = None
      ))))

      val res = await(TestPartnershipKnownFactsService.checkKnownFactsMatch(testUtr, testPostCode))

      res shouldBe Left(PostCodeDoesNotMatch)
    }
    "return NoPostCodesReturned when there are no postcodes in the KFs" in {
      val testUnmatchedPostCode = "AA1 1AA"
      mockGetKnownFacts(saUtr = testUtr)(Future.successful(Right(PartnershipKnownFacts(
        postCode = None,
        correspondencePostCode = None,
        basePostCode = None,
        commsPostCode = None,
        traderPostCode = None
      ))))

      val res = await(TestPartnershipKnownFactsService.checkKnownFactsMatch(testUtr, testPostCode))

      res shouldBe Left(NoPostCodesReturned)
    }
    "return InvalidSautr when sautr doesn't exist in DES" in {
      val testUnmatchedPostCode = "AA1 1AA"
      mockGetKnownFacts(saUtr = testUtr)(Future.successful(Left(PartnershipKnownFactsNotFound)))

      val res = await(TestPartnershipKnownFactsService.checkKnownFactsMatch(testUtr, testPostCode))

      res shouldBe Left(InvalidSautr)
    }

    "return GetPartnershipKnownFactsFailure for any other failure" in {
      mockGetKnownFacts(saUtr = testUtr)(Future.successful(
        Left(UnexpectedGetPartnershipKnownFactsFailure(BAD_REQUEST, ""))
      ))

      val res = await(TestPartnershipKnownFactsService.checkKnownFactsMatch(testUtr, testPostCode))

      res shouldBe Left(GetPartnershipKnownFactsFailure(BAD_REQUEST, ""))
    }

  }

}
