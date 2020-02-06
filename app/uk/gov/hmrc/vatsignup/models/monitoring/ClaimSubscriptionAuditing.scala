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

package uk.gov.hmrc.vatsignup.models.monitoring

import uk.gov.hmrc.vatsignup.services.monitoring.AuditModel

object ClaimSubscriptionAuditing {

  val claimSubscriptionTransactionName = "MTDVATClaimSubscriptionRequest"
  val claimSubscriptionAuditType = "mtdVatClaimSubscription"

  case class ClaimSubscriptionAuditModel(vatNumber: String,
                                         businessPostcode: String,
                                         vatRegistrationDate: String,
                                         isFromBta: Boolean,
                                         isSuccess: Boolean,
                                         allocateEnrolmentFailureMessage: Option[String] = None,
                                         upsertEnrolmentFailureMessage: Option[String] = None
                                        ) extends AuditModel {

    override val transactionName: String = claimSubscriptionTransactionName

    override val detail: Map[String, String] = Map(
      "vatNumber" -> vatNumber,
      "businessPostcode" -> businessPostcode,
      "vatRegistrationDate" -> vatRegistrationDate,
      "isFromBta" -> isFromBta.toString,
      "isSuccess" -> isSuccess.toString,
      "allocateEnrolmentFailureMessage" -> allocateEnrolmentFailureMessage.getOrElse(""),
      "upsertKnownFactsFailureMessage" -> upsertEnrolmentFailureMessage.getOrElse("")
    ).filter { case (_, value) => value.nonEmpty }

    override val auditType: String = claimSubscriptionAuditType
  }

}
