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

import uk.gov.hmrc.vatsignup.services.monitoring.AuditModel

object AgentClientRelationshipAuditing {
  val agentClientRelationshipTransactionName = "VATAgentClientRelationshipRequest"
  val agentClientRelationshipAuditType = "AgentClientRelationshipCheckSubmitted"

  case class AgentClientRelationshipAuditModel(vatNumber: String, agentReferenceNumber: String, haveRelationship: Boolean) extends AuditModel {
    override val transactionName: String = agentClientRelationshipTransactionName
    override val detail: Map[String, String] = Map(
      "vatNumber" -> vatNumber,
      "agentReferenceNumber" -> agentReferenceNumber,
      "haveRelationship" -> s"$haveRelationship"
    )
    override val auditType: String = agentClientRelationshipAuditType
  }
}