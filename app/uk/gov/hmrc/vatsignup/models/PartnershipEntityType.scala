/*
 * Copyright 2018 HM Revenue & Customs
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

sealed trait PartnershipEntityType {
  val StringValue: String

  override def toString: String = StringValue
}

object PartnershipEntityType {

  case object GeneralPartnership extends PartnershipEntityType {
    val StringValue = "ordinaryPartnership"
  }

  case object LimitedPartnership extends PartnershipEntityType {
    val StringValue = "limitedPartnership"
  }

  val partnershipEntityTypeFrontEndKey = "entityType"

  import play.api.libs.json._

  val reader: Reads[PartnershipEntityType] = JsPath.read[String].map {
    case GeneralPartnership.StringValue => GeneralPartnership
    case LimitedPartnership.StringValue => LimitedPartnership
  }
  val writer: Writes[PartnershipEntityType] = new Writes[PartnershipEntityType] {
    def writes(partnershipEntityType: PartnershipEntityType): JsValue =
      JsString(partnershipEntityType.toString)
  }

  implicit val format: Format[PartnershipEntityType] = Format(reader, writer)
}
