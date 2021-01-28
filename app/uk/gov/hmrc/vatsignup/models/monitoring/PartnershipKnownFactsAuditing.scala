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

package uk.gov.hmrc.vatsignup.models.monitoring

import uk.gov.hmrc.vatsignup.models.PartnershipKnownFacts
import uk.gov.hmrc.vatsignup.services.monitoring.AuditModel


object PartnershipKnownFactsAuditing {

  val partnershipKFTransactionName = "MtdVatPartnershipKnownFactsMatchRequest"
  val partnershipKFAuditType = "MtdVatPartnershipKnownFactsMatch"

  case class PartnershipKnownFactsAuditingModel(vatNumber: String,
                                                sautr: String,
                                                postCode: String,
                                                partnershipKnownFacts: Option[PartnershipKnownFacts],
                                                isMatch: Option[Boolean]
                                               ) extends AuditModel {

    override val transactionName: String = partnershipKFTransactionName
    override val auditType: String = partnershipKFAuditType

    override val detail: Map[String, String] = Map[String, Option[String]](
      "vatNumber" -> Some(vatNumber),
      "sautr" -> Some(sautr),
      "postcode" -> Some(postCode),
      "retrieved.postCode" -> partnershipKnownFacts.flatMap(_.postCode),
      "retrieved.correspondencePostCode" -> partnershipKnownFacts.flatMap(_.correspondencePostCode),
      "retrieved.basePostCode" -> partnershipKnownFacts.flatMap(_.basePostCode),
      "retrieved.commsPostCode" -> partnershipKnownFacts.flatMap(_.commsPostCode),
      "retrieved.traderPostCode" -> partnershipKnownFacts.flatMap(_.traderPostCode),
      "isMatch" -> Some(s"$isMatch"),
      "sautrNotFound" -> (if(partnershipKnownFacts.isEmpty) Some("true") else None)
    ).collect {
      case (key, Some(v)) => (key, v)
    }

  }

  object PartnershipKnownFactsAuditingModel {
    def apply(vatNumber: String,
              sautr: String,
              postCode: String,
              partnershipKownFacts: PartnershipKnownFacts,
              isMatch: Boolean
             ): PartnershipKnownFactsAuditingModel =
      new PartnershipKnownFactsAuditingModel(vatNumber, sautr, postCode, Some(partnershipKownFacts), Some(isMatch))

    def apply(vatNumber: String,
              sautr: String,
              postCode: String
             ): PartnershipKnownFactsAuditingModel =
      new PartnershipKnownFactsAuditingModel(vatNumber, sautr, postCode, None, None)
  }

}
