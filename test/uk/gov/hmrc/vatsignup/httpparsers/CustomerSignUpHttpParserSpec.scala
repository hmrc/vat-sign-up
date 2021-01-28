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

import play.api.http.Status.{BAD_REQUEST, OK}
import play.api.libs.json.Json
import uk.gov.hmrc.http.HttpResponse
import org.scalatest.{WordSpec, Matchers}
import uk.gov.hmrc.vatsignup.httpparsers.CustomerSignUpHttpParser.CustomerSignUpHttpReads
import uk.gov.hmrc.vatsignup.models.{CustomerSignUpResponseFailure, CustomerSignUpResponseSuccess}

class CustomerSignUpHttpParserSpec extends WordSpec with Matchers {
  val testHttpVerb = "POST"
  val testUri = "/"

  "CustomerSignUpHttpReads" when {
    "read" should {
      "parse a OK response as a CustomerSignUpResponseSuccess" in {
        val httpResponse = HttpResponse(OK)

        val res = CustomerSignUpHttpReads.read(testHttpVerb, testUri, httpResponse)

        res shouldBe Right(CustomerSignUpResponseSuccess)
      }

      "parse a BAD_REQUEST response as a CustomerSignUpResponseFailure" in {
        val testJson = Json.obj("code" -> "INVALID_REGIME")
        val httpResponse = HttpResponse(BAD_REQUEST, Some(testJson))

        val res = CustomerSignUpHttpReads.read(testHttpVerb, testUri, httpResponse)

        res shouldBe Left(CustomerSignUpResponseFailure(BAD_REQUEST, "{\n  \"code\" : \"INVALID_REGIME\"\n}"))
      }

    }
  }

}
