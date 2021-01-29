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

package uk.gov.hmrc.vatsignup.httpparsers

import play.api.http.Status._
import play.libs.Json
import uk.gov.hmrc.http.{HttpReads, HttpResponse}
import uk.gov.hmrc.vatsignup.models.{CheckAgentClientRelationshipResponseFailure, CheckAgentClientRelationshipSuccessResponse, HaveRelationshipResponse, NoRelationshipResponse}

object AgentClientRelationshipsHttpParser {
  type CheckAgentClientRelationshipResponse = Either[CheckAgentClientRelationshipResponseFailure, CheckAgentClientRelationshipSuccessResponse]
  val NoRelationshipCode = "RELATIONSHIP_NOT_FOUND"

  implicit object CheckAgentClientRelationshipHttpReads extends HttpReads[CheckAgentClientRelationshipResponse] {
    override def read(method: String, url: String, response: HttpResponse): CheckAgentClientRelationshipResponse =
      response.status match {
        case OK => Right(HaveRelationshipResponse)
        case NOT_FOUND =>
          response.json
          if ((response.json \ "code").asOpt[String].contains(NoRelationshipCode)) Right(NoRelationshipResponse)
          else Left(CheckAgentClientRelationshipResponseFailure(NOT_FOUND, response.json))
        case status => Left(CheckAgentClientRelationshipResponseFailure(status, response.json))
      }
  }

}

