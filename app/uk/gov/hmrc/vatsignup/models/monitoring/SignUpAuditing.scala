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

import uk.gov.hmrc.vatsignup.models.ContactPreference
import uk.gov.hmrc.vatsignup.services.monitoring.AuditModel

object SignUpAuditing {
  val signUpTransactionName = "VATSignUpRequest"
  val signUpAuditType = "mtdVatSignUp"

  case class SignUpAuditModel(safeId: String,
                              vatNumber: String,
                              emailAddress: Option[String],
                              transactionEmail: String,
                              emailAddressVerified: Option[Boolean],
                              agentReferenceNumber: Option[String],
                              isSuccess: Boolean,
                              contactPreference: ContactPreference
                             ) extends AuditModel {

    override val transactionName: String = signUpTransactionName
    override val detail: Map[String, String] = Map(
      "safeId" -> Some(safeId),
      "vatNumber" -> Some(vatNumber),
      "emailAddress" -> emailAddress,
      "emailAddressVerified" -> emailAddressVerified.map(_.toString),
      "transactionEmail" -> Some(transactionEmail),
      "agentReferenceNumber" -> agentReferenceNumber,
      "isSuccess" -> Some(s"$isSuccess"),
      "contactPreference" -> Some(contactPreference.toString)
    ).collect { case (key, Some(value)) => key -> value }

    override val auditType: String = signUpAuditType
  }

  case class MigratedSignUpAuditModel(safeId: String,
                                     vatNumber: String,
                                     agentReferenceNumber: Option[String],
                                     isSuccess: Boolean
                                     ) extends AuditModel {

    override val transactionName: String = signUpTransactionName + "Migrated"
    override val detail: Map[String, String] = Map(
      "safeId" -> Some(safeId),
      "vatNumber" -> Some(vatNumber),
      "agentReferenceNumber" -> agentReferenceNumber,
      "isSuccess" -> Some(s"$isSuccess")
    ).collect { case (key, Some(value)) => key -> value }

    override val auditType: String = signUpAuditType
  }

}
