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
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}
import uk.gov.hmrc.vatsignup.connectors.mocks.MockMigratedCustomerSignUpConnector
import uk.gov.hmrc.vatsignup.helpers.TestConstants._
import uk.gov.hmrc.vatsignup.models.monitoring.SignUpAuditing.MigratedSignUpAuditModel
import uk.gov.hmrc.vatsignup.models.{CustomerSignUpResponseFailure, CustomerSignUpResponseSuccess}
import uk.gov.hmrc.vatsignup.service.mocks.monitoring.MockAuditService
import uk.gov.hmrc.vatsignup.services.MigratedSignUpService
import uk.gov.hmrc.vatsignup.services.MigratedSignUpService.MigratedSignUpSuccess

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class MigratedSignUpServiceSpec extends WordSpec with Matchers with MockMigratedCustomerSignUpConnector with MockAuditService {

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
          mockSignUpMigrated(testSafeId, testVatNumber, isPartialMigration = true)(
            Future.successful(Right(CustomerSignUpResponseSuccess))
          )

          val res = await(TestMigratedSignUpService.signUp(testSafeId, testVatNumber, isPartialMigration = true, None))

          verifyAudit(MigratedSignUpAuditModel(
            safeId = testSafeId,
            vatNumber = testVatNumber,
            agentReferenceNumber = None,
            isSuccess = true
          ))

          res shouldBe MigratedSignUpSuccess
        }
      }
      "there is a failure response from DES" should {
        "return MigratedSignUpFailure with the DES status code" in {
          mockSignUpMigrated(testSafeId, testVatNumber, isPartialMigration = true)(
            Future.successful(Left(CustomerSignUpResponseFailure(BAD_REQUEST, "failure reason")))
          )

          intercept[InternalServerException] {
            await(TestMigratedSignUpService.signUp(testSafeId, testVatNumber, isPartialMigration = true, None))
          }

          verifyAudit(MigratedSignUpAuditModel(
            safeId = testSafeId,
            vatNumber = testVatNumber,
            agentReferenceNumber = None,
            isSuccess = false
          ))
        }
      }
    }
    "the user is an agent" when {
      "there is a successful response from DES" should {
        "return MigratedSignUpSuccess" in {
          mockSignUpMigrated(testSafeId, testVatNumber, isPartialMigration = true)(
            Future.successful(Right(CustomerSignUpResponseSuccess))
          )

          val res = await(TestMigratedSignUpService.signUp(testSafeId, testVatNumber, isPartialMigration = true, Some(testAgentReferenceNumber)))

          verifyAudit(MigratedSignUpAuditModel(
            safeId = testSafeId,
            vatNumber = testVatNumber,
            agentReferenceNumber = Some(testAgentReferenceNumber),
            isSuccess = true
          ))

          res shouldBe MigratedSignUpSuccess
        }
      }
      "there is a failure response from DES" should {
        "return MigratedSignUpFailure with the DES status code" in {
          mockSignUpMigrated(testSafeId, testVatNumber, isPartialMigration = true)(
            Future.successful(Left(CustomerSignUpResponseFailure(BAD_REQUEST, "failure reason")))
          )

          intercept[InternalServerException] {
            await(TestMigratedSignUpService.signUp(testSafeId, testVatNumber, isPartialMigration = true, Some(testAgentReferenceNumber)))
          }

          verifyAudit(MigratedSignUpAuditModel(
            safeId = testSafeId,
            vatNumber = testVatNumber,
            agentReferenceNumber = Some(testAgentReferenceNumber),
            isSuccess = false
          ))
        }
      }
    }
  }

}
