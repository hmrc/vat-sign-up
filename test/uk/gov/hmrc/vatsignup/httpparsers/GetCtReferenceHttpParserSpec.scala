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

import play.api.test.Helpers._
import play.api.libs.json.Json
import uk.gov.hmrc.http.HttpResponse
import org.scalatest.{WordSpec, Matchers}
import uk.gov.hmrc.vatsignup.helpers.TestConstants._
import uk.gov.hmrc.vatsignup.httpparsers.GetCtReferenceHttpParser.GetCtReferenceHttpReads.read
import uk.gov.hmrc.vatsignup.httpparsers.GetCtReferenceHttpParser.{CtReferenceNotFound, GetCtReferenceFailure}

class GetCtReferenceHttpParserSpec extends WordSpec with Matchers {
  "GetCtReferenceHttpReads#read" when {
    "the response status is OK" when {
      "the body contains a valid CT reference" should {
        "return the CT reference" in {
          val httpResponse = HttpResponse(
            responseStatus = OK,
            responseJson = Some(Json.obj("CTUTR" -> testCtReference))
          )

          read("", "", httpResponse) shouldBe Right(testCtReference)
        }
      }
      "the body is not valid" should {
        "return a failure" in {
          val httpResponse = HttpResponse(
            responseStatus = OK,
            responseJson = Some(Json.obj())
          )

          read("", "", httpResponse) shouldBe Left(GetCtReferenceFailure(httpResponse.status, httpResponse.body))
        }
      }
    }
    "the response is NOT_FOUND" should {
      "return a CtReferenceNotFound" in {
        val httpResponse = HttpResponse(
          responseStatus = NOT_FOUND
        )

        read("", "", httpResponse) shouldBe Left(CtReferenceNotFound)
      }
    }
    "the response is INTERNAL_SERVER_ERROR" should {
      "return a failure" in {
        val httpResponse = HttpResponse(
          responseStatus = INTERNAL_SERVER_ERROR
        )

        read("", "", httpResponse) shouldBe Left(GetCtReferenceFailure(httpResponse.status, httpResponse.body))
      }
    }
  }
}
