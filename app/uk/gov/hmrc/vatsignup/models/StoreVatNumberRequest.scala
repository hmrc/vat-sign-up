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

package uk.gov.hmrc.vatsignup.models

import java.time.Month
import java.time.format.TextStyle
import java.util.Locale

import play.api.libs.json._

case class StoreVatNumberRequest(vatNumber: String,
                                 vatKnownFacts: Option[VatKnownFacts])

object StoreVatNumberRequest {
  implicit val monthFormat = new Format[Month] {
    override def reads(json: JsValue): JsResult[Month] =
      json.validate[String] map VatKnownFacts.fromDisplayName

    override def writes(o: Month): JsValue = JsString(o.getDisplayName(TextStyle.FULL, Locale.ENGLISH))
  }

  val VatNumberKey = "vatNumber"
  val PostcodeKey = "postCode"
  val RegistrationDateKey = "registrationDate"
  val LastReturnMonthPeriodKey = "lastReturnMonthPeriod"
  val LastNetDueKey = "lastNetDue"

  implicit val format: OFormat[StoreVatNumberRequest] = new OFormat[StoreVatNumberRequest] {
    override def reads(json: JsValue): JsResult[StoreVatNumberRequest] = for {
      vatNumber <- (json \ VatNumberKey).validate[String]
      optPostCode <- (json \ PostcodeKey).validateOpt[String]
      optRegistrationDate <- (json \ RegistrationDateKey).validateOpt[String]
      optLastReturnMonthPeriod <- (json \ LastReturnMonthPeriodKey).validateOpt[Month]
      optLastNetDue <- (json \ LastNetDueKey).validateOpt[String]
    } yield {
      StoreVatNumberRequest(
        vatNumber,
        optRegistrationDate match {
          case Some(registrationDate) =>
            Some(VatKnownFacts(
              businessPostcode = optPostCode,
              vatRegistrationDate = registrationDate,
              lastReturnMonthPeriod = optLastReturnMonthPeriod,
              lastNetDue = optLastNetDue
            ))
          case None =>
            None
        }
      )
    }

    override def writes(storeVatNumberRequest: StoreVatNumberRequest): JsObject = {
      storeVatNumberRequest.vatKnownFacts match {
        case Some(vatKnownFacts) =>
          val postCodeJson = vatKnownFacts.businessPostcode match {
            case Some(postCode) => Json.obj(PostcodeKey -> postCode)
            case None => Json.obj()
          }

          val lastReturnMonthPeriodJson = vatKnownFacts.lastReturnMonthPeriod match {
            case Some(lastReturnMonthPeriod) => Json.obj(LastReturnMonthPeriodKey -> lastReturnMonthPeriod)
            case None => Json.obj()
          }

          val lastNetDueJson = vatKnownFacts.lastNetDue match {
            case Some(lastNetDue) => Json.obj(LastNetDueKey -> lastNetDue)
            case None => Json.obj()
          }

          Json.obj(
            VatNumberKey -> storeVatNumberRequest.vatNumber,
            RegistrationDateKey -> vatKnownFacts.vatRegistrationDate
          ) ++ postCodeJson ++ lastReturnMonthPeriodJson ++ lastNetDueJson
        case None =>
          Json.obj(
            VatNumberKey -> storeVatNumberRequest.vatNumber
          )
      }
    }
  }
}
