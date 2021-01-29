/*
 * Copyright 2021 HM Revenue & Customs
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

import org.scalatest.{EitherValues, Matchers, WordSpec}
import play.api.libs.json.Json
import play.api.test.Helpers._
import uk.gov.hmrc.http.{HttpResponse, InternalServerException}
import uk.gov.hmrc.vatsignup.config.featureswitch.FeatureSwitching
import uk.gov.hmrc.vatsignup.helpers.TestConstants._
import uk.gov.hmrc.vatsignup.httpparsers.KnownFactsAndControlListInformationHttpParser.KnownFactsAndControlListInformationHttpReads.read
import uk.gov.hmrc.vatsignup.httpparsers.KnownFactsAndControlListInformationHttpParser._
import uk.gov.hmrc.vatsignup.models.{KnownFactsAndControlListInformation, VatKnownFacts}

class KnownFactsAndControlListInformationHttpParserSpec extends WordSpec with Matchers with EitherValues with FeatureSwitching {
  val testMethod = "GET"
  val testUrl = "/"

  "KnownFactsAndControlListInformationHttpReads#read" when {
    "the http status is OK" when {
      s"the json is valid" should {
        "return known facts and control list information" in {
          val testResponse = HttpResponse(
            responseStatus = OK,
            responseJson = Some(
              Json.obj(
                "postcode" -> testPostCode,
                "dateOfReg" -> testDateOfRegistration,
                "lastReturnMonthPeriod" -> testLastReturnMonthPeriod,
                "lastNetDue" -> testLastNetDue.toDouble,
                "controlListInformation" -> ControlList34.valid
              )
            )
          )

          read(testMethod, testUrl, testResponse) shouldBe Right(testKnownFactsAndControlListInformation)
        }
      }
      s"the json is valid and the month is not available" should {
        "assume the VMF data is incomplete and return the known facts" in {
          val testResponse = HttpResponse(
            responseStatus = OK,
            responseJson = Some(
              Json.obj(
                "postcode" -> testPostCode,
                "dateOfReg" -> testDateOfRegistration,
                "lastReturnMonthPeriod" -> "N/A",
                "lastNetDue" -> testLastNetDue.toDouble,
                "controlListInformation" -> ControlList34.valid
              )
            )
          )

          read(testMethod, testUrl, testResponse) shouldBe Right(KnownFactsAndControlListInformation(
            VatKnownFacts(
              Some(testPostCode),
              testDateOfRegistration,
              None,
              Some(testLastNetDue)
            ),
            testControlListInformation
          ))
        }
      }

      s"the json is valid, the month is not available and the net due is 0" should {
        "return basic known facts and control list information" in {
          val testResponse = HttpResponse(
            responseStatus = OK,
            responseJson = Some(
              Json.obj(
                "postcode" -> testPostCode,
                "dateOfReg" -> testDateOfRegistration,
                "lastReturnMonthPeriod" -> "N/A",
                "lastNetDue" -> 0.00,
                "controlListInformation" -> ControlList34.valid
              )
            )
          )

          read(testMethod, testUrl, testResponse) shouldBe Right(KnownFactsAndControlListInformation(
            VatKnownFacts(
              Some(testPostCode),
              testDateOfRegistration,
              None,
              None
            ),
            testControlListInformation
          ))
        }
      }

      s"the json is invalid" should {
        "throw an InternalServerException" in {
          val testJson = Json.obj(
            "postcode" -> testPostCode,
            "dateOfReg" -> testDateOfRegistration
          )
          // No control list info
          val testResponse = HttpResponse(
            responseStatus = OK,
            responseJson = Some(testJson)
          )

          intercept[InternalServerException](
            read(
              method = testMethod,
              url = testUrl,
              response = testResponse
            )).message
            .shouldEqual("Invalid JSON response: controlList is missing in the json response")
        }
      }
    }
  }

  "the http status is BAD_REQUEST" should {
    "return KnownFactsInvalidVatNumber" in {
      val testResponse = HttpResponse(BAD_REQUEST)

      read(testMethod, testUrl, testResponse).left.value shouldBe KnownFactsInvalidVatNumber
    }
  }

  "the http status is NOT_FOUND" should {
    "return ControlListInformationVatNumberNotFound" in {
      val testResponse = HttpResponse(NOT_FOUND)

      read(testMethod, testUrl, testResponse).left.value shouldBe ControlListInformationVatNumberNotFound
    }
  }

  "the http status is anything else" should {
    "return UnexpectedKnownFactsAndControlListInformationFailure" in {
      val testResponse = HttpResponse(INTERNAL_SERVER_ERROR)

      read(testMethod, testUrl, testResponse).left.value shouldBe UnexpectedKnownFactsAndControlListInformationFailure(testResponse.status, testResponse.body)
    }
  }
}
