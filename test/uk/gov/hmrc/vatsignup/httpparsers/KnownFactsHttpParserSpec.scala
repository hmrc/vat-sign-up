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

package uk.gov.hmrc.vatsignup.httpparsers

import org.scalatest.{Matchers, WordSpec}
import play.api.libs.json.Json
import play.api.test.Helpers._
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.vatsignup.helpers.TestConstants._
import uk.gov.hmrc.vatsignup.httpparsers.KnownFactsHttpParser.KnownFactsHttpReads.read
import uk.gov.hmrc.vatsignup.httpparsers.KnownFactsHttpParser._

class KnownFactsHttpParserSpec extends WordSpec with Matchers {
  val testMethod = "GET"
  val testUrl = "/"

  "VatSubscriptionKnownFactsHttpReads#read" when {
    "the http status is OK" when {
      s"the json is valid" should {
        "return Known facts" in {
          val testResponse = HttpResponse(
            responseStatus = OK,
            responseJson = Some(
              Json.obj(
                "businessPostCode" -> testPostCode,
                "vatRegistrationDate" -> testDateOfRegistration
              )
            )
          )

          read(testMethod, testUrl, testResponse) shouldBe Right(KnownFacts(
            businessPostcode = testPostCode,
            vatRegistrationDate = testDateOfRegistration
          ))
        }
      }

      s"the json is invalid" should {
        "return InvalidKnownFacts" in {
          val testResponse = HttpResponse(
            responseStatus = OK,
            responseJson = Some(
              Json.obj(
                "businessPostCode" -> testPostCode
              )
            )
          )

          read(testMethod, testUrl, testResponse) shouldBe Left(InvalidKnownFacts(
            status = OK,
            body = invalidJsonResponseMessage
          ))
        }
      }
    }

    "the http status is BAD_REQUEST" should {
      "return InvalidVatNumber" in {
        val testResponse = HttpResponse(BAD_REQUEST)

        read(testMethod, testUrl, testResponse) shouldBe Left(InvalidVatNumber)
      }
    }

    "the http status is NOT_FOUND" should {
      "return VatNumberNotFound" in {
        val testResponse = HttpResponse(NOT_FOUND)

        read(testMethod, testUrl, testResponse) shouldBe Left(VatNumberNotFound)
      }
    }

    "the http status is anything else" should {
      "return InvalidKnownFacts" in {
        val testResponse = HttpResponse(INTERNAL_SERVER_ERROR)

        read(testMethod, testUrl, testResponse) shouldBe Left(InvalidKnownFacts(
          status = testResponse.status,
          body = testResponse.body
        ))
      }
    }
  }
}
