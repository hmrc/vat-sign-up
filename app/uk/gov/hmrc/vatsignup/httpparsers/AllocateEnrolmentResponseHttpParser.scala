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

import play.api.http.Status._
import uk.gov.hmrc.http.{HttpReads, HttpResponse}

object AllocateEnrolmentResponseHttpParser {
  type AllocateEnrolmentResponse = Either[EnrolFailure, EnrolSuccess.type]

  val CodeKey = "code"
  val MultipleEnrolmentsInvalidKey = "MULTIPLE_ENROLMENTS_INVALID"

  implicit object AllocateEnrolmentResponseHttpReads extends HttpReads[AllocateEnrolmentResponse] {
    override def read(method: String, url: String, response: HttpResponse): AllocateEnrolmentResponse = {
      def responseCode: Seq[String] = (response.json \\ CodeKey).map(_.as[String])

      response.status match {
        case CREATED => Right(EnrolSuccess)
        case CONFLICT if responseCode.contains(MultipleEnrolmentsInvalidKey) => Left(MultipleEnrolmentsInvalid)
        case _ => Left(UnexpectedEnrolFailure(response.body))
      }
    }
  }

  case object EnrolSuccess

  case object MultipleEnrolmentsInvalid extends EnrolFailure {
    override val message: String = "Multiple Enrolments are not valid for this service"
  }

  sealed trait EnrolFailure {
    val message: String
  }

  case class UnexpectedEnrolFailure(message: String) extends EnrolFailure

}
