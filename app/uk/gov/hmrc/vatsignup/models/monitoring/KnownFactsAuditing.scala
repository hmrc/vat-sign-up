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

import uk.gov.hmrc.vatsignup.models.VatKnownFacts
import uk.gov.hmrc.vatsignup.services.monitoring.AuditModel

object KnownFactsAuditing {
  val knownFactsTransactionName = "VATKnownFactsMatching"
  val knownFactsAuditType = "vatKnownFactsMatching"

  case class KnownFactsAuditModel(vatNumber: String,
                                  enteredKnownFacts: VatKnownFacts,
                                  storedKnownFacts: VatKnownFacts,
                                  matched: Boolean
                                 ) extends AuditModel {
    override val transactionName: String = knownFactsTransactionName
    override val detail: Map[String, String] = Map(
      "vatNumber" -> Some(vatNumber),
      "enteredPostCode" -> enteredKnownFacts.businessPostcode,
      "enteredVatRegistrationDate" -> Some(enteredKnownFacts.vatRegistrationDate),
      "enteredLastReturnMonthPeriod" -> (enteredKnownFacts.lastReturnMonthPeriod map (_.toString)),
      "enteredLastNetDue" -> enteredKnownFacts.lastNetDue,
      "storedPostCode" -> storedKnownFacts.businessPostcode,
      "storedVatRegistrationDate" -> Some(storedKnownFacts.vatRegistrationDate),
      "storedLastReturnMonthPeriod" -> (storedKnownFacts.lastReturnMonthPeriod map (_.toString)),
      "storedLastNetDue" -> storedKnownFacts.lastNetDue,
      "matched" -> Some(s"$matched")
    ) collect { case (key, Some(value)) => key -> value }

    override val auditType: String = knownFactsAuditType
  }

}
