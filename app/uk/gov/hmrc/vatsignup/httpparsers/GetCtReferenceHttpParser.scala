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

import play.api.http.Status.{NOT_FOUND, OK}
import play.api.libs.json.JsSuccess
import uk.gov.hmrc.http.{HttpReads, HttpResponse}

object GetCtReferenceHttpParser {
  type GetCtReferenceResponse = Either[GetCtReferenceFailureResponse, String]

  val CtReferenceKey = "CTUTR"

  implicit object GetCtReferenceHttpReads extends HttpReads[GetCtReferenceResponse] {
    override def read(method: String, url: String, response: HttpResponse): GetCtReferenceResponse =
      response.status match {
        case OK =>
          (response.json \ CtReferenceKey).validate[String] match {
            case JsSuccess(ctReference, _) =>
              Right(ctReference)
            case _ =>
              Left(GetCtReferenceFailure(response.status, response.body))
          }
        case NOT_FOUND =>
          Left(CtReferenceNotFound)
        case _ =>
          Left(GetCtReferenceFailure(response.status, response.body))
      }
  }

  sealed trait GetCtReferenceFailureResponse

  case object CtReferenceNotFound extends GetCtReferenceFailureResponse

  case class GetCtReferenceFailure(status: Int, body: String) extends GetCtReferenceFailureResponse

}
