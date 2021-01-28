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

  "auto claim enrolment" when {
    "legacy group ID is returned" when {
      "legacy user IDs are returned" when {
        "the known facts connector is successful" when {
          "allocate the new group ID is successful" when {
            "assign the new user IDs is successful" when {
              "returns successful" in {
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
            "assign the new user IDs fails" when {
              "return Failure" in {
                val testSetCredentialIds = Set(testCredentialId, testCredentialId2, testCredentialId3)
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
          "allocate the new group ID fails" when {
            "return Failure" in {
              mockGetGroupIdForLegacyVatEnrolment(testVatNumber)(
                Future.successful(Left(CheckEnrolmentAllocationService.EnrolmentAlreadyAllocated(testGroupId)))
              )
              mockGetUserIds(testVatNumber)(Future.successful(Right(QueryUsersHttpParser.UsersFound(testSetCredentialIds))))
              mockGetUsersForGroup(testGroupId)(
                Future.successful(Right(UsersFound(testMapCredentialRoles)))
              )
              mockAllocateEnrolmentWithoutKnownFacts(testGroupId, testCredentialId, testVatNumber)(
                Future.failed(new InternalServerException(testErrorMsg))
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
    "users groups search returns no admins" should {
      "return NoAdminUsers" in {
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
    "users groups search fails" should {
      "return UsersGroupsSearchFailure" in {
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
    "the only admin user does not have the legacy VAT enrolment" should {
      "return NoAdminUsers" in {
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
    "legacy user IDS are found but no userIds are returned" should {
      "return a ConnectionFailure" in {
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
    "legacy user IDS are not returned" when {
      "returns NoUsersFound" in {
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
    "legacy user IDs are not returned" when {
      "there is a connection failure" in {
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
  "legacy group ID is not returned" when {
    "returns EnrolmentNotAllocated" in {
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
  "legacy group ID is not returned" when {
    "returns EnrolmentStoreProxyFailure" in {
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

}
