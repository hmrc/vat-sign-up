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

package uk.gov.hmrc.vatsignup.helpers.servicemocks

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.libs.json.Writes
import uk.gov.hmrc.vatsignup.services.AgentClientRelationshipService.{LegacyRelationship, MtdVatRelationship, Relationship}

object AgentClientRelationshipsStub extends WireMockMethods {
  def checkAgentClientRelationship(agentNumber: String, vatNumber: String, relationship: String): String =
    s"/agent-client-relationships/agent/$agentNumber/service/$relationship/client/vrn/$vatNumber/"

  def stubCheckAgentClientRelationship[T](agentNumber: String,
                                          vatNumber: String,
                                          relationshipType: Relationship)(status: Int, body: T)(implicit writes: Writes[T]): StubMapping = {

    val relationship = relationshipType match {
      case LegacyRelationship => "HMCE-VATDEC-ORG"
      case MtdVatRelationship => "HMRC-MTD-VAT"
    }

    when(method = GET, uri = checkAgentClientRelationship(agentNumber, vatNumber, relationship))
      .thenReturn(status = status, body = writes.writes(body))
  }
}
