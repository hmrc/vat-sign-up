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

package uk.gov.hmrc.vatsignup.service

import java.util.UUID

import play.api.http.Status.INTERNAL_SERVER_ERROR
import play.api.libs.json.Json
import play.api.test.Helpers._
import play.api.test.FakeRequest
import reactivemongo.api.commands.UpdateWriteResult
import uk.gov.hmrc.auth.core.Enrolments
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.HeaderCarrierConverter
import org.scalatest.{WordSpec, Matchers}
import uk.gov.hmrc.vatsignup.config.mocks.MockConfig
import uk.gov.hmrc.vatsignup.connectors.mocks.{MockAgentClientRelationshipConnector}
import uk.gov.hmrc.vatsignup.helpers.TestConstants
import uk.gov.hmrc.vatsignup.helpers.TestConstants._
import uk.gov.hmrc.vatsignup.httpparsers.GetMandationStatusHttpParser.MigrationInProgress
import uk.gov.hmrc.vatsignup.models._
import uk.gov.hmrc.vatsignup.models.monitoring.AgentClientRelationshipAuditing.AgentClientRelationshipAuditModel
import uk.gov.hmrc.vatsignup.repositories.mocks.MockSubscriptionRequestRepository
import uk.gov.hmrc.vatsignup.service.mocks.monitoring.MockAuditService
import uk.gov.hmrc.vatsignup.service.mocks.{MockControlListEligibilityService, MockKnownFactsMatchingService}
import uk.gov.hmrc.vatsignup.services.ControlListEligibilityService.EligibilitySuccess
import uk.gov.hmrc.vatsignup.services.KnownFactsMatchingService.{KnownFactsMatch, KnownFactsMismatch}
import uk.gov.hmrc.vatsignup.services.StoreVatNumberService._
import uk.gov.hmrc.vatsignup.services._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class StoreVatNumberServiceSpec
  extends WordSpec with Matchers with MockAgentClientRelationshipConnector with MockSubscriptionRequestRepository
    with MockAuditService
    with MockControlListEligibilityService with MockKnownFactsMatchingService {

  object TestStoreVatNumberService extends StoreVatNumberService(
    mockSubscriptionRequestRepository,
    mockAgentClientRelationshipConnector,
    mockControlListEligibilityService,
    mockKnownFactsMatchingService,
    mockAuditService
  )

  implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromHeadersAndSession(FakeRequest().headers)
  implicit val request = FakeRequest("POST", "testUrl")

  val agentUser = Enrolments(Set(testAgentEnrolment))
  val principalUser = Enrolments(Set(testPrincipalEnrolment))
  val freshUser = Enrolments(Set.empty)

  "storeVatNumber" when {
    "the user is an agent" when {
      "there is an agent-client relationship" when {
        "the vat number is not already subscribed for MTD-VAT" when {
          "the VAT number is eligible for MTD" when {
            "the vat number is stored successfully" should {
              "return a StoreVatNumberSuccess" in {
                mockCheckAgentClientRelationship(testAgentReferenceNumber, testVatNumber, testLegacyRelationship)(
                  Future.successful(Right(HaveRelationshipResponse))
                )
                mockGetEligibilityStatus(testVatNumber)(Future.successful(
                  Right(EligibilitySuccess(testTwoKnownFacts, isMigratable = true, isOverseas = false, isDirectDebit = false))
                ))
                mockUpsertVatNumber(testVatNumber, isMigratable = true, isDirectDebit = false)(Future.successful(mock[UpdateWriteResult]))

                val res = await(TestStoreVatNumberService.storeVatNumber(testVatNumber, agentUser, None))
                res shouldBe Right(StoreVatNumberSuccess(isOverseas = false, isDirectDebit = false))

                verifyAudit(AgentClientRelationshipAuditModel(TestConstants.testVatNumber, TestConstants.testAgentReferenceNumber, haveRelationship = true))
              }
            }
            "the vat number is not stored successfully" should {
              "return a VatNumberDatabaseFailure" in {
                mockCheckAgentClientRelationship(testAgentReferenceNumber, testVatNumber, testLegacyRelationship)(
                  Future.successful(Right(HaveRelationshipResponse))
                )
                mockGetEligibilityStatus(testVatNumber)(Future.successful(
                  Right(EligibilitySuccess(testTwoKnownFacts, isMigratable = true, isOverseas = false, isDirectDebit = false))
                ))
                mockUpsertVatNumber(testVatNumber, isMigratable = true, isDirectDebit = false)(Future.failed(new Exception))

                val res = await(TestStoreVatNumberService.storeVatNumber(testVatNumber, agentUser, None))
                res shouldBe Left(VatNumberDatabaseFailure)

                verifyAudit(AgentClientRelationshipAuditModel(TestConstants.testVatNumber, TestConstants.testAgentReferenceNumber, haveRelationship = true))
              }
            }
          }
        }
        "the VAT number is eligible but non migratable" should {
          "return StoreVatNumberSuccess" in {
            mockCheckAgentClientRelationship(testAgentReferenceNumber, testVatNumber, testLegacyRelationship)(
              Future.successful(Right(HaveRelationshipResponse))
            )
            mockGetEligibilityStatus(testVatNumber)(Future.successful(
              Right(EligibilitySuccess(testTwoKnownFacts, isMigratable = false, isOverseas = false, isDirectDebit = false))
            ))
            mockUpsertVatNumber(testVatNumber, isMigratable = false, isDirectDebit = false)(Future.successful(mock[UpdateWriteResult]))

            val res = await(TestStoreVatNumberService.storeVatNumber(testVatNumber, agentUser, None))
            res shouldBe Right(StoreVatNumberSuccess(isOverseas = false, isDirectDebit = false))

            verifyAudit(AgentClientRelationshipAuditModel(TestConstants.testVatNumber, TestConstants.testAgentReferenceNumber, haveRelationship = true))
          }
        }
        "the VAT number is not eligible for MTD" should {
          "return Ineligible" in {
            mockCheckAgentClientRelationship(testAgentReferenceNumber, testVatNumber, testLegacyRelationship)(
              Future.successful(Right(HaveRelationshipResponse))
            )
            mockGetEligibilityStatus(testVatNumber)(
              Future.successful(Left(ControlListEligibilityService.IneligibleVatNumber(testMigratableDates)))
            )

            val res = await(TestStoreVatNumberService.storeVatNumber(testVatNumber, agentUser, None))
            res shouldBe Left(StoreVatNumberService.Ineligible(testMigratableDates))

            verifyAudit(AgentClientRelationshipAuditModel(TestConstants.testVatNumber, TestConstants.testAgentReferenceNumber, haveRelationship = true))
          }
        }
      }

      "there is not an agent-client-relationship" should {
        "return a RelationshipNotFound" in {
          mockCheckAgentClientRelationship(testAgentReferenceNumber, testVatNumber, testLegacyRelationship)(
            Future.successful(Right(NoRelationshipResponse))
          )

          val res = await(TestStoreVatNumberService.storeVatNumber(testVatNumber, agentUser, None))
          res shouldBe Left(RelationshipNotFound)

          verifyAudit(AgentClientRelationshipAuditModel(TestConstants.testVatNumber, TestConstants.testAgentReferenceNumber, haveRelationship = false))
        }
      }

      "the call to agent-client-relationships-fails" should {
        "return an AgentServicesConnectionFailure" in {
          mockCheckAgentClientRelationship(testAgentReferenceNumber, testVatNumber, testLegacyRelationship)(
            Future.successful(Left(CheckAgentClientRelationshipResponseFailure(INTERNAL_SERVER_ERROR, Json.obj())))
          )

          val res = await(TestStoreVatNumberService.storeVatNumber(testVatNumber, agentUser, None))
          res shouldBe Left(AgentServicesConnectionFailure)
        }
      }
    }

    "the user is a principal user" when {
      "the user has an existing HMCE-VAT enrolment" when {
        "the vat number is not already subscribed for MTD-VAT" when {
          "the vat number is stored successfully" should {
            "return StoreVatNumberSuccess" in {
              mockGetEligibilityStatus(testVatNumber)(Future.successful(
                Right(EligibilitySuccess(testTwoKnownFacts, isMigratable = true, isOverseas = false, isDirectDebit = false))
              ))
              mockUpsertVatNumber(testVatNumber, isMigratable = true, isDirectDebit = false)(Future.successful(mock[UpdateWriteResult]))

              val res = await(TestStoreVatNumberService.storeVatNumber(testVatNumber, principalUser, None))
              res shouldBe Right(StoreVatNumberSuccess(isOverseas = false, isDirectDebit = false))
            }
          }
          "the vat number is not stored successfully" should {
            "return a VatNumberDatabaseFailure" in {
              mockGetEligibilityStatus(testVatNumber)(Future.successful(
                Right(EligibilitySuccess(testTwoKnownFacts, isMigratable = true, isOverseas = false, isDirectDebit = false))
              ))
              mockUpsertVatNumber(testVatNumber, isMigratable = true, isDirectDebit = false)(Future.failed(new Exception))

              val res = await(TestStoreVatNumberService.storeVatNumber(testVatNumber, principalUser, None))
              res shouldBe Left(VatNumberDatabaseFailure)
            }
          }
        }
        "the vat number does not match enrolment" should {
          "return DoesNotMatchEnrolment" in {
            val res = await(TestStoreVatNumberService.storeVatNumber(UUID.randomUUID().toString, principalUser, None))
            res shouldBe Left(DoesNotMatchEnrolment)
          }
        }
      }

      "the user does not have either enrolment" should {
        "return InsufficientEnrolments" in {
          val res = await(TestStoreVatNumberService.storeVatNumber(testVatNumber, Enrolments(Set.empty), None))
          res shouldBe Left(InsufficientEnrolments)
        }
      }
    }

    "the user has a fresh cred" when {

      def call = TestStoreVatNumberService.storeVatNumber(
        testVatNumber,
        freshUser,
        Some(VatKnownFacts(Some(testPostCode), testDateOfRegistration, None, None))
      )

      "the vat number is not already subscribed for MTD-VAT" when {
        "the vat number is stored successfully" should {
          "return StoreVatNumberSuccess" in {
            mockGetEligibilityStatus(testVatNumber)(Future.successful(
              Right(EligibilitySuccess(testTwoKnownFacts, isMigratable = true, isOverseas = false, isDirectDebit = false))
            ))
            mockKnownFactsMatching(testVatNumber, testTwoKnownFacts, testTwoKnownFacts)(Right(KnownFactsMatch))
            mockUpsertVatNumber(testVatNumber, isMigratable = true, isDirectDebit = false)(Future.successful(mock[UpdateWriteResult]))

            val res = await(call)
            res shouldBe Right(StoreVatNumberSuccess(isOverseas = false, isDirectDebit = false))
          }
        }
        "Known facts and control list returned ineligible" should {
          "return a Ineligible" in {
            mockGetEligibilityStatus(testVatNumber)(Future.successful(
              Left(ControlListEligibilityService.IneligibleVatNumber(testMigratableDates))
            ))

            val res = await(call)
            res shouldBe Left(StoreVatNumberService.Ineligible(testMigratableDates))
          }
        }
        "Known facts and control list returned ControlListInformationVatNumberNotFound" should {
          "return a VatNotFound" in {
            mockGetEligibilityStatus(testVatNumber)(Future.successful(
              Left(ControlListEligibilityService.VatNumberNotFound)
            ))

            val res = await(call)
            res shouldBe Left(VatNotFound)
          }
        }
        "Known facts and control list returned KnownFactsInvalidVatNumber" should {
          "return a VatInvalid" in {
            mockGetEligibilityStatus(testVatNumber)(Future.successful(
              Left(ControlListEligibilityService.InvalidVatNumber))
            )

            val res = await(call)
            res shouldBe Left(VatInvalid)
          }
        }
        "known facts returned from api differs from known facts by the user" should {
          "return a KnownFactsMismatch" in {
            mockGetEligibilityStatus(testVatNumber)(Future.successful(
              Right(EligibilitySuccess(testTwoKnownFacts, isMigratable = true, isOverseas = false, isDirectDebit = false))
            ))
            mockKnownFactsMatching(testVatNumber, testTwoKnownFacts, testTwoKnownFacts)(Left(KnownFactsMismatch))

            val res = await(call)
            res shouldBe Left(StoreVatNumberService.KnownFactsMismatch)
          }
        }
        "the vat number is not stored successfully" should {
          "return a VatNumberDatabaseFailure" in {
            mockGetEligibilityStatus(testVatNumber)(Future.successful(
              Right(EligibilitySuccess(testTwoKnownFacts, isMigratable = true, isOverseas = false, isDirectDebit = false))
            ))
            mockKnownFactsMatching(testVatNumber, testTwoKnownFacts, testTwoKnownFacts)(Right(KnownFactsMatch))
            mockUpsertVatNumber(testVatNumber, isMigratable = true, isDirectDebit = false)(Future.failed(new Exception))

            val res = await(call)
            res shouldBe Left(VatNumberDatabaseFailure)
          }
        }
      }
      "the user does not have either enrolment and did not provide both known facts" should {
        "return InsufficientEnrolments" in {
          val res = await(TestStoreVatNumberService.storeVatNumber(testVatNumber, freshUser, None))
          res shouldBe Left(InsufficientEnrolments)
        }
      }
    }

    "the user does not have either enrolment" should {
      "return InsufficientEnrolments" in {
        val res = await(TestStoreVatNumberService.storeVatNumber(testVatNumber, freshUser, None))
        res shouldBe Left(InsufficientEnrolments)
      }
    }
  }

}
