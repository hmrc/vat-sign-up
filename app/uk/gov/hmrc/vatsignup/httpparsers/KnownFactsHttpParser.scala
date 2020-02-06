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

import play.api.http.Status.{BAD_REQUEST, NOT_FOUND, OK}
import play.api.libs.json.JsSuccess
import uk.gov.hmrc.http.{HttpReads, HttpResponse}

object KnownFactsHttpParser {
  type KnownFactsHttpParserResponse = Either[KnownFactsFailure, KnownFacts]

  val postCodeKey = "businessPostCode"

  val registrationDateKey = "vatRegistrationDate"

  val invalidJsonResponseMessage = "Invalid JSON response"

  implicit object KnownFactsHttpReads extends HttpReads[KnownFactsHttpParserResponse] {
    def read(method: String, url: String, response: HttpResponse): KnownFactsHttpParserResponse = {
      response.status match {
        case OK =>
          (
            (response.json \ postCodeKey).validate[String],
            (response.json \ registrationDateKey).validate[String]
          ) match {
            case (JsSuccess(bpc, _), JsSuccess(vrd, _)) => Right(KnownFacts(
              businessPostcode = bpc,
              vatRegistrationDate = vrd
            ))
            case _ => Left(InvalidKnownFacts(
              status = response.status,
              body = invalidJsonResponseMessage
            ))
          }
        case BAD_REQUEST =>
          Left(InvalidVatNumber)
        case NOT_FOUND =>
          Left(VatNumberNotFound)
        case _ =>
          Left(InvalidKnownFacts(
            status = response.status,
            body = response.body
          ))
      }
    }
  }

  case class KnownFacts(businessPostcode: String, vatRegistrationDate: String)

  sealed trait KnownFactsFailure

  case class InvalidKnownFacts(status: Int, body: String) extends KnownFactsFailure

  case object InvalidVatNumber extends KnownFactsFailure

  case object VatNumberNotFound extends KnownFactsFailure

}
