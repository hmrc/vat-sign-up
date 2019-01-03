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

package uk.gov.hmrc.vatsignup.httpparsers

import org.scalatest.EitherValues
import play.api.http.Status._
import play.api.libs.json.Json
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.vatsignup.helpers.TestConstants._
import uk.gov.hmrc.vatsignup.httpparsers.GetPartnershipKnownFactsHttpParser._
import uk.gov.hmrc.vatsignup.models.PartnershipKnownFacts

class GetPartnershipKnownFactsHttpParserSpec extends UnitSpec with EitherValues {
  "GetIdentityVerificationOutcome#read" when {
    def read(httpResponse: HttpResponse): GetPartnershipKnownFactsResponse = GetPartnershipKnownFactsHttpReads.read("GET", "", httpResponse)

    "the response status is OK" when {
      "the json is valid and contains all postcodes" should {
        "return a successful list of postcodes" in {
          read(HttpResponse(
            responseStatus = OK,
            responseJson = Some(Json.obj(
              postCodeKey -> testPostCode,
              correspondenceDetailsKey -> Json.obj(
                correspondencePostCodeKey -> testCorrespondencePostCode
              ),
              basePostCodeKey -> testBasePostCode,
              commsPostCodeKey -> testCommsPostCode,
              traderPostCodeKey -> testTraderPostCode
            ))
          )) shouldBe Right(PartnershipKnownFacts(
            postCode = Some(testPostCode),
            correspondencePostCode = Some(testCorrespondencePostCode),
            basePostCode = Some(testBasePostCode),
            commsPostCode = Some(testCommsPostCode),
            traderPostCode = Some(testTraderPostCode)
          ))
        }
      }
      "the json is valid but contains no postcodes" should {
        "return a successful empty list of postcodes" in {
          read(HttpResponse(
            responseStatus = OK,
            responseJson = Some(Json.obj())
          )) shouldBe Right(PartnershipKnownFacts(None, None, None, None, None))
        }
      }
      "the json is invalid" should {
        "return an UnexpectedGetPartnershipKnownFactsFailure" in {
          val httpResponse = HttpResponse(
            responseStatus = OK,
            responseJson = Some(Json.obj(
              postCodeKey -> Json.obj()
            ))
          )

          read(httpResponse) shouldBe Left(UnexpectedGetPartnershipKnownFactsFailure(OK, httpResponse.body))
        }
      }

    }
    "the response status is NOT_FOUND" should {
      "return PartnershipKnownFactsNotFound" in {
        read(HttpResponse(responseStatus = NOT_FOUND)) shouldBe Left(PartnershipKnownFactsNotFound)
      }
    }
    "any other response status" should {
      "return an UnexpectedGetPartnershipKnownFactsFailure" in {
        val httpResponse = HttpResponse(responseStatus = INTERNAL_SERVER_ERROR)

        read(httpResponse) shouldBe Left(UnexpectedGetPartnershipKnownFactsFailure(INTERNAL_SERVER_ERROR, httpResponse.body))
      }
    }
  }
}
