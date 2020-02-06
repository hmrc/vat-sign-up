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
import play.api.libs.json.{JsError, JsSuccess}
import uk.gov.hmrc.http.{HttpReads, HttpResponse}
import uk.gov.hmrc.vatsignup.models.PartnershipKnownFacts

object GetPartnershipKnownFactsHttpParser {
  type GetPartnershipKnownFactsResponse = Either[GetPartnershipKnownFactsFailure, PartnershipKnownFacts]

  val postCodeKey = "postCode"

  val correspondenceDetailsKey = "correspondenceDetails"
  val correspondencePostCodeKey = "corresPostCode"

  val basePostCodeKey = "basePostCode"

  val commsPostCodeKey = "commsPostCode"

  val traderPostCodeKey = "traderPostCode"

  implicit object GetPartnershipKnownFactsHttpReads extends HttpReads[GetPartnershipKnownFactsResponse] {
    override def read(method: String, url: String, response: HttpResponse): GetPartnershipKnownFactsResponse =
      response.status match {
        case OK =>
          val json = response.json

          val res = for {
            postCode <- (json \ postCodeKey).validateOpt[String]
            correspondencePostCode <- (json \ correspondenceDetailsKey \ correspondencePostCodeKey).validateOpt[String]
            baseTaxpayerPostCode <- (json \ basePostCodeKey).validateOpt[String]
            commsPostCode <- (json \ commsPostCodeKey).validateOpt[String]
            traderPostCode <- (json \ traderPostCodeKey).validateOpt[String]
          } yield PartnershipKnownFacts(
            postCode = postCode,
            correspondencePostCode = correspondencePostCode,
            basePostCode = baseTaxpayerPostCode,
            commsPostCode = commsPostCode,
            traderPostCode = traderPostCode
          )

          res match {
            case JsSuccess(partnershipKnownFacts, _) => Right(partnershipKnownFacts)
            case error: JsError => Left(UnexpectedGetPartnershipKnownFactsFailure(OK, response.body))
          }
        case NOT_FOUND =>
          Left(PartnershipKnownFactsNotFound)
        case status =>
          Left(UnexpectedGetPartnershipKnownFactsFailure(status, response.body))
      }
  }

  sealed trait GetPartnershipKnownFactsFailure

  case class UnexpectedGetPartnershipKnownFactsFailure(status: Int, body: String) extends GetPartnershipKnownFactsFailure

  case object PartnershipKnownFactsNotFound extends GetPartnershipKnownFactsFailure

}
