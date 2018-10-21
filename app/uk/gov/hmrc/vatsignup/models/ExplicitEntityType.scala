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

sealed trait ExplicitEntityType {
  val StringValue: String

  override def toString: String = StringValue
}

object ExplicitEntityType {

  case object GeneralPartnership extends ExplicitEntityType {
    val StringValue = "generalPartnership"
  }

  sealed trait LimitedPartnershipBase extends ExplicitEntityType

  case object LimitedPartnership extends LimitedPartnershipBase {
    val StringValue = "limitedPartnership"
  }

  case object LimitedLiabilityPartnership extends LimitedPartnershipBase {
    val StringValue = "limitedLiabilityPartnership"
  }

  case object ScottishLimitedPartnership extends LimitedPartnershipBase {
    val StringValue = "scottishLimitedPartnership"
  }

  val partnershipEntityTypeFrontEndKey = "entityType"

  import play.api.libs.json._

  val reader: Reads[ExplicitEntityType] = JsPath.read[String].map {
    case GeneralPartnership.StringValue => GeneralPartnership
    case LimitedPartnership.StringValue => LimitedPartnership
    case LimitedLiabilityPartnership.StringValue => LimitedLiabilityPartnership
    case ScottishLimitedPartnership.StringValue => ScottishLimitedPartnership
  }
  val writer: Writes[ExplicitEntityType] = new Writes[ExplicitEntityType] {
    def writes(partnershipEntityType: ExplicitEntityType): JsValue =
      JsString(partnershipEntityType.toString)
  }

  implicit val format: Format[ExplicitEntityType] = Format(reader, writer)
}
