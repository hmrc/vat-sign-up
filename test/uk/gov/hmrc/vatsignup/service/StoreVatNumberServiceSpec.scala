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

package uk.gov.hmrc.vatsignup.service

import java.util.UUID

import play.api.http.Status.INTERNAL_SERVER_ERROR
import play.api.libs.json.Json
import play.api.test.FakeRequest
import reactivemongo.api.commands.UpdateWriteResult
import uk.gov.hmrc.auth.core.Enrolments
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.HeaderCarrierConverter
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.vatsignup.config.featureswitch.AlreadySubscribedCheck
import uk.gov.hmrc.vatsignup.config.mocks.MockConfig
import uk.gov.hmrc.vatsignup.connectors.mocks.{MockAgentClientRelationshipsConnector, MockKnownFactsAndControlListInformationConnector, MockMandationStatusConnector}
import uk.gov.hmrc.vatsignup.helpers.TestConstants
import uk.gov.hmrc.vatsignup.helpers.TestConstants._
import uk.gov.hmrc.vatsignup.httpparsers.KnownFactsAndControlListInformationHttpParser.{ControlListInformationVatNumberNotFound, KnownFactsInvalidVatNumber}
import uk.gov.hmrc.vatsignup.models._
import uk.gov.hmrc.vatsignup.models.controllist.{ControlListInformation, DeRegOrDeath}
import uk.gov.hmrc.vatsignup.models.monitoring.AgentClientRelationshipAuditing.AgentClientRelationshipAuditModel
import uk.gov.hmrc.vatsignup.models.monitoring.ControlListAuditing._
import uk.gov.hmrc.vatsignup.repositories.mocks.MockSubscriptionRequestRepository
import uk.gov.hmrc.vatsignup.service.mocks.monitoring.MockAuditService
import uk.gov.hmrc.vatsignup.services.StoreVatNumberService._
import uk.gov.hmrc.vatsignup.services._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class StoreVatNumberServiceSpec
  extends UnitSpec with MockAgentClientRelationshipsConnector with MockSubscriptionRequestRepository
    with MockAuditService with MockConfig
    with MockMandationStatusConnector
    with MockKnownFactsAndControlListInformationConnector {

  object TestStoreVatNumberService extends StoreVatNumberService(
    mockSubscriptionRequestRepository,
    mockAgentClientRelationshipsConnector,
    mockMandationStatusConnector,
    mockKnownFactsAndControlListInformationConnector,
    mockAuditService,
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
                enable(AlreadySubscribedCheck)

                mockCheckAgentClientRelationship(testAgentReferenceNumber, testVatNumber)(Future.successful(Right(HaveRelationshipResponse)))
                mockGetMandationStatus(testVatNumber)(Future.successful(Right(NonMTDfB)))
                mockGetKnownFactsAndControlListInformation(testVatNumber)(Future.successful(Right(testKnownFactsAndControlListInformation)))
                mockUpsertVatNumber(testVatNumber, isMigratable = true)(Future.successful(mock[UpdateWriteResult]))

                val res = await(TestStoreVatNumberService.storeVatNumber(testVatNumber, agentUser, None, None))
                res shouldBe Right(StoreVatNumberSuccess)

                verifyAudit(AgentClientRelationshipAuditModel(TestConstants.testVatNumber, TestConstants.testAgentReferenceNumber, haveRelationship = true))
              }
            }
            "the vat number is not stored successfully" should {
              "return a VatNumberDatabaseFailure" in {
                enable(AlreadySubscribedCheck)

                mockCheckAgentClientRelationship(testAgentReferenceNumber, testVatNumber)(Future.successful(Right(HaveRelationshipResponse)))
                mockGetMandationStatus(testVatNumber)(Future.successful(Right(NonDigital)))
                mockGetKnownFactsAndControlListInformation(testVatNumber)(Future.successful(Right(testKnownFactsAndControlListInformation)))
                mockUpsertVatNumber(testVatNumber, isMigratable = true)(Future.failed(new Exception))

                val res = await(TestStoreVatNumberService.storeVatNumber(testVatNumber, agentUser, None, None))
                res shouldBe Left(VatNumberDatabaseFailure)

                verifyAudit(AgentClientRelationshipAuditModel(TestConstants.testVatNumber, TestConstants.testAgentReferenceNumber, haveRelationship = true))
              }
            }
          }
          "the VAT number is already voluntarily subscribed for MTD-VAT" should {
            "return AlreadySubscribed" in {
              enable(AlreadySubscribedCheck)

              mockCheckAgentClientRelationship(testAgentReferenceNumber, testVatNumber)(Future.successful(Right(HaveRelationshipResponse)))
              mockGetMandationStatus(testVatNumber)(Future.successful(Right(MTDfBVoluntary)))

              val res = await(TestStoreVatNumberService.storeVatNumber(testVatNumber, agentUser, None, None))
              res shouldBe Left(AlreadySubscribed)

              verifyAudit(AgentClientRelationshipAuditModel(TestConstants.testVatNumber, TestConstants.testAgentReferenceNumber, haveRelationship = true))
            }
          }
          "the VAT number is already mandated for MTD-VAT" should {
            "return AlreadySubscribed" in {
              enable(AlreadySubscribedCheck)

              mockCheckAgentClientRelationship(testAgentReferenceNumber, testVatNumber)(Future.successful(Right(HaveRelationshipResponse)))
              mockGetMandationStatus(testVatNumber)(Future.successful(Right(MTDfBMandated)))

              val res = await(TestStoreVatNumberService.storeVatNumber(testVatNumber, agentUser, None, None))
              res shouldBe Left(AlreadySubscribed)

              verifyAudit(AgentClientRelationshipAuditModel(TestConstants.testVatNumber, TestConstants.testAgentReferenceNumber, haveRelationship = true))
            }
          }
          "the already subscribed check feature switch is turned off" should {
            "always treat the user as not subscribed" in {
              disable(AlreadySubscribedCheck)

              mockCheckAgentClientRelationship(testAgentReferenceNumber, testVatNumber)(Future.successful(Right(HaveRelationshipResponse)))
              mockGetKnownFactsAndControlListInformation(testVatNumber)(Future.successful(Right(testKnownFactsAndControlListInformation)))
              mockUpsertVatNumber(testVatNumber, isMigratable = true)(Future.successful(mock[UpdateWriteResult]))

              val res = await(TestStoreVatNumberService.storeVatNumber(testVatNumber, agentUser, None, None))
              res shouldBe Right(StoreVatNumberSuccess)

              verifyAudit(AgentClientRelationshipAuditModel(TestConstants.testVatNumber, TestConstants.testAgentReferenceNumber, haveRelationship = true))
            }
          }
        }
        "the VAT number is eligible but non migratable" should {
          "return StoreVatNumberSuccess" in {
            sys.props += "control-list.eligible.stagger_1" -> "NonMigratable"

            enable(AlreadySubscribedCheck)

            val failures = testKnownFactsAndControlListInformation.controlListInformation.validate(mockConfig.eligibilityConfig)
            val nonMigratableReasons = failures match {
              case Right(uk.gov.hmrc.vatsignup.models.controllist.NonMigratable(err)) => err.toList.map(_.toString)
            }

            mockCheckAgentClientRelationship(testAgentReferenceNumber, testVatNumber)(Future.successful(Right(HaveRelationshipResponse)))
            mockGetMandationStatus(testVatNumber)(Future.successful(Right(NonMTDfB)))
            mockGetKnownFactsAndControlListInformation(testVatNumber)(Future.successful(Right(testKnownFactsAndControlListInformation)))
            mockUpsertVatNumber(testVatNumber, isMigratable = false)(Future.successful(mock[UpdateWriteResult]))

            val res = await(TestStoreVatNumberService.storeVatNumber(testVatNumber, agentUser, None, None))
            res shouldBe Right(StoreVatNumberSuccess)

            verifyAudit(AgentClientRelationshipAuditModel(TestConstants.testVatNumber, TestConstants.testAgentReferenceNumber, haveRelationship = true))
            verifyAudit(ControlListAuditModel(testVatNumber, isSuccess = true, nonMigratableReasons = nonMigratableReasons))

            sys.props += "control-list.eligible.stagger_1" -> "Migratable"
          }
        }
        "the VAT number is not eligible for MTD" should {
          "return Ineligible" in {
            enable(AlreadySubscribedCheck)

            val testIneligible = testKnownFactsAndControlListInformation.copy(controlListInformation =
              ControlListInformation(testKnownFactsAndControlListInformation.controlListInformation.controlList + DeRegOrDeath)
            )
            val failures = testIneligible.controlListInformation.validate(mockConfig.eligibilityConfig)
            assert(!failures.isRight)
            val ineligibilityReasons = failures match {
              case Left(uk.gov.hmrc.vatsignup.models.controllist.Ineligible(err)) => err.toList.map(_.toString)
            }

            mockCheckAgentClientRelationship(testAgentReferenceNumber, testVatNumber)(Future.successful(Right(HaveRelationshipResponse)))
            mockGetMandationStatus(testVatNumber)(Future.successful(Right(NonMTDfB)))
            mockGetKnownFactsAndControlListInformation(testVatNumber)(Future.successful(Right(testIneligible)))

            val res = await(TestStoreVatNumberService.storeVatNumber(testVatNumber, agentUser, None, None))
            res shouldBe Left(Ineligible)

            verifyAudit(AgentClientRelationshipAuditModel(TestConstants.testVatNumber, TestConstants.testAgentReferenceNumber, haveRelationship = true))
            verifyAudit(ControlListAuditModel(testVatNumber, isSuccess = false, ineligibilityReasons))
          }
        }
      }

      "there is not an agent-client-relationship" should {
        "return a RelationshipNotFound" in {
          mockCheckAgentClientRelationship(testAgentReferenceNumber, testVatNumber)(Future.successful(Right(NoRelationshipResponse)))

          val res = await(TestStoreVatNumberService.storeVatNumber(testVatNumber, agentUser, None, None))
          res shouldBe Left(RelationshipNotFound)

          verifyAudit(AgentClientRelationshipAuditModel(TestConstants.testVatNumber, TestConstants.testAgentReferenceNumber, haveRelationship = false))
        }
      }

      "the call to agent-client-relationships-fails" should {
        "return an AgentServicesConnectionFailure" in {
          mockCheckAgentClientRelationship(testAgentReferenceNumber, testVatNumber)(
            Future.successful(Left(CheckAgentClientRelationshipResponseFailure(INTERNAL_SERVER_ERROR, Json.obj())))
          )

          val res = await(TestStoreVatNumberService.storeVatNumber(testVatNumber, agentUser, None, None))
          res shouldBe Left(AgentServicesConnectionFailure)
        }
      }
    }

    "the user is a principal user" when {
      "the user has an existing HMCE-VAT enrolment" when {
        "the vat number is not already subscribed for MTD-VAT" when {
          "the vat number is stored successfully" should {
            "return StoreVatNumberSuccess" in {
              enable(AlreadySubscribedCheck)

              mockGetMandationStatus(testVatNumber)(Future.successful(Right(NonMTDfB)))
              mockGetKnownFactsAndControlListInformation(testVatNumber)(Future.successful(Right(testKnownFactsAndControlListInformation)))
              mockUpsertVatNumber(testVatNumber, isMigratable = true)(Future.successful(mock[UpdateWriteResult]))

              val res = await(TestStoreVatNumberService.storeVatNumber(testVatNumber, principalUser, None, None))
              res shouldBe Right(StoreVatNumberSuccess)
            }
          }
          "the vat number is not stored successfully" should {
            "return a VatNumberDatabaseFailure" in {
              enable(AlreadySubscribedCheck)

              mockGetMandationStatus(testVatNumber)(Future.successful(Right(NonDigital)))
              mockGetKnownFactsAndControlListInformation(testVatNumber)(Future.successful(Right(testKnownFactsAndControlListInformation)))
              mockUpsertVatNumber(testVatNumber, isMigratable = true)(Future.failed(new Exception))

              val res = await(TestStoreVatNumberService.storeVatNumber(testVatNumber, principalUser, None, None))
              res shouldBe Left(VatNumberDatabaseFailure)
            }
          }
        }
        "the VAT number is already subscribed for MTD-VAT" should {
          "return AlreadySubscribed" in {
            enable(AlreadySubscribedCheck)

            mockGetMandationStatus(testVatNumber)(Future.successful(Right(MTDfBVoluntary)))

            val res = await(TestStoreVatNumberService.storeVatNumber(testVatNumber, principalUser, None, None))
            res shouldBe Left(AlreadySubscribed)
          }
        }
        "the vat number does not match enrolment" should {
          "return DoesNotMatchEnrolment" in {
            enable(AlreadySubscribedCheck)

            val res = await(TestStoreVatNumberService.storeVatNumber(UUID.randomUUID().toString, principalUser, None, None))
            res shouldBe Left(DoesNotMatchEnrolment)
          }
        }
      }

      "the user does not have either enrolment" should {
        "return InsufficientEnrolments" in {
          val res = await(TestStoreVatNumberService.storeVatNumber(testVatNumber, Enrolments(Set.empty), None, None))
          res shouldBe Left(InsufficientEnrolments)
        }
      }
    }

    "the user has a fresh cred" when {

      def call = TestStoreVatNumberService.storeVatNumber(testVatNumber, freshUser, Some(testPostCode filterNot (_.isWhitespace)), Some(testDateOfRegistration))

      "the vat number is not already subscribed for MTD-VAT" when {
        "the vat number is stored successfully" should {
          "return StoreVatNumberSuccess" in {
            enable(AlreadySubscribedCheck)

            mockGetMandationStatus(testVatNumber)(Future.successful(Right(NonMTDfB)))
            mockGetKnownFactsAndControlListInformation(testVatNumber)(Future.successful(Right(testKnownFactsAndControlListInformation)))
            mockUpsertVatNumber(testVatNumber, isMigratable = true)(Future.successful(mock[UpdateWriteResult]))

            val res = await(call)
            verifyAudit(ControlListAuditModel(testVatNumber, isSuccess = true))
            res shouldBe Right(StoreVatNumberSuccess)
          }
        }
        "Known facts and control list returned ineligible" should {
          "return a Ineligible" in {
            enable(AlreadySubscribedCheck)

            val testIneligible = testKnownFactsAndControlListInformation.copy(controlListInformation =
              ControlListInformation(testKnownFactsAndControlListInformation.controlListInformation.controlList + DeRegOrDeath)
            )
            val failures = testIneligible.controlListInformation.validate(mockConfig.eligibilityConfig)
            assert(!failures.isRight)
            val ineligibilityReasons = failures match {
              case Left(uk.gov.hmrc.vatsignup.models.controllist.Ineligible(err)) => err.toList.map(_.toString)
            }

            mockGetMandationStatus(testVatNumber)(Future.successful(Right(NonMTDfB)))
            mockGetKnownFactsAndControlListInformation(testVatNumber)(Future.successful(Right(testIneligible)))

            val res = await(call)
            verifyAudit(ControlListAuditModel(testVatNumber, isSuccess = false, failureReasons = ineligibilityReasons))
            res shouldBe Left(Ineligible)
          }
        }
        "Known facts and control list returned ControlListInformationVatNumberNotFound" should {
          "return a VatNotFound" in {
            enable(AlreadySubscribedCheck)

            mockGetMandationStatus(testVatNumber)(Future.successful(Right(NonMTDfB)))
            mockGetKnownFactsAndControlListInformation(testVatNumber)(Future.successful(Left(ControlListInformationVatNumberNotFound)))

            val res = await(call)
            verifyAudit(ControlListAuditModel(testVatNumber, isSuccess = false, failureReasons = Seq(vatNumberNotFound)))
            res shouldBe Left(VatNotFound)
          }
        }
        "Known facts and control list returned KnownFactsInvalidVatNumber" should {
          "return a VatInvalid" in {
            enable(AlreadySubscribedCheck)

            mockGetMandationStatus(testVatNumber)(Future.successful(Right(NonMTDfB)))
            mockGetKnownFactsAndControlListInformation(testVatNumber)(Future.successful(Left(KnownFactsInvalidVatNumber)))

            val res = await(call)
            verifyAudit(ControlListAuditModel(testVatNumber, isSuccess = false, failureReasons = Seq(invalidVatNumber)))
            res shouldBe Left(VatInvalid)
          }
        }
        "known facts returned from api differs from known facts by the user" should {
          "return a KnownFactsMismatch" in {
            enable(AlreadySubscribedCheck)

            mockGetMandationStatus(testVatNumber)(Future.successful(Right(NonMTDfB)))
            mockGetKnownFactsAndControlListInformation(testVatNumber)(Future.successful(
              Right(testKnownFactsAndControlListInformation.copy(businessPostcode = testKnownFactsAndControlListInformation.businessPostcode.drop(1)))
            ))

            val res = await(call)
            res shouldBe Left(KnownFactsMismatch)
          }
        }
        "the vat number is not stored successfully" should {
          "return a VatNumberDatabaseFailure" in {
            enable(AlreadySubscribedCheck)

            mockGetMandationStatus(testVatNumber)(Future.successful(Right(NonDigital)))
            mockGetKnownFactsAndControlListInformation(testVatNumber)(Future.successful(Right(testKnownFactsAndControlListInformation)))
            mockUpsertVatNumber(testVatNumber, isMigratable = true)(Future.failed(new Exception))

            val res = await(call)
            res shouldBe Left(VatNumberDatabaseFailure)
          }
        }
      }
      "the VAT number is already subscribed for MTD-VAT" should {
        "return AlreadySubscribed" in {
          enable(AlreadySubscribedCheck)

          mockGetMandationStatus(testVatNumber)(Future.successful(Right(MTDfBVoluntary)))

          val res = await(call)
          res shouldBe Left(AlreadySubscribed)
        }
      }
      "the user does not have either enrolment and did not provide both known facts" should {
        "return InsufficientEnrolments" in {
          enable(AlreadySubscribedCheck)

          val res = await(TestStoreVatNumberService.storeVatNumber(testVatNumber, freshUser, None, None))
          res shouldBe Left(InsufficientEnrolments)
        }
      }
    }

    "the user does not have either enrolment" should {
      "return InsufficientEnrolments" in {
        val res = await(TestStoreVatNumberService.storeVatNumber(testVatNumber, freshUser, None, None))
        res shouldBe Left(InsufficientEnrolments)
      }
    }
  }

}
