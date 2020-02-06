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
import play.api.test.Helpers._
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.vatsignup.httpparsers.GetEmailVerificationStateHttpParser.GetEmailVerificationStateHttpReads.read
import uk.gov.hmrc.vatsignup.httpparsers.GetEmailVerificationStateHttpParser._

class GetEmailVerificationStateHttpParserSpec extends WordSpec with Matchers {
  "GetEmailVerifiedHttpReads#read" when {
    "the response status is OK" should {
      "return a RegistrationSuccess with the returned SAFE ID" in {
        val httpResponse = HttpResponse(
          responseStatus = OK
        )

        read("", "", httpResponse) shouldBe Right(EmailVerified)
      }
    }

    "the response status is NOT_FOUND" should {
      "return an InvalidJsonResponse" in {
        val httpResponse = HttpResponse(
          responseStatus = NOT_FOUND
        )

        read("", "", httpResponse) shouldBe Right(EmailNotVerified)
      }
    }

    "the response status is INTERNAL_SERVER_ERROR" should {
      "return an InvalidJsonResponse" in {
        val httpResponse = HttpResponse(
          responseStatus = INTERNAL_SERVER_ERROR
        )

        read("", "", httpResponse) shouldBe Left(GetEmailVerificationStateErrorResponse(INTERNAL_SERVER_ERROR, httpResponse.body))
      }
    }

  }
}
