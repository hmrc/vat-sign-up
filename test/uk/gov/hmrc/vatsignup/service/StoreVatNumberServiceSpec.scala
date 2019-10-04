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

import java.util.UUID

import play.api.test.FakeRequest
import reactivemongo.api.commands.UpdateWriteResult
import uk.gov.hmrc.auth.core.Enrolments
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.HeaderCarrierConverter
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.vatsignup.config.mocks.MockConfig
import uk.gov.hmrc.vatsignup.connectors.mocks.MockMandationStatusConnector
import uk.gov.hmrc.vatsignup.helpers.TestConstants._
import uk.gov.hmrc.vatsignup.httpparsers.GetMandationStatusHttpParser.MigrationInProgress
import uk.gov.hmrc.vatsignup.models._
import uk.gov.hmrc.vatsignup.repositories.mocks.MockSubscriptionRequestRepository
import uk.gov.hmrc.vatsignup.service.mocks.{MockAgentClientRelationshipService, MockControlListEligibilityService, MockKnownFactsMatchingService}
import uk.gov.hmrc.vatsignup.services.AgentClientRelationshipService.{RelationshipCheckError, RelationshipCheckNotFound, RelationshipCheckSuccess}
import uk.gov.hmrc.vatsignup.services.ControlListEligibilityService.EligibilitySuccess
import uk.gov.hmrc.vatsignup.services.KnownFactsMatchingService.{KnownFactsMatch, KnownFactsMismatch}
import uk.gov.hmrc.vatsignup.services.StoreVatNumberService._
import uk.gov.hmrc.vatsignup.services._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class StoreVatNumberServiceSpec
  extends UnitSpec with MockSubscriptionRequestRepository with MockConfig with MockMandationStatusConnector
    with MockControlListEligibilityService with MockKnownFactsMatchingService with MockAgentClientRelationshipService {

  object TestStoreVatNumberService extends StoreVatNumberService(
    mockSubscriptionRequestRepository,
    mockMandationStatusConnector,
    mockControlListEligibilityService,
    mockKnownFactsMatchingService,
    mockAgentClientRelationshipService,
    mockConfig
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
                mockCheckAgentClientRelationship(testVatNumber, testAgentReferenceNumber)(
                  Future.successful(Right(RelationshipCheckSuccess))
                )
                mockGetMandationStatus(testVatNumber)(Future.successful(Right(NonMTDfB)))
                mockGetEligibilityStatus(testVatNumber)(Future.successful(
                  Right(EligibilitySuccess(testTwoKnownFacts, isMigratable = true, isOverseas = false, isDirectDebit = false))
                ))
                mockUpsertVatNumber(testVatNumber, isMigratable = true, isDirectDebit = false)(Future.successful(mock[UpdateWriteResult]))

                val res = await(TestStoreVatNumberService.storeVatNumber(testVatNumber, agentUser, None))
                res shouldBe Right(StoreVatNumberSuccess(isOverseas = false, isDirectDebit = false))
              }
            }
            "the vat number is not stored successfully" should {
              "return a VatNumberDatabaseFailure" in {
                mockCheckAgentClientRelationship(testVatNumber, testAgentReferenceNumber)(
                  Future.successful(Right(RelationshipCheckSuccess))
                )
                mockGetMandationStatus(testVatNumber)(Future.successful(Right(NonDigital)))
                mockGetEligibilityStatus(testVatNumber)(Future.successful(
                  Right(EligibilitySuccess(testTwoKnownFacts, isMigratable = true, isOverseas = false, isDirectDebit = false))
                ))
                mockUpsertVatNumber(testVatNumber, isMigratable = true, isDirectDebit = false)(Future.failed(new Exception))

                val res = await(TestStoreVatNumberService.storeVatNumber(testVatNumber, agentUser, None))
                res shouldBe Left(VatNumberDatabaseFailure)
              }
            }
          }
          "the VAT number is already voluntarily subscribed for MTD-VAT" should {
            "return AlreadySubscribed" in {
              mockCheckAgentClientRelationship(testVatNumber, testAgentReferenceNumber)(
                Future.successful(Right(RelationshipCheckSuccess))
              )
              mockGetMandationStatus(testVatNumber)(Future.successful(Right(MTDfBVoluntary)))

              val res = await(TestStoreVatNumberService.storeVatNumber(testVatNumber, agentUser, None))
              res shouldBe Left(AlreadySubscribed)
            }
          }
          "the VAT number is already mandated for MTD-VAT" should {
            "return AlreadySubscribed" in {
              mockCheckAgentClientRelationship(testVatNumber, testAgentReferenceNumber)(
                Future.successful(Right(RelationshipCheckSuccess))
              )
              mockGetMandationStatus(testVatNumber)(Future.successful(Right(MTDfBMandated)))

              val res = await(TestStoreVatNumberService.storeVatNumber(testVatNumber, agentUser, None))
              res shouldBe Left(AlreadySubscribed)
            }
          }
          "the VAT number migration is already in progress for MTD-VAT" should {
            "return VatMigrationInProgress" in {
              mockCheckAgentClientRelationship(testVatNumber, testAgentReferenceNumber)(
                Future.successful(Right(RelationshipCheckSuccess))
              )
              mockGetMandationStatus(testVatNumber)(Future.successful(Left(MigrationInProgress)))

              val res = await(TestStoreVatNumberService.storeVatNumber(testVatNumber, agentUser, None))
              res shouldBe Left(VatMigrationInProgress)
            }
          }
        }
        "the VAT number is eligible but non migratable" should {
          "return StoreVatNumberSuccess" in {
            mockCheckAgentClientRelationship(testVatNumber, testAgentReferenceNumber)(
              Future.successful(Right(RelationshipCheckSuccess))
            )
            mockGetMandationStatus(testVatNumber)(Future.successful(Right(NonMTDfB)))
            mockGetEligibilityStatus(testVatNumber)(Future.successful(
              Right(EligibilitySuccess(testTwoKnownFacts, isMigratable = false, isOverseas = false, isDirectDebit = false))
            ))
            mockUpsertVatNumber(testVatNumber, isMigratable = false, isDirectDebit = false)(Future.successful(mock[UpdateWriteResult]))

            val res = await(TestStoreVatNumberService.storeVatNumber(testVatNumber, agentUser, None))
            res shouldBe Right(StoreVatNumberSuccess(isOverseas = false, isDirectDebit = false))
          }
        }
        "the VAT number is not eligible for MTD" should {
          "return Ineligible" in {
            mockCheckAgentClientRelationship(testVatNumber, testAgentReferenceNumber)(
              Future.successful(Right(RelationshipCheckSuccess))
            )
            mockGetMandationStatus(testVatNumber)(Future.successful(Right(NonMTDfB)))
            mockGetEligibilityStatus(testVatNumber)(
              Future.successful(Left(ControlListEligibilityService.IneligibleVatNumber(testMigratableDates)))
            )

            val res = await(TestStoreVatNumberService.storeVatNumber(testVatNumber, agentUser, None))
            res shouldBe Left(StoreVatNumberService.Ineligible(testMigratableDates))
          }
        }
      }

      "the AgentClientRelationshipService does not find a relationship" should {
        "return a RelationshipNotFound" in {
          mockCheckAgentClientRelationship(testVatNumber, testAgentReferenceNumber)(
            Future.successful(Left(RelationshipCheckNotFound))
          )

          val res = await(TestStoreVatNumberService.storeVatNumber(testVatNumber, agentUser, None))
          res shouldBe Left(RelationshipNotFound)
        }
      }

      "the AgentClientRelationshipService fails on the relationship check" should {
        "return an AgentServicesConnectionFailure" in {
          mockCheckAgentClientRelationship(testVatNumber, testAgentReferenceNumber)(
            Future.successful(Left(RelationshipCheckError))
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
              mockGetMandationStatus(testVatNumber)(Future.successful(Right(NonMTDfB)))
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
              mockGetMandationStatus(testVatNumber)(Future.successful(Right(NonDigital)))
              mockGetEligibilityStatus(testVatNumber)(Future.successful(
                Right(EligibilitySuccess(testTwoKnownFacts, isMigratable = true, isOverseas = false, isDirectDebit = false))
              ))
              mockUpsertVatNumber(testVatNumber, isMigratable = true, isDirectDebit = false)(Future.failed(new Exception))

              val res = await(TestStoreVatNumberService.storeVatNumber(testVatNumber, principalUser, None))
              res shouldBe Left(VatNumberDatabaseFailure)
            }
          }
        }
        "the vat number is currently being migrated" should {
          "return a VatMigrationInProgress" in {
            mockGetMandationStatus(testVatNumber)(Future.successful(Left(MigrationInProgress)))
            mockGetEligibilityStatus(testVatNumber)(Future.successful(
              Right(EligibilitySuccess(testTwoKnownFacts, isMigratable = true, isOverseas = false, isDirectDebit = false))
            ))

            val res = await(TestStoreVatNumberService.storeVatNumber(testVatNumber, principalUser, None))
            res shouldBe Left(VatMigrationInProgress)
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
            mockGetMandationStatus(testVatNumber)(Future.successful(Right(NonMTDfB)))
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
            mockGetMandationStatus(testVatNumber)(Future.successful(Right(NonMTDfB)))
            mockGetEligibilityStatus(testVatNumber)(Future.successful(
              Left(ControlListEligibilityService.IneligibleVatNumber(testMigratableDates))
            ))

            val res = await(call)
            res shouldBe Left(StoreVatNumberService.Ineligible(testMigratableDates))
          }
        }
        "Known facts and control list returned ControlListInformationVatNumberNotFound" should {
          "return a VatNotFound" in {
            mockGetMandationStatus(testVatNumber)(Future.successful(Right(NonMTDfB)))
            mockGetEligibilityStatus(testVatNumber)(Future.successful(
              Left(ControlListEligibilityService.VatNumberNotFound)
            ))

            val res = await(call)
            res shouldBe Left(VatNotFound)
          }
        }
        "Known facts and control list returned KnownFactsInvalidVatNumber" should {
          "return a VatInvalid" in {
            mockGetMandationStatus(testVatNumber)(Future.successful(Right(NonMTDfB)))
            mockGetEligibilityStatus(testVatNumber)(Future.successful(
              Left(ControlListEligibilityService.InvalidVatNumber))
            )

            val res = await(call)
            res shouldBe Left(VatInvalid)
          }
        }
        "known facts returned from api differs from known facts by the user" should {
          "return a KnownFactsMismatch" in {
            mockGetMandationStatus(testVatNumber)(Future.successful(Right(NonMTDfB)))
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
            mockGetMandationStatus(testVatNumber)(Future.successful(Right(NonDigital)))
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
      "the vat number migration is currently in progress" should {
        "return a VatMigrationInProgress" in {
          mockGetMandationStatus(testVatNumber)(Future.successful(Left(MigrationInProgress)))
          mockGetEligibilityStatus(testVatNumber)(Future.successful(
            Right(EligibilitySuccess(testTwoKnownFacts, isMigratable = true, isOverseas = false, isDirectDebit = false))
          ))

          val res = await(TestStoreVatNumberService.storeVatNumber(testVatNumber, principalUser, None))
          res shouldBe Left(VatMigrationInProgress)
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
