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

import org.scalatest.EitherValues
import play.api.test.Helpers._
import play.api.libs.json.Json
import uk.gov.hmrc.http.HttpResponse
import org.scalatest.{WordSpec, Matchers}
import uk.gov.hmrc.vatsignup.helpers.TestConstants._
import uk.gov.hmrc.vatsignup.httpparsers.KnownFactsHttpParser.KnownFacts
import uk.gov.hmrc.vatsignup.httpparsers.VatCustomerDetailsHttpParser.VatCustomerDetailsHttpReads.read
import uk.gov.hmrc.vatsignup.httpparsers.VatCustomerDetailsHttpParser._
import uk.gov.hmrc.vatsignup.models.VatCustomerDetails

class VatCustomerDetailsHttpParserSpec extends WordSpec with Matchers with EitherValues {
  val testMethod = "GET"
  val testUrl = "/"

  "VatCustomerDetails#read" when {
    "the http status is OK" when {
      s"the json contains deregistered" should {
        "return Deregistered" in {
          val testResponse = HttpResponse(
            responseStatus = OK,
            responseJson = Some(
              Json.obj(
                "deregistered" -> true
              )
            )
          )

          read(testMethod, testUrl, testResponse) shouldBe Left(Deregistered)
        }
      }
      s"the json does not contain deregistered and is valid" should {
        "return Known facts" in {
          val testResponse = HttpResponse(
            responseStatus = OK,
            responseJson = Some(
              Json.obj(
                "businessPostCode" -> testPostCode,
                "vatRegistrationDate" -> testDateOfRegistration,
                "isOverseas" -> false
              )
            )
          )

          read(testMethod, testUrl, testResponse) shouldBe Right(VatCustomerDetails(
            KnownFacts(
              businessPostcode = testPostCode,
              vatRegistrationDate = testDateOfRegistration
            ),
            isOverseas = false
          ))
        }
      }

      s"the json does not contain deregistered and is invalid" should {
        "return InvalidKnownFacts" in {
          val testResponse = HttpResponse(
            responseStatus = OK,
            responseJson = Some(
              Json.obj(
                "businessPostCode" -> testPostCode
              )
            )
          )

          read(testMethod, testUrl, testResponse).left.value shouldBe InvalidKnownFacts(
            status = OK,
            body = invalidJsonResponseMessage
          )
        }
      }
    }

    "the http status is BAD_REQUEST" should {
      "return InvalidVatNumber" in {
        val testResponse = HttpResponse(BAD_REQUEST)

        read(testMethod, testUrl, testResponse).left.value shouldBe InvalidVatNumber
      }
    }

    "the http status is NOT_FOUND" should {
      "return VatNumberNotFound" in {
        val testResponse = HttpResponse(NOT_FOUND)

        read(testMethod, testUrl, testResponse).left.value shouldBe VatNumberNotFound
      }
    }

    "the http status is anything else" should {
      "return InvalidKnownFacts" in {
        val testResponse = HttpResponse(INTERNAL_SERVER_ERROR)

        read(testMethod, testUrl, testResponse).left.value shouldBe InvalidKnownFacts(
          status = testResponse.status,
          body = testResponse.body
        )
      }
    }
  }
}
