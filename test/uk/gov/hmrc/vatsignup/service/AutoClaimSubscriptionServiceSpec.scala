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
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.vatsignup.connectors.mocks.{MockEnrolmentStoreProxyConnector, MockKnownFactsConnector, MockTaxEnrolmentsConnector}
import uk.gov.hmrc.vatsignup.helpers.TestConstants._
import uk.gov.hmrc.vatsignup.httpparsers._
import uk.gov.hmrc.vatsignup.service.mocks.MockAssignEnrolmentToUserService
import uk.gov.hmrc.vatsignup.services.AutoClaimEnrolmentService._
import uk.gov.hmrc.vatsignup.services.{AssignEnrolmentToUserService, AutoClaimEnrolmentService}
import uk.gov.hmrc.vatsignup.utils.KnownFactsDateFormatter.KnownFactsDateFormatter

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


class AutoClaimSubscriptionServiceSpec extends UnitSpec with MockKnownFactsConnector with MockTaxEnrolmentsConnector
  with MockEnrolmentStoreProxyConnector with MockAssignEnrolmentToUserService {

  object TestAutoClaimEnrolmentService extends AutoClaimEnrolmentService(
    mockKnownFactsConnector,
    mockTaxEnrolmentsConnector,
    mockEnrolmentStoreProxyConnector,
    mockAssignEnrolmentToUserService
  )

  implicit val hc: HeaderCarrier = HeaderCarrier()
  implicit val request: Request[_] = FakeRequest()

  val testSetCredentialIds = Set(testCredentialId)

  "auto claim enrolment" should {
    "legacy group ID is returned" when {
      "legacy user IDs are returned" when {
        "the known facts connector is successful" when {
          "upsert the enrolment is successful" when {
            "allocate the new group ID is successful" when {
              "assign the new user IDs is successful" when {
                "returns successful" in {
                  mockGetAllocatedEnrolment(testVatNumber)(Future.successful(Right(EnrolmentStoreProxyHttpParser.EnrolmentAlreadyAllocated(testGroupId))))
                  mockGetUserIds(testVatNumber)(Future.successful(Right(QueryUsersHttpParser.UsersFound(testSetCredentialIds))))
                  mockGetKnownFacts(testVatNumber)(Future.successful(Right(KnownFactsHttpParser.KnownFacts(testPostCode, testDateOfRegistration))))
                  mockUpsertEnrolment(testVatNumber, testPostCode, testDateOfRegistration.toTaxEnrolmentsFormat)(
                    Future.successful(Right(UpsertEnrolmentResponseHttpParser.UpsertEnrolmentSuccess)))
                  mockAllocateEnrolmentWithoutKnownFacts(testGroupId, testCredentialId, testVatNumber)(
                    Future.successful(Right(AllocateEnrolmentResponseHttpParser.EnrolSuccess)))
                  mockAssignEnrolmentToUser(testSetCredentialIds, testVatNumber)(
                    Future.successful(Right(AssignEnrolmentToUserService.EnrolmentAssignedToUsers)))

                  val res = await(TestAutoClaimEnrolmentService.autoClaimEnrolment(testVatNumber))

                  res shouldBe Right(AutoClaimEnrolmentService.EnrolmentAssigned)
                }
              }
              "assign the new user IDs fails" when {
                "return Failure" in {
                  mockGetAllocatedEnrolment(testVatNumber)(Future.successful(Right(EnrolmentStoreProxyHttpParser.EnrolmentAlreadyAllocated(testGroupId))))
                  mockGetUserIds(testVatNumber)(Future.successful(Right(QueryUsersHttpParser.UsersFound(testSetCredentialIds))))
                  mockGetKnownFacts(testVatNumber)(Future.successful(Right(KnownFactsHttpParser.KnownFacts(testPostCode, testDateOfRegistration))))
                  mockUpsertEnrolment(testVatNumber, testPostCode, testDateOfRegistration.toTaxEnrolmentsFormat)(
                    Future.successful(Right(UpsertEnrolmentResponseHttpParser.UpsertEnrolmentSuccess)))
                  mockAllocateEnrolmentWithoutKnownFacts(testGroupId, testCredentialId, testVatNumber)(
                    Future.successful(Right(AllocateEnrolmentResponseHttpParser.EnrolSuccess)))
                  mockAssignEnrolmentToUser(testSetCredentialIds, testVatNumber)(
                    Future.successful(Left(AssignEnrolmentToUserService.EnrolmentAssignmentFailed)))

                  val res = await(TestAutoClaimEnrolmentService.autoClaimEnrolment(testVatNumber))

                  res shouldBe Left(AutoClaimEnrolmentService.EnrolmentAssignmentFailure)
                }
              }
            }
            "allocate the new group ID fails" when {
              "return Failure" in {
                mockGetAllocatedEnrolment(testVatNumber)(Future.successful(Right(EnrolmentStoreProxyHttpParser.EnrolmentAlreadyAllocated(testGroupId))))
                mockGetUserIds(testVatNumber)(Future.successful(Right(QueryUsersHttpParser.UsersFound(testSetCredentialIds))))
                mockGetKnownFacts(testVatNumber)(Future.successful(Right(KnownFactsHttpParser.KnownFacts(testPostCode, testDateOfRegistration))))
                mockUpsertEnrolment(testVatNumber, testPostCode, testDateOfRegistration.toTaxEnrolmentsFormat)(
                  Future.successful(Right(UpsertEnrolmentResponseHttpParser.UpsertEnrolmentSuccess)))
                mockAllocateEnrolmentWithoutKnownFacts(testGroupId, testCredentialId, testVatNumber)(
                  Future.successful(Left(AllocateEnrolmentResponseHttpParser.EnrolFailure(testErrorMsg))))

                val res = await(TestAutoClaimEnrolmentService.autoClaimEnrolment(testVatNumber))

                res shouldBe Left(AutoClaimEnrolmentService.EnrolFailure(testErrorMsg))
              }
            }
          }
          "upsert the enrolment fails" when {
            "return Failure" in {
              mockGetAllocatedEnrolment(testVatNumber)(Future.successful(Right(EnrolmentStoreProxyHttpParser.EnrolmentAlreadyAllocated(testGroupId))))
              mockGetUserIds(testVatNumber)(Future.successful(Right(QueryUsersHttpParser.UsersFound(testSetCredentialIds))))
              mockGetKnownFacts(testVatNumber)(Future.successful(Right(KnownFactsHttpParser.KnownFacts(testPostCode, testDateOfRegistration))))
              mockUpsertEnrolment(testVatNumber, testPostCode, testDateOfRegistration.toTaxEnrolmentsFormat)(
                Future.successful(Left(UpsertEnrolmentResponseHttpParser.UpsertEnrolmentFailure(BAD_REQUEST, testErrorMsg))))

              val res = await(TestAutoClaimEnrolmentService.autoClaimEnrolment(testVatNumber))

              res shouldBe Left(AutoClaimEnrolmentService.UpsertEnrolmentFailure(testErrorMsg))
            }
          }
        }
        "the known facts connector fails" when {
          "returns InvalidVatNumber" in {
            mockGetAllocatedEnrolment(testVatNumber)(Future.successful(Right(EnrolmentStoreProxyHttpParser.EnrolmentAlreadyAllocated(testGroupId))))
            mockGetUserIds(testVatNumber)(Future.successful(Right(QueryUsersHttpParser.UsersFound(testSetCredentialIds))))
            mockGetKnownFacts(testVatNumber)(Future.successful(Left(KnownFactsHttpParser.InvalidVatNumber)))

            val res = await(TestAutoClaimEnrolmentService.autoClaimEnrolment(testVatNumber))

            res shouldBe Left(InvalidVatNumber)
          }
          "returns VatNumberNotFound" in {
            mockGetAllocatedEnrolment(testVatNumber)(Future.successful(Right(EnrolmentStoreProxyHttpParser.EnrolmentAlreadyAllocated(testGroupId))))
            mockGetUserIds(testVatNumber)(Future.successful(Right(QueryUsersHttpParser.UsersFound(testSetCredentialIds))))
            mockGetKnownFacts(testVatNumber)(Future.successful(Left(KnownFactsHttpParser.VatNumberNotFound)))

            val res = await(TestAutoClaimEnrolmentService.autoClaimEnrolment(testVatNumber))

            res shouldBe Left(VatNumberNotFound)
          }
        }
      }
      "legacy user IDS don't exist and the set is empty" when {
        "returns NoUsersFound" in {
          mockGetAllocatedEnrolment(testVatNumber)(Future.successful(Right(EnrolmentStoreProxyHttpParser.EnrolmentAlreadyAllocated(testGroupId))))
          mockGetUserIds(testVatNumber)(Future.successful(Right(QueryUsersHttpParser.UsersFound(Set()))))

          val res = await(TestAutoClaimEnrolmentService.autoClaimEnrolment(testVatNumber))

          res shouldBe Left(EnrolmentStoreProxyConnectionFailure)
        }
      }
      "legacy user IDS are not returned" when {
        "returns NoUsersFound" in {
          mockGetAllocatedEnrolment(testVatNumber)(Future.successful(Right(EnrolmentStoreProxyHttpParser.EnrolmentAlreadyAllocated(testGroupId))))
          mockGetUserIds(testVatNumber)(Future.successful(Right(QueryUsersHttpParser.NoUsersFound)))

          val res = await(TestAutoClaimEnrolmentService.autoClaimEnrolment(testVatNumber))

          res shouldBe Left(NoUsersFound)
        }
      }
      "legacy user IDs are not returned" when {
        "returns ConnectionFailure" in {
          mockGetAllocatedEnrolment(testVatNumber)(Future.successful(Right(EnrolmentStoreProxyHttpParser.EnrolmentAlreadyAllocated(testGroupId))))
          mockGetUserIds(testVatNumber)(Future.successful(Left(QueryUsersHttpParser.EnrolmentStoreProxyConnectionFailure(BAD_REQUEST))))

          val res = await(TestAutoClaimEnrolmentService.autoClaimEnrolment(testVatNumber))

          res shouldBe Left(EnrolmentStoreProxyConnectionFailure)
        }
      }
    }
    "legacy group ID is not returned" when {
      "returns EnrolmentNotAllocated" in {
        mockGetAllocatedEnrolment(testVatNumber)(Future.successful(Right(EnrolmentStoreProxyHttpParser.EnrolmentNotAllocated)))

        val res = await(TestAutoClaimEnrolmentService.autoClaimEnrolment(testVatNumber))

        res shouldBe Left(EnrolmentNotAllocated)
      }
    }
    "legacy group ID is not returned" when {
      "returns EnrolmentStoreProxyFailure" in {
        mockGetAllocatedEnrolment(testVatNumber)(Future.successful(Left(EnrolmentStoreProxyHttpParser.EnrolmentStoreProxyFailure(BAD_REQUEST))))

        val res = await(TestAutoClaimEnrolmentService.autoClaimEnrolment(testVatNumber))

        res shouldBe Left(EnrolmentStoreProxyFailure(BAD_REQUEST))

      }
    }
  }
}
