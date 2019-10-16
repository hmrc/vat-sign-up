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

package uk.gov.hmrc.vatsignup.service

import play.api.test.FakeRequest
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.vatsignup.connectors.mocks.MockMigratedCustomerSignUpConnector
import uk.gov.hmrc.vatsignup.service.mocks.monitoring.MockAuditService
import uk.gov.hmrc.vatsignup.services.MigratedSignUpService
import uk.gov.hmrc.vatsignup.helpers.TestConstants._
import uk.gov.hmrc.vatsignup.models.{CustomerSignUpResponseFailure, CustomerSignUpResponseSuccess, Digital}
import uk.gov.hmrc.vatsignup.services.MigratedSignUpService.{MigratedSignUpFailure, MigratedSignUpSuccess}
import play.api.http.Status._
import uk.gov.hmrc.vatsignup.models.monitoring.SignUpAuditing.{MigratedSignUpAuditModel, SignUpAuditModel}

import scala.concurrent.ExecutionContext.Implicits.global

class MigratedSignUpServiceSpec extends UnitSpec with MockMigratedCustomerSignUpConnector with MockAuditService {

  object TestMigratedSignUpService extends MigratedSignUpService(
    mockMigratedCustomerSignUpConnector,
    mockAuditService
  )

  implicit val hc: HeaderCarrier = HeaderCarrier()
  implicit val req = FakeRequest()

  "The migrated sign up service" when {
    "the user is the principal" when {
      "there is a successful response from DES" should {
        "return MigratedSignUpSuccess" in {
          mockSignUpMigrated(testSafeId, testVatNumber, true)(
            Right(CustomerSignUpResponseSuccess)
          )

          val res = await(TestMigratedSignUpService.signUp(testSafeId, testVatNumber, None, true))

          verifyAudit(MigratedSignUpAuditModel(
            safeId = testSafeId,
            vatNumber = testVatNumber,
            agentReferenceNumber = None,
            isSuccess = true
          ))

          res shouldBe Right(MigratedSignUpSuccess)
        }
      }
      "there is a failure response from DES" should {
        "return MigratedSignUpFailure with the DES status code" in {
          mockSignUpMigrated(testSafeId, testVatNumber, true)(
            Left(CustomerSignUpResponseFailure(BAD_REQUEST, "failure reason"))
          )

          val res = await(TestMigratedSignUpService.signUp(testSafeId, testVatNumber, None, true))

          verifyAudit(MigratedSignUpAuditModel(
            safeId = testSafeId,
            vatNumber = testVatNumber,
            agentReferenceNumber = None,
            isSuccess = false
          ))

          res shouldBe Left(MigratedSignUpFailure(BAD_REQUEST, "failure reason"))
        }
      }
    }
    "the user is an agent" when {
      "there is a successful response from DES" should {
        "return MigratedSignUpSuccess" in {
          mockSignUpMigrated(testSafeId, testVatNumber, true)(
            Right(CustomerSignUpResponseSuccess)
          )

          val res = await(TestMigratedSignUpService.signUp(testSafeId, testVatNumber, Some(testAgentReferenceNumber), true))

          verifyAudit(MigratedSignUpAuditModel(
            safeId = testSafeId,
            vatNumber = testVatNumber,
            agentReferenceNumber = Some(testAgentReferenceNumber),
            isSuccess = true
          ))

          res shouldBe Right(MigratedSignUpSuccess)
        }
      }
      "there is a failure response from DES" should {
        "return MigratedSignUpFailure with the DES status code" in {
          mockSignUpMigrated(testSafeId, testVatNumber, true)(
            Left(CustomerSignUpResponseFailure(BAD_REQUEST, "failure reason"))
          )

          val res = await(TestMigratedSignUpService.signUp(testSafeId, testVatNumber, Some(testAgentReferenceNumber), true))

          verifyAudit(MigratedSignUpAuditModel(
            safeId = testSafeId,
            vatNumber = testVatNumber,
            agentReferenceNumber = Some(testAgentReferenceNumber),
            isSuccess = false
          ))

          res shouldBe Left(MigratedSignUpFailure(BAD_REQUEST, "failure reason"))
        }
      }
    }
  }

}
