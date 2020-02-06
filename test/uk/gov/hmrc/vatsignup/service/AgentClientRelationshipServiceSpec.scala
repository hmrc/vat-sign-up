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

package uk.gov.hmrc.vatsignup.service

import org.scalatest.{Matchers, WordSpec}
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.HeaderCarrierConverter
import uk.gov.hmrc.vatsignup.connectors.mocks.MockAgentClientRelationshipConnector
import uk.gov.hmrc.vatsignup.helpers.TestConstants
import uk.gov.hmrc.vatsignup.helpers.TestConstants.{testAgentReferenceNumber, testLegacyRelationship, testMtdVatRelationship, testVatNumber}
import uk.gov.hmrc.vatsignup.models.monitoring.AgentClientRelationshipAuditing.AgentClientRelationshipAuditModel
import uk.gov.hmrc.vatsignup.models.{CheckAgentClientRelationshipResponseFailure, HaveRelationshipResponse, NoRelationshipResponse}
import uk.gov.hmrc.vatsignup.service.mocks.monitoring.MockAuditService
import uk.gov.hmrc.vatsignup.services.AgentClientRelationshipService
import uk.gov.hmrc.vatsignup.services.AgentClientRelationshipService.{RelationshipCheckError, RelationshipCheckNotFound, RelationshipCheckSuccess}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AgentClientRelationshipServiceSpec extends WordSpec with Matchers with MockAuditService with MockAgentClientRelationshipConnector {

  object TestAgentClientRelationshipService extends AgentClientRelationshipService(
    mockAgentClientRelationshipConnector,
    mockAuditService
  )

  implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromHeadersAndSession(FakeRequest().headers)
  implicit val request = FakeRequest("POST", "testUrl")

  "AgentClientRelationshipService" should {
    "return RelationshipCheckSuccess if the legacy relationship exists" in {
      mockCheckAgentClientRelationship(testAgentReferenceNumber, testVatNumber, testLegacyRelationship)(
        Future.successful(Right(HaveRelationshipResponse))
      )

      await(TestAgentClientRelationshipService.checkAgentClientRelationship(testVatNumber, testAgentReferenceNumber)) shouldBe Right(RelationshipCheckSuccess)

      verifyAudit(AgentClientRelationshipAuditModel(TestConstants.testVatNumber, TestConstants.testAgentReferenceNumber, haveRelationship = true))
    }

    "return RelationshipCheckSuccess if the mtd vat relationship exists" in {
      mockCheckAgentClientRelationship(testAgentReferenceNumber, testVatNumber, testLegacyRelationship)(
        Future.successful(Right(NoRelationshipResponse))
      )
      mockCheckAgentClientRelationship(testAgentReferenceNumber, testVatNumber, testMtdVatRelationship)(
        Future.successful(Right(HaveRelationshipResponse))
      )

      await(TestAgentClientRelationshipService.checkAgentClientRelationship(testVatNumber, testAgentReferenceNumber)) shouldBe Right(RelationshipCheckSuccess)

      verifyAudit(AgentClientRelationshipAuditModel(TestConstants.testVatNumber, TestConstants.testAgentReferenceNumber, haveRelationship = true))
    }

    "return RelationshipCheckNotFound if both mtd vat and legacy relationships don't exist" in {
      mockCheckAgentClientRelationship(testAgentReferenceNumber, testVatNumber, testLegacyRelationship)(
        Future.successful(Right(NoRelationshipResponse))
      )
      mockCheckAgentClientRelationship(testAgentReferenceNumber, testVatNumber, testMtdVatRelationship)(
        Future.successful(Right(NoRelationshipResponse))
      )

      await(TestAgentClientRelationshipService.checkAgentClientRelationship(testVatNumber, testAgentReferenceNumber)) shouldBe Left(RelationshipCheckNotFound)

      verifyAudit(AgentClientRelationshipAuditModel(TestConstants.testVatNumber, TestConstants.testAgentReferenceNumber, haveRelationship = false))
    }

    "return RelationshipCheckError if the legacy relationship check fails" in {
      mockCheckAgentClientRelationship(testAgentReferenceNumber, testVatNumber, testLegacyRelationship)(
        Future.successful(Left(CheckAgentClientRelationshipResponseFailure(INTERNAL_SERVER_ERROR, Json.obj())))
      )

      await(TestAgentClientRelationshipService.checkAgentClientRelationship(testVatNumber, testAgentReferenceNumber)) shouldBe Left(RelationshipCheckError)
    }

    "return RelationshipCheckError if the mtd vat relationship check fails and the legacy relationship doesn't exist" in {
      mockCheckAgentClientRelationship(testAgentReferenceNumber, testVatNumber, testLegacyRelationship)(
        Future.successful(Right(NoRelationshipResponse))
      )
      mockCheckAgentClientRelationship(testAgentReferenceNumber, testVatNumber, testMtdVatRelationship)(
        Future.successful(Left(CheckAgentClientRelationshipResponseFailure(INTERNAL_SERVER_ERROR, Json.obj())))
      )

      await(TestAgentClientRelationshipService.checkAgentClientRelationship(testVatNumber, testAgentReferenceNumber)) shouldBe Left(RelationshipCheckError)
    }
  }

}
