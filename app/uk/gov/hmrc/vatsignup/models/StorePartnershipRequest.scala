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

package uk.gov.hmrc.vatsignup.models

import play.api.libs.json.{JsResult, JsValue, Reads}
import uk.gov.hmrc.http.BadRequestException

case class StorePartnershipRequest(partnership: PartnershipBusinessEntity, postCode: Option[String])

object StorePartnershipRequest {

  implicit val reader = new Reads[StorePartnershipRequest] {
    override def reads(json: JsValue): JsResult[StorePartnershipRequest] =
      for {
        partnership <- json.validate[PartnershipBusinessEntity](PartnershipBusinessEntityReader)
        postCode <- (json \ "postCode").validateOpt[String]
      } yield StorePartnershipRequest(partnership, postCode)

  }

  object PartnershipBusinessEntityReader extends Reads[PartnershipBusinessEntity] {
    override def reads(json: JsValue): JsResult[PartnershipBusinessEntity] =
      for {
        sautr <- (json \ "sautr").validateOpt[String]
        optCompanyNumber <- (json \ "crn").validateOpt[String]
        partnershipType <- (json \ "partnershipType").validate[String]
      } yield (partnershipType, optCompanyNumber) match {
        case ("generalPartnership", _) => GeneralPartnership(sautr)
        case ("limitedPartnership", Some(companyNumber)) => LimitedPartnership(sautr, companyNumber)
        case ("limitedLiabilityPartnership", Some(companyNumber)) => LimitedLiabilityPartnership(sautr, companyNumber)
        case ("scottishLimitedPartnership", Some(companyNumber)) => ScottishLimitedPartnership(sautr, companyNumber)
        case _ => throw new BadRequestException(s"Invalid Partnership Information ${json.toString()}")
      }
  }

}