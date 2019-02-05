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

import play.api.http.Status.INTERNAL_SERVER_ERROR
import play.api.libs.json.Json
import play.api.test.FakeRequest
import reactivemongo.api.commands.UpdateWriteResult
import uk.gov.hmrc.auth.core.Enrolments
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.HeaderCarrierConverter
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.vatsignup.config.mocks.MockConfig
import uk.gov.hmrc.vatsignup.connectors.mocks.{MockAgentClientRelationshipsConnector, MockMandationStatusConnector}
import uk.gov.hmrc.vatsignup.helpers.TestConstants._
import uk.gov.hmrc.vatsignup.models._
import uk.gov.hmrc.vatsignup.models.monitoring.AgentClientRelationshipAuditing.AgentClientRelationshipAuditModel
import uk.gov.hmrc.vatsignup.repositories.mocks.MockUnconfirmedSubscriptionRequestRepository
import uk.gov.hmrc.vatsignup.service.mocks.monitoring.MockAuditService
import uk.gov.hmrc.vatsignup.service.mocks.{MockClaimSubscriptionService, MockControlListEligibilityService}
import uk.gov.hmrc.vatsignup.services.ClaimSubscriptionService.{KnownFactsFailure, SubscriptionClaimed}
import uk.gov.hmrc.vatsignup.services.ControlListEligibilityService.EligibilitySuccess
import uk.gov.hmrc.vatsignup.services.StoreVatNumberWithRequestIdService._
import uk.gov.hmrc.vatsignup.services._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class StoreVatNumberWithRequestIdServiceSpec
  extends UnitSpec with MockAgentClientRelationshipsConnector with MockUnconfirmedSubscriptionRequestRepository
    with MockAuditService with MockConfig
    with MockMandationStatusConnector
    with MockControlListEligibilityService
    with MockClaimSubscriptionService {

  object TestStoreVatNumberService extends StoreVatNumberWithRequestIdService(
    mockUnconfirmedSubscriptionRequestRepository,
    mockAgentClientRelationshipsConnector,
    mockMandationStatusConnector,
    mockControlListEligibilityService,
    mockClaimSubscriptionService,
    mockAuditService,
    mockConfig
  )

  implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromHeadersAndSession(FakeRequest().headers)
  implicit val request = FakeRequest("POST", "testUrl")

  val agentUser = Enrolments(Set(testAgentEnrolment))
  val principalUser = Enrolments(Set(testPrincipalEnrolment))
  val freshUser = Enrolments(Set.empty)

  "storeVatNumberWithRequestId" when {
    "the user is an agent" when {
      "there is an agent-client relationship" when {
        "the vat number is not already subscribed for MTD-VAT" when {
          "the VAT number is eligible for MTD" when {
            "the vat number is stored successfully" should {
              "return a StoreVatNumberSuccess" in {
                mockCheckAgentClientRelationship(testAgentReferenceNumber, testVatNumber)(
                  Future.successful(Right(HaveRelationshipResponse))
                )
                mockGetMandationStatus(testVatNumber)(Future.successful(Right(NonMTDfB)))
                mockGetEligibilityStatus(testVatNumber)(Future.successful(
                  Right(EligibilitySuccess(testPostCode, testDateOfRegistration, isMigratable = true, isOverseas = false))
                ))
                mockUpsertVatNumber(
                  requestId = testToken,
                  vatNumber = testVatNumber,
                  isMigratable = true
                )(Future.successful(mock[UpdateWriteResult]))

                val res = await(TestStoreVatNumberService.storeVatNumber(
                  requestId = testToken,
                  vatNumber = testVatNumber,
                  enrolments = agentUser,
                  businessPostcode = None,
                  vatRegistrationDate = None,
                  isFromBta = None
                ))
                res shouldBe Right(StoreVatNumberSuccess)

                verifyAudit(AgentClientRelationshipAuditModel(
                  vatNumber = testVatNumber,
                  agentReferenceNumber = testAgentReferenceNumber,
                  haveRelationship = true
                ))
              }
            }
            "the vat number is not stored successfully" should {
              "return a VatNumberDatabaseFailure" in {
                mockCheckAgentClientRelationship(testAgentReferenceNumber, testVatNumber)(
                  Future.successful(Right(HaveRelationshipResponse))
                )
                mockGetMandationStatus(testVatNumber)(Future.successful(Right(NonDigital)))
                mockGetEligibilityStatus(testVatNumber)(Future.successful(
                  Right(EligibilitySuccess(testPostCode, testDateOfRegistration, isMigratable = true, isOverseas = false))
                ))
                mockUpsertVatNumber(
                  requestId = testToken,
                  vatNumber = testVatNumber,
                  isMigratable = true
                )(Future.failed(new Exception))

                val res = await(TestStoreVatNumberService.storeVatNumber(
                  requestId = testToken,
                  vatNumber = testVatNumber,
                  enrolments = agentUser,
                  businessPostcode = None,
                  vatRegistrationDate = None,
                  isFromBta = None
                ))
                res shouldBe Left(VatNumberDatabaseFailure)

                verifyAudit(AgentClientRelationshipAuditModel(
                  vatNumber = testVatNumber,
                  agentReferenceNumber = testAgentReferenceNumber,
                  haveRelationship = true
                ))
              }
            }
          }
          "the VAT number is already voluntarily subscribed for MTD-VAT" should {
            "return AlreadySubscribed" in {
              mockCheckAgentClientRelationship(testAgentReferenceNumber, testVatNumber)(
                Future.successful(Right(HaveRelationshipResponse))
              )
              mockGetMandationStatus(testVatNumber)(Future.successful(Right(MTDfBVoluntary)))

              val res = await(TestStoreVatNumberService.storeVatNumber(
                requestId = testToken,
                vatNumber = testVatNumber,
                enrolments = agentUser,
                businessPostcode = None,
                vatRegistrationDate = None,
                isFromBta = None
              ))
              res shouldBe Left(AlreadySubscribed(subscriptionClaimed = false))

              verifyAudit(AgentClientRelationshipAuditModel(
                vatNumber = testVatNumber,
                agentReferenceNumber = testAgentReferenceNumber,
                haveRelationship = true
              ))
            }
          }
          "the VAT number is already mandated for MTD-VAT" should {
            "return AlreadySubscribed" in {
              mockCheckAgentClientRelationship(testAgentReferenceNumber, testVatNumber)(
                Future.successful(Right(HaveRelationshipResponse))
              )
              mockGetMandationStatus(testVatNumber)(Future.successful(Right(MTDfBMandated)))

              val res = await(TestStoreVatNumberService.storeVatNumber(
                requestId = testToken,
                vatNumber = testVatNumber,
                enrolments = agentUser,
                businessPostcode = None,
                vatRegistrationDate = None,
                isFromBta = None
              ))
              res shouldBe Left(AlreadySubscribed(subscriptionClaimed = false))

              verifyAudit(AgentClientRelationshipAuditModel(
                vatNumber = testVatNumber,
                agentReferenceNumber = testAgentReferenceNumber,
                haveRelationship = true
              ))
            }
          }
        }
        "the VAT number is eligible but non migratable" should {
          "return StoreVatNumberSuccess" in {
            mockCheckAgentClientRelationship(testAgentReferenceNumber, testVatNumber)(
              Future.successful(Right(HaveRelationshipResponse))
            )
            mockGetMandationStatus(testVatNumber)(Future.successful(Right(NonMTDfB)))
            mockGetEligibilityStatus(testVatNumber)(
              Future.successful(Right(EligibilitySuccess(testPostCode, testDateOfRegistration, isMigratable = false, isOverseas = false)))
            )
            mockUpsertVatNumber(
              requestId = testToken,
              vatNumber = testVatNumber,
              isMigratable = false
            )(Future.successful(mock[UpdateWriteResult]))

            val res = await(TestStoreVatNumberService.storeVatNumber(
              requestId = testToken,
              vatNumber = testVatNumber,
              enrolments = agentUser,
              businessPostcode = None,
              vatRegistrationDate = None,
              isFromBta = None
            ))
            res shouldBe Right(StoreVatNumberSuccess)

            verifyAudit(AgentClientRelationshipAuditModel(
              vatNumber = testVatNumber,
              agentReferenceNumber = testAgentReferenceNumber,
              haveRelationship = true
            ))
          }
        }
        "the VAT number is not eligible for MTD" should {
          "return Ineligible" in {
            mockCheckAgentClientRelationship(testAgentReferenceNumber, testVatNumber)(
              Future.successful(Right(HaveRelationshipResponse))
            )
            mockGetMandationStatus(testVatNumber)(Future.successful(Right(NonMTDfB)))
            mockGetEligibilityStatus(testVatNumber)(
              Future.successful(Left(ControlListEligibilityService.IneligibleVatNumber(testMigratableDates)))
            )

            val res = await(TestStoreVatNumberService.storeVatNumber(
              requestId = testToken,
              vatNumber = testVatNumber,
              enrolments = agentUser,
              businessPostcode = None,
              vatRegistrationDate = None,
              isFromBta = None
            ))
            res shouldBe Left(Ineligible(testMigratableDates))

            verifyAudit(AgentClientRelationshipAuditModel(
              vatNumber = testVatNumber,
              agentReferenceNumber = testAgentReferenceNumber,
              haveRelationship = true
            ))
          }
        }
      }

      "there is not an agent-client-relationship" should {
        "return a RelationshipNotFound" in {
          mockCheckAgentClientRelationship(testAgentReferenceNumber, testVatNumber)(
            Future.successful(Right(NoRelationshipResponse))
          )

          val res = await(TestStoreVatNumberService.storeVatNumber(
            requestId = testToken,
            vatNumber = testVatNumber,
            enrolments = agentUser,
            businessPostcode = None,
            vatRegistrationDate = None,
            isFromBta = None
          ))
          res shouldBe Left(RelationshipNotFound)

          verifyAudit(AgentClientRelationshipAuditModel(
            vatNumber = testVatNumber,
            agentReferenceNumber = testAgentReferenceNumber,
            haveRelationship = false
          ))
        }
      }

      "the call to agent-client-relationships-fails" should {
        "return an AgentServicesConnectionFailure" in {
          mockCheckAgentClientRelationship(testAgentReferenceNumber, testVatNumber)(
            Future.successful(Left(CheckAgentClientRelationshipResponseFailure(INTERNAL_SERVER_ERROR, Json.obj())))
          )

          val res = await(TestStoreVatNumberService.storeVatNumber(
            requestId = testToken,
            vatNumber = testVatNumber,
            enrolments = agentUser,
            businessPostcode = None,
            vatRegistrationDate = None,
            isFromBta = None
          ))
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
              mockGetEligibilityStatus(testVatNumber)(Future.successful(Right(
                EligibilitySuccess(testPostCode, testDateOfRegistration, isMigratable = true, isOverseas = false)
              )))
              mockUpsertVatNumber(
                requestId = testToken,
                vatNumber = testVatNumber,
                isMigratable = true
              )(Future.successful(mock[UpdateWriteResult]))

              val res = await(TestStoreVatNumberService.storeVatNumber(
                requestId = testToken,
                vatNumber = testVatNumber,
                enrolments = principalUser,
                businessPostcode = None,
                vatRegistrationDate = None,
                isFromBta = Some(false)
              ))
              res shouldBe Right(StoreVatNumberSuccess)
            }
          }
          "the vat number is not stored successfully" should {
            "return a VatNumberDatabaseFailure" in {
              mockGetMandationStatus(testVatNumber)(Future.successful(Right(NonDigital)))
              mockGetEligibilityStatus(testVatNumber)(Future.successful(Right(
                EligibilitySuccess(testPostCode, testDateOfRegistration, isMigratable = true, isOverseas = false)
              )))
              mockUpsertVatNumber(testToken, testVatNumber, isMigratable = true)(Future.failed(new Exception))

              val res = await(TestStoreVatNumberService.storeVatNumber(
                requestId = testToken,
                vatNumber = testVatNumber,
                enrolments = principalUser,
                businessPostcode = None,
                vatRegistrationDate = None,
                isFromBta = Some(false)
              ))
              res shouldBe Left(VatNumberDatabaseFailure)
            }
          }
        }
        "the vat number does not match enrolment" should {
          "return DoesNotMatchEnrolment" in {
            val res = await(TestStoreVatNumberService.storeVatNumber(
              requestId = testToken,
              vatNumber = UUID.randomUUID().toString,
              enrolments = principalUser,
              businessPostcode = None,
              vatRegistrationDate = None,
              isFromBta = Some(false)
            ))
            res shouldBe Left(DoesNotMatchEnrolment)
          }
        }
      }

      "the user does not have either enrolment" should {
        "return InsufficientEnrolments" in {
          val res = await(TestStoreVatNumberService.storeVatNumber(
            requestId = testToken,
            vatNumber = testVatNumber,
            enrolments = Enrolments(Set.empty),
            businessPostcode = None,
            vatRegistrationDate = None,
            isFromBta = Some(false)
          ))
          res shouldBe Left(InsufficientEnrolments)
        }
      }
    }

    "the user has a fresh cred" when {

      def call = TestStoreVatNumberService.storeVatNumber(
        requestId = testToken,
        vatNumber = testVatNumber,
        enrolments = freshUser,
        businessPostcode = Some(testPostCode),
        vatRegistrationDate = Some(testDateOfRegistration),
        isFromBta = Some(false)
      )

      "the vat number is not already subscribed for MTD-VAT" when {
        "the vat number is stored successfully" should {
          "return StoreVatNumberSuccess" in {
            mockGetMandationStatus(testVatNumber)(Future.successful(Right(NonMTDfB)))
            mockGetEligibilityStatus(testVatNumber)(Future.successful(Right(
              EligibilitySuccess(testPostCode, testDateOfRegistration, isMigratable = true, isOverseas = false)
            )))
            mockUpsertVatNumber(testToken, testVatNumber, isMigratable = true)(
              Future.successful(mock[UpdateWriteResult])
            )

            val res = await(call)
            res shouldBe Right(StoreVatNumberSuccess)
          }
        }
        "Known facts and control list returned ineligible" should {
          "return a Ineligible" in {
            mockGetMandationStatus(testVatNumber)(Future.successful(Right(NonMTDfB)))
            mockGetEligibilityStatus(testVatNumber)(
              Future.successful(Left(ControlListEligibilityService.IneligibleVatNumber(testMigratableDates)))
            )

            val res = await(call)
            res shouldBe Left(Ineligible(testMigratableDates))
          }
        }
        "Known facts and control list returned ControlListInformationVatNumberNotFound" should {
          "return a VatNotFound" in {
            mockGetMandationStatus(testVatNumber)(Future.successful(Right(NonMTDfB)))
            mockGetEligibilityStatus(testVatNumber)(
              Future.successful(Left(ControlListEligibilityService.VatNumberNotFound))
            )

            val res = await(call)
            res shouldBe Left(VatNotFound)
          }
        }
        "Known facts and control list returned KnownFactsInvalidVatNumber" should {
          "return a VatInvalid" in {
            mockGetMandationStatus(testVatNumber)(Future.successful(Right(NonMTDfB)))
            mockGetEligibilityStatus(testVatNumber)(
              Future.successful(Left(ControlListEligibilityService.InvalidVatNumber))
            )

            val res = await(call)
            res shouldBe Left(VatInvalid)
          }
        }
        "known facts returned from api differs from known facts by the user" should {
          "return a KnownFactsMismatch" in {
            mockGetMandationStatus(testVatNumber)(Future.successful(Right(NonMTDfB)))
            mockGetEligibilityStatus(testVatNumber)(Future.successful(Right(
              EligibilitySuccess(testPostCode.drop(1), testDateOfRegistration, isMigratable = false, isOverseas = false)
            )))

            val res = await(call)
            res shouldBe Left(KnownFactsMismatch)
          }
        }
        "the vat number is not stored successfully" should {
          "return a VatNumberDatabaseFailure" in {
            mockGetMandationStatus(testVatNumber)(Future.successful(Right(NonDigital)))
            mockGetEligibilityStatus(testVatNumber)(
              Future.successful(Right(EligibilitySuccess(testPostCode, testDateOfRegistration, isMigratable = true, isOverseas = false)))
            )
            mockUpsertVatNumber(testToken, testVatNumber, isMigratable = true)(Future.failed(new Exception))

            val res = await(call)
            res shouldBe Left(VatNumberDatabaseFailure)
          }
        }
      }
      "the VAT number is already subscribed for MTD-VAT" when {
        "the claim subscription is successful" should {
          "return AlreadySubscribed with the subscription claimed" in {
            mockGetMandationStatus(testVatNumber)(Future.successful(Right(MTDfBVoluntary)))
            mockClaimSubscription(testVatNumber, Some(testPostCode), Some(testDateOfRegistration), isFromBta = false)(Future.successful(Right(SubscriptionClaimed)))

            val res = await(call)
            res shouldBe Left(AlreadySubscribed(subscriptionClaimed = true))
          }
        }

        "the claim subscription fails" should {
          "return ClaimSubscriptionFailure" in {
            mockGetMandationStatus(testVatNumber)(Future.successful(Right(MTDfBVoluntary)))
            mockClaimSubscription(testVatNumber, Some(testPostCode), Some(testDateOfRegistration), isFromBta = false)(Future.successful(Left(KnownFactsFailure)))

            val res = await(call)
            res shouldBe Left(ClaimSubscriptionFailure)
          }
        }
      }
      "the user does not have either enrolment and did not provide both known facts" should {
        "return InsufficientEnrolments" in {
          val res = await(TestStoreVatNumberService.storeVatNumber(testToken, testVatNumber, freshUser, None, None, None))
          res shouldBe Left(InsufficientEnrolments)
        }
      }
    }

    "the user does not have either enrolment" should {
      "return InsufficientEnrolments" in {
        val res = await(TestStoreVatNumberService.storeVatNumber(testToken, testVatNumber, freshUser, None, None, None))
        res shouldBe Left(InsufficientEnrolments)
      }
    }
  }

}
