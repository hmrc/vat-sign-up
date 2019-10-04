/*
 * Copyright 2019 HM Revenue & Customs
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

package uk.gov.hmrc.vatsignup.connectors.mocks

import org.mockito.ArgumentMatchers
import org.mockito.Mockito.reset
import org.scalatest.{BeforeAndAfterEach, Suite}
import org.scalatest.mockito.MockitoSugar
import uk.gov.hmrc.vatsignup.connectors.AgentClientRelationshipConnector
import uk.gov.hmrc.vatsignup.httpparsers.AgentClientRelationshipsHttpParser.CheckAgentClientRelationshipResponse
import org.mockito.Mockito._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.vatsignup.services.AgentClientRelationshipService.{LegacyRelationship, MtdVatRelationship, Relationship}

import scala.concurrent.{ExecutionContext, Future}

trait MockAgentClientRelationshipConnector extends MockitoSugar with BeforeAndAfterEach {
  this: Suite =>

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockAgentClientRelationshipConnector)
  }

  val mockAgentClientRelationshipConnector: AgentClientRelationshipConnector = mock[AgentClientRelationshipConnector]

  def mockCheckAgentClientRelationship(agentNumber: String,
                                       vatNumber: String,
                                       relationshipType: Relationship)(response: Future[CheckAgentClientRelationshipResponse]): Unit = {

    when(mockAgentClientRelationshipConnector.checkAgentClientRelationship(
      ArgumentMatchers.eq(agentNumber),
      ArgumentMatchers.eq(vatNumber),
      ArgumentMatchers.eq(relationshipType)
    )(
      ArgumentMatchers.any[HeaderCarrier],
      ArgumentMatchers.any[ExecutionContext]
    )) thenReturn response
  }
}