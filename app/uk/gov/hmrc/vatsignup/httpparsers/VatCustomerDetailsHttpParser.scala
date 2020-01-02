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
import play.api.libs.json.JsSuccess
import uk.gov.hmrc.http.{HttpReads, HttpResponse}
import uk.gov.hmrc.vatsignup.httpparsers.KnownFactsHttpParser.KnownFacts
import uk.gov.hmrc.vatsignup.models.VatCustomerDetails

object VatCustomerDetailsHttpParser {
  type VatCustomerDetailsHttpParserResponse = Either[VatCustomerDetailsFailure, VatCustomerDetails]

  val postCodeKey = "businessPostCode"

  val registrationDateKey = "vatRegistrationDate"

  val invalidJsonResponseMessage = "Invalid JSON response"

  val isOverseasKey = "isOverseas"

  val deregisteredUserKey = "deregistered"

  implicit object VatCustomerDetailsHttpReads extends HttpReads[VatCustomerDetailsHttpParserResponse] {
    def read(method: String, url: String, response: HttpResponse): VatCustomerDetailsHttpParserResponse = {
      response.status match {
        case OK =>
          (response.json \ deregisteredUserKey).validateOpt[Boolean] match {
            case JsSuccess(Some(true), _) =>
              Left(Deregistered)
            case JsSuccess(_, _) =>
              (for {
                businessPostcode <- (response.json \ postCodeKey).validate[String]
                vatRegistrationDate <- (response.json \ registrationDateKey).validate[String]
                isOverseas <- (response.json \ isOverseasKey).validate[Boolean]
              } yield VatCustomerDetails(KnownFacts(businessPostcode, vatRegistrationDate), isOverseas)) match {
                case JsSuccess(customerDetails, _) =>
                  Right(customerDetails)
                case _ =>
                  Left(InvalidKnownFacts(
                    status = response.status,
                    body = invalidJsonResponseMessage
                  ))
              }
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

  sealed trait VatCustomerDetailsFailure

  case class InvalidKnownFacts(status: Int, body: String) extends VatCustomerDetailsFailure

  case object InvalidVatNumber extends VatCustomerDetailsFailure

  case object VatNumberNotFound extends VatCustomerDetailsFailure

  case object Deregistered extends VatCustomerDetailsFailure

}
