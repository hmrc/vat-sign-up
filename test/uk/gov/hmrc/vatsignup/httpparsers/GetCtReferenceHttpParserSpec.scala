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

package uk.gov.hmrc.vatsignup.httpparsers

import play.api.http.Status._
import play.api.libs.json.Json
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.vatsignup.helpers.TestConstants._
import uk.gov.hmrc.vatsignup.httpparsers.GetCtReferenceHttpParser.GetCtReferenceFailure
import uk.gov.hmrc.vatsignup.httpparsers.GetCtReferenceHttpParser.GetCtReferenceHttpReads.read

class GetCtReferenceHttpParserSpec extends UnitSpec {
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
    "the response is INTERNAL_SERVER_ERRER" should {
      "return a failure" in {
        val httpResponse = HttpResponse(
          responseStatus = INTERNAL_SERVER_ERROR
        )

        read("", "", httpResponse) shouldBe Left(GetCtReferenceFailure(httpResponse.status, httpResponse.body))
      }
    }
  }
}
