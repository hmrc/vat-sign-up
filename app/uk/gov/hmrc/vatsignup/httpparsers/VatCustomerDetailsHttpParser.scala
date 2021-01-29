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
import play.api.libs.json.{JsError, JsSuccess}
import uk.gov.hmrc.http.{HttpReads, HttpResponse, InternalServerException}
import uk.gov.hmrc.vatsignup.models.{KnownFacts, VatCustomerDetails}

object VatCustomerDetailsHttpParser {
  type VatCustomerDetailsHttpParserResponse = Either[VatCustomerDetailsFailure, VatCustomerDetails]

  val postCodeKey = "businessPostCode"

  val registrationDateKey = "vatRegistrationDate"

  val invalidJsonResponseMessage = "Invalid JSON response"

  val isOverseasKey = "isOverseas"

  val deregisteredUserKey = "deregistered"

  //noinspection ScalaStyle
  implicit object VatCustomerDetailsHttpReads extends HttpReads[VatCustomerDetailsHttpParserResponse] {
    def read(method: String, url: String, response: HttpResponse): VatCustomerDetailsHttpParserResponse = {
      response.status match {
        case OK =>
          (response.json \ deregisteredUserKey).validateOpt[Boolean] match {
            case JsSuccess(Some(true), _) =>
              Left(Deregistered)
            case JsSuccess(_, _) =>
              (
                (response.json \ postCodeKey).validateOpt[String],
                (response.json \ registrationDateKey).validate[String],
                (response.json \ isOverseasKey).validate[Boolean]
              ) match {
                case (JsSuccess(postCode, _), JsSuccess(regDate, _), JsSuccess(isOverseas, _)) =>
                  Right(VatCustomerDetails(KnownFacts(postCode, regDate), isOverseas))
                case (JsError(_), _, _) => throw new InternalServerException("[VatCustomerDetailsHttpParser] postcode missing in known facts response")
                case (_, JsError(_), _) => throw new InternalServerException("[VatCustomerDetailsHttpParser] registration date missing in known facts response")
                case (_, _, JsError(_)) => throw new InternalServerException("[VatCustomerDetailsHttpParser] isOverseas field failed to parse in known facts response")
              }
          }
        case BAD_REQUEST =>
          Left(InvalidVatNumber)
        case NOT_FOUND =>
          Left(VatNumberNotFound)
        case _ => throw new InternalServerException(
          s"[VatCustomerDetailsHttpParser] GetKnownFacts API returned, Response Status: ${response.status} Response Body: ${response.body}")
      }
    }
  }

  sealed trait VatCustomerDetailsFailure

  case object InvalidVatNumber extends VatCustomerDetailsFailure

  case object VatNumberNotFound extends VatCustomerDetailsFailure

  case object Deregistered extends VatCustomerDetailsFailure

}
