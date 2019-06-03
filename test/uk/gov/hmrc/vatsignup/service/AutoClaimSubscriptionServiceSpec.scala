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

import play.api.http.Status._
import play.api.mvc.Request
import play.api.test.FakeRequest
import uk.gov.hmrc.auth.core.{Admin, Assistant}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.vatsignup.connectors.mocks._
import uk.gov.hmrc.vatsignup.helpers.TestConstants._
import uk.gov.hmrc.vatsignup.httpparsers.GetUsersForGroupHttpParser.{UsersFound, UsersGroupsSearchConnectionFailure}
import uk.gov.hmrc.vatsignup.httpparsers._
import uk.gov.hmrc.vatsignup.service.mocks.{MockAssignEnrolmentToUserService, MockCheckEnrolmentAllocationService}
import uk.gov.hmrc.vatsignup.services.AutoClaimEnrolmentService._
import uk.gov.hmrc.vatsignup.services.{AssignEnrolmentToUserService, AutoClaimEnrolmentService, CheckEnrolmentAllocationService}
import uk.gov.hmrc.vatsignup.utils.KnownFactsDateFormatter.KnownFactsDateFormatter

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


class AutoClaimSubscriptionServiceSpec extends UnitSpec with MockKnownFactsConnector with MockTaxEnrolmentsConnector
  with MockEnrolmentStoreProxyConnector with MockCheckEnrolmentAllocationService with MockAssignEnrolmentToUserService
  with MockUsersGroupsSearchConnector {

  object TestAutoClaimEnrolmentService extends AutoClaimEnrolmentService(
    mockKnownFactsConnector,
    mockTaxEnrolmentsConnector,
    mockEnrolmentStoreProxyConnector,
    mockCheckEnrolmentAllocationService,
    mockAssignEnrolmentToUserService,
    mockUsersGroupsSearchConnector
  )

  implicit val hc: HeaderCarrier = HeaderCarrier()
  implicit val request: Request[_] = FakeRequest()

  private val testSetCredentialIds = Set(testCredentialId, testCredentialId2)
  private val testMapCredentialRoles = Map(
    testCredentialId -> Admin,
    testCredentialId2 -> Assistant,
    testCredentialId3 -> Admin
  )

  "auto claim enrolment" should {
    "legacy group ID is returned" when {
      "legacy user IDs are returned" when {
        "the known facts connector is successful" when {
          "upsert the enrolment is successful" when {
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
                  mockGetKnownFacts(testVatNumber)(
                    Future.successful(Right(KnownFactsHttpParser.KnownFacts(testPostCode, testDateOfRegistration)))
                  )
                  mockUpsertEnrolment(testVatNumber, testPostCode, testDateOfRegistration.toTaxEnrolmentsFormat)(
                    Future.successful(Right(UpsertEnrolmentResponseHttpParser.UpsertEnrolmentSuccess))
                  )
                  mockAllocateEnrolmentWithoutKnownFacts(testGroupId, testCredentialId, testVatNumber)(
                    Future.successful(Right(AllocateEnrolmentResponseHttpParser.EnrolSuccess))
                  )
                  mockAssignEnrolmentToUser(testSetCredentialIds filterNot (_ == testCredentialId), testVatNumber)(
                    Future.successful(Right(AssignEnrolmentToUserService.EnrolmentAssignedToUsers))
                  )

                  val res = await(TestAutoClaimEnrolmentService.autoClaimEnrolment(testVatNumber))

                  res shouldBe Right(AutoClaimEnrolmentService.EnrolmentAssigned)
                }
              }
              "assign the new user IDs fails" when {
                "return Failure" in {
                  mockGetGroupIdForLegacyVatEnrolment(testVatNumber)(
                    Future.successful(Left(CheckEnrolmentAllocationService.EnrolmentAlreadyAllocated(testGroupId)))
                  )
                  mockGetUserIds(testVatNumber)(Future.successful(Right(QueryUsersHttpParser.UsersFound(testSetCredentialIds))))
                  mockGetUsersForGroup(testGroupId)(
                    Future.successful(Right(UsersFound(testMapCredentialRoles)))
                  )
                  mockGetKnownFacts(testVatNumber)(Future.successful(Right(KnownFactsHttpParser.KnownFacts(testPostCode, testDateOfRegistration))))
                  mockUpsertEnrolment(testVatNumber, testPostCode, testDateOfRegistration.toTaxEnrolmentsFormat)(
                    Future.successful(Right(UpsertEnrolmentResponseHttpParser.UpsertEnrolmentSuccess))
                  )
                  mockAllocateEnrolmentWithoutKnownFacts(testGroupId, testCredentialId, testVatNumber)(
                    Future.successful(Right(AllocateEnrolmentResponseHttpParser.EnrolSuccess))
                  )
                  mockAssignEnrolmentToUser(testSetCredentialIds filterNot (_ == testCredentialId), testVatNumber)(
                    Future.successful(Left(AssignEnrolmentToUserService.EnrolmentAssignmentFailed))
                  )

                  val res = await(TestAutoClaimEnrolmentService.autoClaimEnrolment(testVatNumber))

                  res shouldBe Left(AutoClaimEnrolmentService.EnrolmentAssignmentFailure)
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
                mockGetKnownFacts(testVatNumber)(Future.successful(Right(KnownFactsHttpParser.KnownFacts(testPostCode, testDateOfRegistration))))
                mockUpsertEnrolment(testVatNumber, testPostCode, testDateOfRegistration.toTaxEnrolmentsFormat)(
                  Future.successful(Right(UpsertEnrolmentResponseHttpParser.UpsertEnrolmentSuccess))
                )
                mockAllocateEnrolmentWithoutKnownFacts(testGroupId, testCredentialId, testVatNumber)(
                  Future.successful(Left(AllocateEnrolmentResponseHttpParser.EnrolFailure(testErrorMsg)))
                )

                val res = await(TestAutoClaimEnrolmentService.autoClaimEnrolment(testVatNumber))

                res shouldBe Left(AutoClaimEnrolmentService.EnrolFailure(testErrorMsg))
              }
            }
          }
          "upsert the enrolment fails" when {
            "return Failure" in {
              mockGetGroupIdForLegacyVatEnrolment(testVatNumber)(
                Future.successful(Left(CheckEnrolmentAllocationService.EnrolmentAlreadyAllocated(testGroupId)))
              )
              mockGetUserIds(testVatNumber)(Future.successful(Right(QueryUsersHttpParser.UsersFound(testSetCredentialIds))))
              mockGetUsersForGroup(testGroupId)(
                Future.successful(Right(UsersFound(testMapCredentialRoles)))
              )
              mockGetKnownFacts(testVatNumber)(Future.successful(Right(KnownFactsHttpParser.KnownFacts(testPostCode, testDateOfRegistration))))
              mockUpsertEnrolment(testVatNumber, testPostCode, testDateOfRegistration.toTaxEnrolmentsFormat)(
                Future.successful(Left(UpsertEnrolmentResponseHttpParser.UpsertEnrolmentFailure(BAD_REQUEST, testErrorMsg)))
              )

              val res = await(TestAutoClaimEnrolmentService.autoClaimEnrolment(testVatNumber))

              res shouldBe Left(AutoClaimEnrolmentService.UpsertEnrolmentFailure(testErrorMsg))
            }
          }
        }
        "the known facts connector fails" when {
          "returns InvalidVatNumber" in {
            mockGetGroupIdForLegacyVatEnrolment(testVatNumber)(
              Future.successful(Left(CheckEnrolmentAllocationService.EnrolmentAlreadyAllocated(testGroupId)))
            )
            mockGetUserIds(testVatNumber)(Future.successful(Right(QueryUsersHttpParser.UsersFound(testSetCredentialIds))))
            mockGetUsersForGroup(testGroupId)(
              Future.successful(Right(UsersFound(testMapCredentialRoles)))
            )
            mockGetKnownFacts(testVatNumber)(Future.successful(Left(KnownFactsHttpParser.InvalidVatNumber)))

            val res = await(TestAutoClaimEnrolmentService.autoClaimEnrolment(testVatNumber))

            res shouldBe Left(InvalidVatNumber)
          }
          "returns VatNumberNotFound" in {
            mockGetGroupIdForLegacyVatEnrolment(testVatNumber)(
              Future.successful(Left(CheckEnrolmentAllocationService.EnrolmentAlreadyAllocated(testGroupId)))
            )
            mockGetUserIds(testVatNumber)(Future.successful(Right(QueryUsersHttpParser.UsersFound(testSetCredentialIds))))
            mockGetUsersForGroup(testGroupId)(
              Future.successful(Right(UsersFound(testMapCredentialRoles)))
            )
            mockGetKnownFacts(testVatNumber)(Future.successful(Left(KnownFactsHttpParser.VatNumberNotFound)))

            val res = await(TestAutoClaimEnrolmentService.autoClaimEnrolment(testVatNumber))

            res shouldBe Left(VatNumberNotFound)
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

          val res = await(TestAutoClaimEnrolmentService.autoClaimEnrolment(testVatNumber))

          res shouldBe Left(NoAdminUsers)
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

          val res = await(TestAutoClaimEnrolmentService.autoClaimEnrolment(testVatNumber))

          res shouldBe Left(UsersGroupsSearchFailure)
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

          val res = await(TestAutoClaimEnrolmentService.autoClaimEnrolment(testVatNumber))

          res shouldBe Left(NoAdminUsers)
        }
      }
      "legacy user IDS don't exist and the set is empty" when {
        "returns NoUsersFound" in {
          mockGetGroupIdForLegacyVatEnrolment(testVatNumber)(
            Future.successful(Left(CheckEnrolmentAllocationService.EnrolmentAlreadyAllocated(testGroupId)))
          )
          mockGetUserIds(testVatNumber)(Future.successful(Right(QueryUsersHttpParser.UsersFound(Set()))))

          val res = await(TestAutoClaimEnrolmentService.autoClaimEnrolment(testVatNumber))

          res shouldBe Left(EnrolmentStoreProxyConnectionFailure)
        }
      }
      "legacy user IDS are not returned" when {
        "returns NoUsersFound" in {
          mockGetGroupIdForLegacyVatEnrolment(testVatNumber)(
            Future.successful(Left(CheckEnrolmentAllocationService.EnrolmentAlreadyAllocated(testGroupId)))
          )
          mockGetUserIds(testVatNumber)(Future.successful(Right(QueryUsersHttpParser.NoUsersFound)))

          val res = await(TestAutoClaimEnrolmentService.autoClaimEnrolment(testVatNumber))

          res shouldBe Left(NoUsersFound)
        }
      }
      "legacy user IDs are not returned" when {
        "returns ConnectionFailure" in {
          mockGetGroupIdForLegacyVatEnrolment(testVatNumber)(
            Future.successful(Left(CheckEnrolmentAllocationService.EnrolmentAlreadyAllocated(testGroupId)))
          )
          mockGetUserIds(testVatNumber)(Future.successful(Left(QueryUsersHttpParser.EnrolmentStoreProxyConnectionFailure(BAD_REQUEST))))

          val res = await(TestAutoClaimEnrolmentService.autoClaimEnrolment(testVatNumber))

          res shouldBe Left(EnrolmentStoreProxyConnectionFailure)
        }
      }
    }
    "legacy group ID is not returned" when {
      "returns EnrolmentNotAllocated" in {
        mockGetGroupIdForLegacyVatEnrolment(testVatNumber)(
          Future.successful(Right(CheckEnrolmentAllocationService.EnrolmentNotAllocated))
        )

        val res = await(TestAutoClaimEnrolmentService.autoClaimEnrolment(testVatNumber))

        res shouldBe Left(EnrolmentNotAllocated)
      }
    }
    "legacy group ID is not returned" when {
      "returns EnrolmentStoreProxyFailure" in {
        mockGetGroupIdForLegacyVatEnrolment(testVatNumber)(
          Future.successful(Left(CheckEnrolmentAllocationService.UnexpectedEnrolmentStoreProxyFailure(BAD_REQUEST)))
        )

        val res = await(TestAutoClaimEnrolmentService.autoClaimEnrolment(testVatNumber))

        res shouldBe Left(EnrolmentStoreProxyFailure(BAD_REQUEST))

      }
    }
  }
}
