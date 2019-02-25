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
import uk.gov.hmrc.vatsignup.httpparsers.KnownFactsAndControlListInformationHttpParser.KnownFactsAndControlListInformationHttpReads.read
import uk.gov.hmrc.vatsignup.httpparsers.KnownFactsAndControlListInformationHttpParser._
import uk.gov.hmrc.vatsignup.models.VatKnownFacts

class KnownFactsAndControlListInformationHttpParserSpec extends UnitSpec with EitherValues {
  val testMethod = "GET"
  val testUrl = "/"

  "KnownFactsAndControlListInformationHttpReads#read" when {
    "the http status is OK" when {
      s"the json is valid" should {
        "return Known facts and control list information" in {
          val testResponse = HttpResponse(
            responseStatus = OK,
            responseJson = Some(
              Json.obj(
                "postcode" -> testPostCode,
                "dateOfReg" -> testDateOfRegistration,
                "lastReturnMonthPeriod" -> testLastReturnMonthPeriod,
                "lastNetDue" -> testLastNetDue,
                "controlListInformation" -> ControlList32.valid
              )
            )
          )

          read(testMethod, testUrl, testResponse) shouldBe Right(VatKnownFacts(
            testPostCode,
            testDateOfRegistration,
            Some(testLastReturnMonthPeriod),
            Some(testLastNetDue),
            testControlListInformation
          ))
        }
      }

      s"the json is invalid" should {
        "return UnexpectedKnownFactsAndControlListInformationFailure" in {
          val testJson = Json.obj(
            "postcode" -> testPostCode,
            "dateOfReg" -> testDateOfRegistration
          )
          // No control list info
          val testResponse = HttpResponse(
            responseStatus = OK,
            responseJson = Some(testJson)
          )

          val res: UnexpectedKnownFactsAndControlListInformationFailure = read(
            method = testMethod,
            url = testUrl,
            response = testResponse
          ).left.value.asInstanceOf[UnexpectedKnownFactsAndControlListInformationFailure]

          res.status shouldBe OK
          res.body should include(invalidJsonResponseMessage)

          Json.parse(res.body.replace(invalidJsonResponseMessage,"")) shouldBe testJson
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

}
