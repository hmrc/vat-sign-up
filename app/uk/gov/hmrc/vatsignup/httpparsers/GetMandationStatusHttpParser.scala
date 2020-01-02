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

import play.api.http.Status._
import uk.gov.hmrc.http.{HttpReads, HttpResponse}
import uk.gov.hmrc.vatsignup.models._

object GetMandationStatusHttpParser {
  type GetMandationStatusResponse = Either[GetMandationStatusFailure, MandationStatus]

  val MandationStatusKey = "mandationStatus"

  implicit object GetMandationStatusHttpReads extends HttpReads[GetMandationStatusResponse] {
    override def read(method: String, url: String, response: HttpResponse): GetMandationStatusResponse =
      response.status match {
        case OK =>
          (response.json \ MandationStatusKey).asOpt[String] collect {
          case MTDfBMandated.Name => MTDfBMandated
          case MTDfBVoluntary.Name => MTDfBVoluntary
          case NonMTDfB.Name => NonMTDfB
          case NonDigital.Name => NonDigital
        } toRight GetMandationStatusHttpFailure(OK, response.body)
        case PRECONDITION_FAILED =>
          Left(MigrationInProgress)
        case NOT_FOUND =>
          Left(VatNumberNotFound)
        case status =>
          Left(GetMandationStatusHttpFailure(status, response.body))
      }
  }

  sealed trait GetMandationStatusFailure
  case object VatNumberNotFound extends GetMandationStatusFailure
  case object MigrationInProgress extends GetMandationStatusFailure
  case class GetMandationStatusHttpFailure(status: Int, body: String) extends GetMandationStatusFailure

}
