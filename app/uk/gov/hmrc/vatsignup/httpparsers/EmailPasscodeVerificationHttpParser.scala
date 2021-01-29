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

import play.api.http.Status.{CREATED, FORBIDDEN, NOT_FOUND, NO_CONTENT}
import uk.gov.hmrc.http.{HttpReads, HttpResponse, InternalServerException}

object EmailPasscodeVerificationHttpParser {

  val codeKey = "code"
  val passcodeMismatchKey = "PASSCODE_MISMATCH"
  val passcodeNotFoundKey = "PASSCODE_NOT_FOUND"
  val maxAttemptsExceededKey = "MAX_EMAILS_EXCEEDED"

  implicit object VerifyEmailVerificationPasscodeHttpReads extends HttpReads[EmailPasscodeVerificationResult] {
    override def read(method: String, url: String, response: HttpResponse): EmailPasscodeVerificationResult = {

      def errorCode: Option[String] = (response.json \ codeKey).asOpt[String]

      response.status match {
        case CREATED => EmailVerifiedSuccessfully
        case NO_CONTENT => EmailAlreadyVerified
        case NOT_FOUND if errorCode contains passcodeMismatchKey => PasscodeMismatch
        case NOT_FOUND if errorCode contains passcodeNotFoundKey => PasscodeNotFound
        case FORBIDDEN if errorCode contains maxAttemptsExceededKey => MaxAttemptsExceeded
        case status =>
          throw new InternalServerException(s"Unexpected response returned from VerifyEmailPasscode endpoint - Status: $status, response: ${response.body}")
      }
    }
  }

  sealed trait EmailPasscodeVerificationResult

  case object EmailVerifiedSuccessfully extends EmailPasscodeVerificationResult

  case object EmailAlreadyVerified extends EmailPasscodeVerificationResult

  case object PasscodeNotFound extends EmailPasscodeVerificationResult

  case object PasscodeMismatch extends EmailPasscodeVerificationResult

  case object MaxAttemptsExceeded extends EmailPasscodeVerificationResult

}