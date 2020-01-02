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

object AutoClaimEnrolementAuditing {

  val AutoClaimEnrolementTransactionName = "MTDVATAutoEnrolment"
  val AutoClaimEnrolementAuditType = "mtdVatAutoEnrolment"

  case class AutoClaimEnrolementAuditingModel(vatNumber: String,
                                              triggerPoint: String,
                                              isSuccess: Boolean,
                                              call: Option[String] = None,
                                              groupId: Option[String] = None,
                                              userIds: Set[String] = Set.empty,
                                              auditInformation: Option[String] = None
                                             ) extends AuditModel {

    override val transactionName: String = AutoClaimEnrolementTransactionName

    override val detail: Map[String, String] = Map(
      "vatNumber" -> vatNumber,
      "triggerPoint" -> triggerPoint,
      "isSuccess" -> isSuccess.toString,
      "groupId" -> groupId.getOrElse(""),
      "userIds" -> userIds.mkString(", "),
      "call" -> call.getOrElse(""),
      "reason" -> auditInformation.getOrElse("")
    ).filter { case (_, value) => value.nonEmpty }

    override val auditType: String = AutoClaimEnrolementAuditType
  }

}
