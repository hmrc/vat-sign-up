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
import uk.gov.hmrc.vatsignup.httpparsers.CreateEmailVerificationRequestHttpParser.CreateEmailVerificationRequestHttpReads.read
import uk.gov.hmrc.vatsignup.httpparsers.CreateEmailVerificationRequestHttpParser._

class CreateEmailVerificationRequestHttpParserSpec extends WordSpec with Matchers {
  "GetEmailVerifiedHttpReads#read" when {
    "the response status is OK" should {
      "return a RegistrationSuccess with the returned SAFE ID" in {
        val httpResponse = HttpResponse(
          responseStatus = CREATED
        )

        read("", "", httpResponse) shouldBe Right(EmailVerificationRequestSent)
      }
    }

    "the response status is NOT_FOUND" should {
      "return an InvalidJsonResponse" in {
        val httpResponse = HttpResponse(
          responseStatus = CONFLICT
        )

        read("", "", httpResponse) shouldBe Right(EmailAlreadyVerified)
      }
    }

    "the response status is INTERNAL_SERVER_ERROR" should {
      "return an InvalidJsonResponse" in {
        val httpResponse = HttpResponse(
          responseStatus = INTERNAL_SERVER_ERROR
        )

        read("", "", httpResponse) shouldBe Left(EmailVerificationRequestFailure(INTERNAL_SERVER_ERROR, httpResponse.body))
      }
    }

  }
}
