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

package uk.gov.hmrc.vatsignup.connectors

import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.vatsignup.config.AppConfig
import uk.gov.hmrc.vatsignup.httpparsers.AgentClientRelationshipsHttpParser._
import uk.gov.hmrc.vatsignup.services.AgentClientRelationshipService.{LegacyRelationship, MtdVatRelationship, Relationship}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AgentClientRelationshipConnector @Inject()(val http: HttpClient,
                                                 val applicationConfig: AppConfig) {

  def checkAgentClientRelationship(agentNumber: String,
                                   vatNumber: String,
                                   relationshipType: Relationship
                                  )(implicit hc: HeaderCarrier,
                                    ec: ExecutionContext): Future[CheckAgentClientRelationshipResponse] = {

    val relationship = relationshipType match {
      case LegacyRelationship => "HMCE-VATDEC-ORG"
      case MtdVatRelationship => "HMRC-MTD-VAT"
    }

    http.GET[CheckAgentClientRelationshipResponse](
      s"${applicationConfig.agentClientRelationshipUrl}/agent/$agentNumber/service/$relationship/client/vrn/$vatNumber/"
    )
  }

}
