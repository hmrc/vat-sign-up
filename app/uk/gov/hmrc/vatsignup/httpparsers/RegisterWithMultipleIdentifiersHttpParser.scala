/*
 * Copyright 2019 HM Revenue & Customs
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
import play.api.libs.json.{JsError, JsSuccess}
import uk.gov.hmrc.http.{HttpReads, HttpResponse, InternalServerException}
import uk.gov.hmrc.vatsignup.config.Constants.Des._

object RegisterWithMultipleIdentifiersHttpParser {
  type RegisterWithMultipleIdentifiersResponse = Either[RegisterWithMultipleIdsErrorResponse, RegisterWithMultipleIdsSuccess]

  implicit object RegisterWithMultipleIdentifiersHttpReads extends HttpReads[RegisterWithMultipleIdentifiersResponse] {
    override def read(method: String, url: String, response: HttpResponse): RegisterWithMultipleIdentifiersResponse = {
      response.status match {
        case OK =>
          (for {
            idType <- (response.json \ IdentificationKey \ 0 \ IdTypeKey).validate[String]
            if idType == SafeIdKey
            safeId <- (response.json \ IdentificationKey \ 0 \ IdValueKey).validate[String]
          } yield safeId) match {
            case JsSuccess(safeId, _) => Right(RegisterWithMultipleIdsSuccess(safeId))
            case error: JsError => throw new InternalServerException(s"Invalid JSON returned on Register API: ${response.body}")
          }
        case status =>
          Left(RegisterWithMultipleIdsErrorResponse(status, response.body))
      }
    }
  }

  case class RegisterWithMultipleIdsSuccess(safeId: String)

  case class RegisterWithMultipleIdsErrorResponse(status: Int, body: String)

}
