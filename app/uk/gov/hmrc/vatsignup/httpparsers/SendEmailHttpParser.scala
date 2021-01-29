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

import play.api.http.Status.ACCEPTED
import uk.gov.hmrc.http.{HttpReads, HttpResponse}

object SendEmailHttpParser {
  type SendEmailResponse = Either[SendEmailFailure, EmailQueued.type]

  implicit object SendEmailHttpReads extends HttpReads[SendEmailResponse] {
    override def read(method: String, url: String, response: HttpResponse): SendEmailResponse =
      response.status match {
        case ACCEPTED => Right(EmailQueued)
        case status => Left(SendEmailFailure(status, response.body))
      }
  }

  case object EmailQueued

  case class SendEmailFailure(status: Int, body: String)
}
