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

import org.scalatest.{Matchers, WordSpec}
import play.api.mvc.Request
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.{Admin, Assistant}
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}
import uk.gov.hmrc.vatsignup.connectors.mocks._
import uk.gov.hmrc.vatsignup.helpers.TestConstants._
import uk.gov.hmrc.vatsignup.httpparsers.GetUsersForGroupHttpParser.{UsersFound, UsersGroupsSearchConnectionFailure}
import uk.gov.hmrc.vatsignup.httpparsers._
import uk.gov.hmrc.vatsignup.models.monitoring.AutoClaimEnrolementAuditing.AutoClaimEnrolementAuditingModel
import uk.gov.hmrc.vatsignup.service.mocks.monitoring.MockAuditService
import uk.gov.hmrc.vatsignup.service.mocks.{MockAssignEnrolmentToUserService, MockCheckEnrolmentAllocationService}
import uk.gov.hmrc.vatsignup.services.AutoClaimEnrolmentService._
import uk.gov.hmrc.vatsignup.services.{AssignEnrolmentToUserService, AutoClaimEnrolmentService, CheckEnrolmentAllocationService}
import uk.gov.hmrc.vatsignup.utils.KnownFactsDateFormatter.KnownFactsDateFormatter

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AutoClaimSubscriptionServiceSpec extends WordSpec with Matchers
  with MockEnrolmentStoreProxyConnector
  with MockCheckEnrolmentAllocationService
  with MockAssignEnrolmentToUserService
  with MockUsersGroupsSearchConnector
  with MockAuditService {

  object TestAutoClaimEnrolmentService extends AutoClaimEnrolmentService(
    mockEnrolmentStoreProxyConnector,
    mockCheckEnrolmentAllocationService,
    mockAssignEnrolmentToUserService,
    mockUsersGroupsSearchConnector,
    mockAuditService
  )

  implicit val hc: HeaderCarrier = HeaderCarrier()
  implicit val request: Request[_] = FakeRequest()

  private val testSetCredentialIds = Set(testCredentialId, testCredentialId2)
  private val testMapCredentialRoles = Map(
    testCredentialId -> Admin,
    testCredentialId2 -> Assistant,
    testCredentialId3 -> Admin
  )

  "autoClaimEnrolment" when {
    "no group IDs with an MTDVAT enrolment are returned" when {
      "a group ID with a legacy VAT enrolment is returned" when {
        "user IDs with legacy VAT enrolments are returned" when {
          "allocating the MTDVAT enrolment to the group ID is successful" when {
            "assign the MTDVAT enrolment to the user IDs is successful" should {
              "return Right(EnrolmentAssigned)" in {
                mockGetGroupIdForMtdVatEnrolment(testVatNumber, ignoreAssignments = true)(
                  Future.successful(Right(CheckEnrolmentAllocationService.EnrolmentNotAllocated))
                )
                mockGetGroupIdForLegacyVatEnrolment(testVatNumber)(
                  Future.successful(Left(CheckEnrolmentAllocationService.EnrolmentAlreadyAllocated(testGroupId)))
                )
                mockGetUserIds(testVatNumber)(
                  Future.successful(Right(QueryUsersHttpParser.UsersFound(testSetCredentialIds)))
                )
                mockGetUsersForGroup(testGroupId)(
                  Future.successful(Right(UsersFound(testMapCredentialRoles)))
                )
                mockAllocateEnrolmentWithoutKnownFacts(testGroupId, testCredentialId, testVatNumber)(
                  Future.successful(Right(AllocateEnrolmentResponseHttpParser.EnrolSuccess))
                )
                mockAssignEnrolmentToUser(testSetCredentialIds filterNot (_ == testCredentialId), testVatNumber)(
                  Future.successful(Right(AssignEnrolmentToUserService.EnrolmentAssignedToUsers))
                )

                val res = await(TestAutoClaimEnrolmentService.autoClaimEnrolment(testVatNumber, agentLedSignUp))

                res shouldBe Right(AutoClaimEnrolmentService.EnrolmentAssigned)

                verifyAudit(AutoClaimEnrolementAuditingModel(
                  testVatNumber,
                  agentLedSignUp,
                  isSuccess = true,
                  groupId = Some(testGroupId),
                  userIds = testSetCredentialIds
                ))
              }
            }

            "assigning the MTDVAT enrolment to the user ID fails" when {
              "return EnrolmentAssignmentFailureForIds(Set(<FailedIDs>)" in {
                val testSetCredentialIds = Set(testCredentialId, testCredentialId2, testCredentialId3)

                mockGetGroupIdForMtdVatEnrolment(testVatNumber, ignoreAssignments = true)(
                  Future.successful(Right(CheckEnrolmentAllocationService.EnrolmentNotAllocated))
                )
                mockGetGroupIdForLegacyVatEnrolment(testVatNumber)(
                  Future.successful(Left(CheckEnrolmentAllocationService.EnrolmentAlreadyAllocated(testGroupId)))
                )
                mockGetUserIds(testVatNumber)(Future.successful(Right(QueryUsersHttpParser.UsersFound(testSetCredentialIds))))
                mockGetUsersForGroup(testGroupId)(
                  Future.successful(Right(UsersFound(testMapCredentialRoles)))
                )
                mockAllocateEnrolmentWithoutKnownFacts(testGroupId, testCredentialId, testVatNumber)(
                  Future.successful(Right(AllocateEnrolmentResponseHttpParser.EnrolSuccess))
                )
                mockAssignEnrolmentToUser(testSetCredentialIds filterNot (_ == testCredentialId), testVatNumber)(
                  Future.successful(Left(AssignEnrolmentToUserService.EnrolmentAssignmentFailed(Set(testCredentialId3))))
                )

                val res = await(TestAutoClaimEnrolmentService.autoClaimEnrolment(testVatNumber, agentLedSignUp))

                res shouldBe Left(AutoClaimEnrolmentService.EnrolmentAssignmentFailureForIds(Set(testCredentialId3)))

                verifyAudit(AutoClaimEnrolementAuditingModel(
                  testVatNumber,
                  agentLedSignUp,
                  isSuccess = false,
                  call = Some(assignEnrolmentToUserCall),
                  groupId = Some(testGroupId),
                  userIds = testSetCredentialIds,
                  auditInformation = Some(AutoClaimEnrolmentService.EnrolmentAssignmentFailureForIds(Set(testCredentialId3)).toString)
                ))
              }
            }
          }

          "allocating the MTDVAT enrolment to the group ID fails" when {
            "throw an exception with the credential ID and error message from the allocate enrolment API" in {
              mockGetGroupIdForMtdVatEnrolment(testVatNumber, ignoreAssignments = true)(
                Future.successful(Right(CheckEnrolmentAllocationService.EnrolmentNotAllocated))
              )
              mockGetGroupIdForLegacyVatEnrolment(testVatNumber)(
                Future.successful(Left(CheckEnrolmentAllocationService.EnrolmentAlreadyAllocated(testGroupId)))
              )
              mockGetUserIds(testVatNumber)(Future.successful(Right(QueryUsersHttpParser.UsersFound(testSetCredentialIds))))
              mockGetUsersForGroup(testGroupId)(
                Future.successful(Right(UsersFound(testMapCredentialRoles)))
              )
              mockAllocateEnrolmentWithoutKnownFacts(testGroupId, testCredentialId, testVatNumber)(
                Future.failed(new InternalServerException(testCredentialId + testErrorMsg))
              )
              intercept[InternalServerException] {
                await(TestAutoClaimEnrolmentService.autoClaimEnrolment(testVatNumber, agentLedSignUp))

                verifyAudit(AutoClaimEnrolementAuditingModel(
                  testVatNumber,
                  agentLedSignUp,
                  isSuccess = false,
                  call = Some(allocateEnrolmentWithoutKnownFactsCall),
                  groupId = Some(testGroupId),
                  userIds = testSetCredentialIds,
                  auditInformation = Some((testCredentialId, testErrorMsg).toString)
                ))
              }
            }
          }
        }
      }
    }

    "the call to users groups search returns no admin user IDs" should {
      "return Left(NoAdminUsers)" in {
        mockGetGroupIdForMtdVatEnrolment(testVatNumber, ignoreAssignments = true)(
          Future.successful(Right(CheckEnrolmentAllocationService.EnrolmentNotAllocated))
        )
        mockGetGroupIdForLegacyVatEnrolment(testVatNumber)(
          Future.successful(Left(CheckEnrolmentAllocationService.EnrolmentAlreadyAllocated(testGroupId)))
        )
        mockGetUserIds(testVatNumber)(Future.successful(Right(QueryUsersHttpParser.UsersFound(testSetCredentialIds))))
        mockGetUsersForGroup(testGroupId)(
          Future.successful(Right(UsersFound(Map(testCredentialId -> Assistant))))
        )

        val res = await(TestAutoClaimEnrolmentService.autoClaimEnrolment(testVatNumber, agentLedSignUp))

        res shouldBe Left(NoAdminUsers)

        verifyAudit(AutoClaimEnrolementAuditingModel(
          testVatNumber,
          agentLedSignUp,
          isSuccess = false,
          call = Some(getAdminUserIdCall),
          groupId = Some(testGroupId),
          userIds = testSetCredentialIds,
          auditInformation = Some(AutoClaimEnrolmentService.NoAdminUsers.toString)
        ))
      }
    }

    "the call to users groups search fails" should {
      "return Left(UsersGroupsSearchFailure)" in {
        mockGetGroupIdForMtdVatEnrolment(testVatNumber, ignoreAssignments = true)(
          Future.successful(Right(CheckEnrolmentAllocationService.EnrolmentNotAllocated))
        )
        mockGetGroupIdForLegacyVatEnrolment(testVatNumber)(
          Future.successful(Left(CheckEnrolmentAllocationService.EnrolmentAlreadyAllocated(testGroupId)))
        )
        mockGetUserIds(testVatNumber)(Future.successful(Right(QueryUsersHttpParser.UsersFound(testSetCredentialIds))))
        mockGetUsersForGroup(testGroupId)(
          Future.successful(Left(UsersGroupsSearchConnectionFailure(INTERNAL_SERVER_ERROR)))
        )

        val res = await(TestAutoClaimEnrolmentService.autoClaimEnrolment(testVatNumber, agentLedSignUp))

        res shouldBe Left(UsersGroupsSearchFailure)

        verifyAudit(AutoClaimEnrolementAuditingModel(
          testVatNumber,
          agentLedSignUp,
          isSuccess = false,
          call = Some(getAdminUserIdCall),
          groupId = Some(testGroupId),
          userIds = testSetCredentialIds,
          auditInformation = Some(AutoClaimEnrolmentService.UsersGroupsSearchFailure.toString)
        ))
      }
    }

    "the only admin user ID does not have the legacy VAT enrolment" should {
      "return Left(NoAdminUsers)" in {
        mockGetGroupIdForMtdVatEnrolment(testVatNumber, ignoreAssignments = true)(
          Future.successful(Right(CheckEnrolmentAllocationService.EnrolmentNotAllocated))
        )
        mockGetGroupIdForLegacyVatEnrolment(testVatNumber)(
          Future.successful(Left(CheckEnrolmentAllocationService.EnrolmentAlreadyAllocated(testGroupId)))
        )
        mockGetUserIds(testVatNumber)(
          Future.successful(Right(QueryUsersHttpParser.UsersFound(
            Set(
              testCredentialId,
              testCredentialId2
            )
          )))
        )
        mockGetUsersForGroup(testGroupId)(
          Future.successful(Right(UsersFound(
            Map(
              testCredentialId -> Assistant,
              testCredentialId2 -> Assistant,
              testCredentialId3 -> Admin
            )
          )))
        )

        val res = await(TestAutoClaimEnrolmentService.autoClaimEnrolment(testVatNumber, agentLedSignUp))

        res shouldBe Left(NoAdminUsers)

        verifyAudit(AutoClaimEnrolementAuditingModel(
          testVatNumber,
          agentLedSignUp,
          isSuccess = false,
          call = Some(getAdminUserIdCall),
          groupId = Some(testGroupId),
          userIds = testSetCredentialIds,
          auditInformation = Some(AutoClaimEnrolmentService.NoAdminUsers.toString)
        ))
      }
    }

    "user IDs with legacy VAT enrolment are found but no the get user IDs API returns an empty list of user IDs" should {
      "return a ConnectionFailure" in {
        mockGetGroupIdForMtdVatEnrolment(testVatNumber, ignoreAssignments = true)(
          Future.successful(Right(CheckEnrolmentAllocationService.EnrolmentNotAllocated))
        )
        mockGetGroupIdForLegacyVatEnrolment(testVatNumber)(
          Future.successful(Left(CheckEnrolmentAllocationService.EnrolmentAlreadyAllocated(testGroupId)))
        )
        mockGetUserIds(testVatNumber)(Future.successful(Right(QueryUsersHttpParser.UsersFound(Set()))))

        val res = await(TestAutoClaimEnrolmentService.autoClaimEnrolment(testVatNumber, agentLedSignUp))

        res shouldBe Left(EnrolmentStoreProxyConnectionFailure)

        verifyAudit(AutoClaimEnrolementAuditingModel(
          testVatNumber,
          agentLedSignUp,
          isSuccess = false,
          call = Some(getLegacyEnrolmentUserIDsCall),
          groupId = Some(testGroupId),
          auditInformation = Some(AutoClaimEnrolmentService.EnrolmentStoreProxyConnectionFailure.toString)
        ))
      }
    }

    "user IDs with legacy VAT enrolments are not returned from the get user IDs API" should {
      "return Left(NoUsersFound)" in {
        mockGetGroupIdForMtdVatEnrolment(testVatNumber, ignoreAssignments = true)(
          Future.successful(Right(CheckEnrolmentAllocationService.EnrolmentNotAllocated))
        )
        mockGetGroupIdForLegacyVatEnrolment(testVatNumber)(
          Future.successful(Left(CheckEnrolmentAllocationService.EnrolmentAlreadyAllocated(testGroupId)))
        )
        mockGetUserIds(testVatNumber)(Future.successful(Right(QueryUsersHttpParser.NoUsersFound)))

        val res = await(TestAutoClaimEnrolmentService.autoClaimEnrolment(testVatNumber, agentLedSignUp))

        res shouldBe Left(NoUsersFound)

        verifyAudit(AutoClaimEnrolementAuditingModel(
          testVatNumber,
          agentLedSignUp,
          isSuccess = true,
          call = Some(getLegacyEnrolmentUserIDsCall),
          groupId = Some(testGroupId),
          auditInformation = Some(AutoClaimEnrolmentService.NoUsersFound.toString)
        ))
      }
    }

    "the call to get user IDs API fails" should {
      "return Left(EnrolmentStoreProxyConnectionFailure)" in {
        mockGetGroupIdForMtdVatEnrolment(testVatNumber, ignoreAssignments = true)(
          Future.successful(Right(CheckEnrolmentAllocationService.EnrolmentNotAllocated))
        )
        mockGetGroupIdForLegacyVatEnrolment(testVatNumber)(
          Future.successful(Left(CheckEnrolmentAllocationService.EnrolmentAlreadyAllocated(testGroupId)))
        )
        mockGetUserIds(testVatNumber)(Future.successful(Left(QueryUsersHttpParser.EnrolmentStoreProxyConnectionFailure(BAD_REQUEST))))

        val res = await(TestAutoClaimEnrolmentService.autoClaimEnrolment(testVatNumber, agentLedSignUp))

        res shouldBe Left(EnrolmentStoreProxyConnectionFailure)

        verifyAudit(AutoClaimEnrolementAuditingModel(
          testVatNumber,
          agentLedSignUp,
          isSuccess = false,
          call = Some(getLegacyEnrolmentUserIDsCall),
          groupId = Some(testGroupId),
          auditInformation = Some(AutoClaimEnrolmentService.EnrolmentStoreProxyConnectionFailure.toString)
        ))
      }
    }
  }

  "no group ID with a legacy VAT enrolment is returned" should {
    "return Left(EnrolmentNotAllocated)" in {
      mockGetGroupIdForMtdVatEnrolment(testVatNumber, ignoreAssignments = true)(
        Future.successful(Right(CheckEnrolmentAllocationService.EnrolmentNotAllocated))
      )
      mockGetGroupIdForLegacyVatEnrolment(testVatNumber)(
        Future.successful(Right(CheckEnrolmentAllocationService.EnrolmentNotAllocated))
      )

      val res = await(TestAutoClaimEnrolmentService.autoClaimEnrolment(testVatNumber, agentLedSignUp))

      res shouldBe Left(EnrolmentNotAllocated)

      verifyAudit(AutoClaimEnrolementAuditingModel(
        testVatNumber,
        agentLedSignUp,
        isSuccess = true,
        call = Some(getLegacyEnrolmentAllocationCall),
        auditInformation = Some(AutoClaimEnrolmentService.EnrolmentNotAllocated.toString)
      ))
    }
  }

  "the call to get group IDs with legacy VAT enrolment fails" should {
    "return Left(EnrolmentStoreProxyFailure)" in {
      mockGetGroupIdForMtdVatEnrolment(testVatNumber, ignoreAssignments = true)(
        Future.successful(Right(CheckEnrolmentAllocationService.EnrolmentNotAllocated))
      )
      mockGetGroupIdForLegacyVatEnrolment(testVatNumber)(
        Future.successful(Left(CheckEnrolmentAllocationService.UnexpectedEnrolmentStoreProxyFailure(BAD_REQUEST)))
      )

      val res = await(TestAutoClaimEnrolmentService.autoClaimEnrolment(testVatNumber, agentLedSignUp))

      res shouldBe Left(EnrolmentStoreProxyFailure(BAD_REQUEST))

      verifyAudit(AutoClaimEnrolementAuditingModel(
        testVatNumber,
        agentLedSignUp,
        isSuccess = false,
        call = Some(getLegacyEnrolmentAllocationCall),
        auditInformation = Some(AutoClaimEnrolmentService.EnrolmentStoreProxyFailure(BAD_REQUEST).toString)
      ))
    }
  }

  "the call to get group IDs with MTDVAT enrolment fails" should {
    "return Left(EnrolmentStoreProxyFailure)" in {
      mockGetGroupIdForMtdVatEnrolment(testVatNumber, ignoreAssignments = true)(
        Future.successful(Left(CheckEnrolmentAllocationService.UnexpectedEnrolmentStoreProxyFailure(BAD_REQUEST)))      )

      val res = await(TestAutoClaimEnrolmentService.autoClaimEnrolment(testVatNumber, agentLedSignUp))

      res shouldBe Left(EnrolmentStoreProxyFailure(BAD_REQUEST))

      verifyAudit(AutoClaimEnrolementAuditingModel(
        testVatNumber,
        agentLedSignUp,
        isSuccess = false,
        call = Some(getMtdvatEnrolmentAllocationCall),
        auditInformation = Some(AutoClaimEnrolmentService.EnrolmentStoreProxyFailure(BAD_REQUEST).toString)
      ))
    }
  }

  "the call to get group IDs with MTDVAT enrolment returns a group ID" should {
    "return Left(EnrolmentAlreadyAllocated)" in {
      mockGetGroupIdForMtdVatEnrolment(testVatNumber, ignoreAssignments = true)(
        Future.successful(Left(CheckEnrolmentAllocationService.EnrolmentAlreadyAllocated(testGroupId)))
      )

      val res = await(TestAutoClaimEnrolmentService.autoClaimEnrolment(testVatNumber, agentLedSignUp))

      res shouldBe Left(EnrolmentAlreadyAllocated)

      verifyAudit(AutoClaimEnrolementAuditingModel(
        testVatNumber,
        agentLedSignUp,
        isSuccess = false,
        call = Some(getMtdvatEnrolmentAllocationCall),
        auditInformation = Some(AutoClaimEnrolmentService.EnrolmentAlreadyAllocated.toString)
      ))
    }
  }

}
