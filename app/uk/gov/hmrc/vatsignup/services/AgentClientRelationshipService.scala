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

package uk.gov.hmrc.vatsignup.services

import javax.inject.{Inject, Singleton}
import play.api.mvc.Request
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.vatsignup.connectors.AgentClientRelationshipConnector
import uk.gov.hmrc.vatsignup.models.monitoring.AgentClientRelationshipAuditing.AgentClientRelationshipAuditModel
import uk.gov.hmrc.vatsignup.models.{HaveRelationshipResponse, NoRelationshipResponse}
import uk.gov.hmrc.vatsignup.services.AgentClientRelationshipService._
import uk.gov.hmrc.vatsignup.services.monitoring.AuditService

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AgentClientRelationshipService @Inject()(agentClientRelationshipsConnector: AgentClientRelationshipConnector,
                                               auditService: AuditService
                                              )(implicit ex: ExecutionContext) {

  def checkAgentClientRelationship(vatNumber: String,
                                   agentReferenceNumber: String
                                  )(implicit hc: HeaderCarrier, request: Request[_]): Future[RelationshipCheckResponse] = {
    agentClientRelationshipsConnector.checkAgentClientRelationship(agentReferenceNumber, vatNumber, LegacyRelationship) flatMap {
      case Right(NoRelationshipResponse) =>
        agentClientRelationshipsConnector.checkAgentClientRelationship(agentReferenceNumber, vatNumber, MtdVatRelationship)
      case response => Future.successful(response)
    } map {
      case Right(HaveRelationshipResponse) =>
        auditService.audit(AgentClientRelationshipAuditModel(vatNumber, agentReferenceNumber, haveRelationship = true))
        Right(RelationshipCheckSuccess)
      case Right(NoRelationshipResponse) =>
        auditService.audit(AgentClientRelationshipAuditModel(vatNumber, agentReferenceNumber, haveRelationship = false))
        Left(RelationshipCheckNotFound)
      case Left(_) =>
        Left(RelationshipCheckError)
    }
  }

}

object AgentClientRelationshipService {

  sealed trait Relationship

  case object LegacyRelationship extends Relationship

  case object MtdVatRelationship extends Relationship

  type RelationshipCheckResponse = Either[RelationshipCheckFailure, RelationshipCheckSuccess.type]

  case object RelationshipCheckSuccess

  sealed trait RelationshipCheckFailure

  case object RelationshipCheckError extends RelationshipCheckFailure

  case object RelationshipCheckNotFound extends RelationshipCheckFailure

}