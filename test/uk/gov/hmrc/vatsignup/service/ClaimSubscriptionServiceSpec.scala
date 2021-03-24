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
import uk.gov.hmrc.http.{ForbiddenException, HeaderCarrier, InternalServerException}
import uk.gov.hmrc.vatsignup.connectors.mocks._
import uk.gov.hmrc.vatsignup.helpers.TestConstants._
import uk.gov.hmrc.vatsignup.httpparsers.AllocateEnrolmentResponseHttpParser.EnrolSuccess
import uk.gov.hmrc.vatsignup.httpparsers.VatCustomerDetailsHttpParser
import uk.gov.hmrc.vatsignup.models.KnownFacts
import uk.gov.hmrc.vatsignup.models.monitoring.ClaimSubscriptionAuditing.ClaimSubscriptionAuditModel
import uk.gov.hmrc.vatsignup.service.mocks.MockCheckEnrolmentAllocationService
import uk.gov.hmrc.vatsignup.service.mocks.monitoring.MockAuditService
import uk.gov.hmrc.vatsignup.services.ClaimSubscriptionService._
import uk.gov.hmrc.vatsignup.services.{CheckEnrolmentAllocationService, ClaimSubscriptionService}
import uk.gov.hmrc.vatsignup.utils.KnownFactsDateFormatter.KnownFactsDateFormatter

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ClaimSubscriptionServiceSpec extends WordSpec with Matchers
  with MockVatCustomerDetailsConnector
  with MockAuthConnector
  with MockTaxEnrolmentsConnector
  with MockEnrolmentStoreProxyConnector
  with MockCheckEnrolmentAllocationService
  with MockAuditService {

  object TestClaimSubscriptionService extends ClaimSubscriptionService(
    mockAuthConnector,
    mockVatCustomerDetailsConnector,
    mockEnrolmentStoreProxyConnector,
    mockCheckEnrolmentAllocationService,
    mockAuditService
  )

  implicit val hc: HeaderCarrier = HeaderCarrier()
  implicit val request: Request[_] = FakeRequest()

  "claimSubscription" when {
    "the known facts connector is successful" when {
      "auth returns a valid ggw credential and group ID" when {
        "CheckEnrolmentAllocation returns EnrolmentNotAllocated" when {
          "tax enrolment to allocate enrolment returns a success" should {
            "return SubscriptionClaimed" in {
              mockGetVatCustomerDetails(testVatNumber)(Future.successful(Right(testVatCustomerDetails)))
              mockAuthRetrieveCredentialAndGroupId(testCredentials, Some(testGroupId))
              mockGetGroupIdForMtdVatEnrolment(testVatNumber, ignoreAssignments = true)(
                Future.successful(Right(CheckEnrolmentAllocationService.EnrolmentNotAllocated))
              )
              mockAllocateEnrolmentWithoutKnownFacts(
                testGroupId,
                testCredentialId,
                testVatNumber
              )(Future.successful(Right(EnrolSuccess)))

              val res = await(TestClaimSubscriptionService.claimSubscription(
                testVatNumber,
                Some(testPostCode),
                testDateOfRegistration,
                isFromBta = false
              ))

              res shouldBe Right(SubscriptionClaimed)
              verifyAudit(ClaimSubscriptionAuditModel(
                testVatNumber,
                isFromBta = false,
                isSuccess = true
              ))
            }

            "return SubscriptionClaimed when postcodes have mismatching cases" in {
              val testPostCode1 = "zz11 1ZZ"
              val testPostCode2 = "ZZ11 1zz"

              mockGetVatCustomerDetails(testVatNumber)(Future.successful(Right(testVatCustomerDetails.copy(
                knownFacts = KnownFacts(Some(testPostCode1), testDateOfRegistration)
              ))))
              mockAuthRetrieveCredentialAndGroupId(testCredentials, Some(testGroupId))
              mockGetGroupIdForMtdVatEnrolment(testVatNumber, ignoreAssignments = true)(
                Future.successful(Right(CheckEnrolmentAllocationService.EnrolmentNotAllocated))
              )
              mockAllocateEnrolmentWithoutKnownFacts(
                testGroupId,
                testCredentialId,
                testVatNumber
              )(Future.successful(Right(EnrolSuccess)))

              val res = await(TestClaimSubscriptionService.claimSubscription(
                testVatNumber,
                Some(testPostCode2),
                testDateOfRegistration,
                isFromBta = false
              ))

              res shouldBe Right(SubscriptionClaimed)
              verifyAudit(ClaimSubscriptionAuditModel(
                testVatNumber,
                isFromBta = false,
                isSuccess = true
              ))
            }

            "return SubscriptionClaimed when vrn is overseas" in {
              mockGetVatCustomerDetails(testVatNumber)(Future.successful(Right(testVatCustomerDetails)))
              mockAuthRetrieveCredentialAndGroupId(testCredentials, Some(testGroupId))
              mockGetGroupIdForMtdVatEnrolment(testVatNumber, ignoreAssignments = true)(
                Future.successful(Right(CheckEnrolmentAllocationService.EnrolmentNotAllocated))
              )
              mockAllocateEnrolmentWithoutKnownFacts(
                testGroupId,
                testCredentialId,
                testVatNumber
              )(Future.successful(Right(EnrolSuccess)))

              val res = await(TestClaimSubscriptionService.claimSubscription(
                testVatNumber,
                None,
                testDateOfRegistration,
                isFromBta = false
              ))

              res shouldBe Right(SubscriptionClaimed)
              verifyAudit(ClaimSubscriptionAuditModel(
                testVatNumber,
                isFromBta = false,
                isSuccess = true
              ))
            }
          }
          "tax enrolment to allocate enrolment returns a failure" should {
            "throw an exception" in {
              val allocateEnrolmentFailureMessage = "allocateEnrolmentFailure"
              mockGetVatCustomerDetails(testVatNumber)(Future.successful(Right(testVatCustomerDetails)))
              mockAuthRetrieveCredentialAndGroupId(testCredentials, Some(testGroupId))
              mockGetGroupIdForMtdVatEnrolment(testVatNumber, ignoreAssignments = true)(
                Future.successful(Right(CheckEnrolmentAllocationService.EnrolmentNotAllocated))
              )
              mockAllocateEnrolmentWithoutKnownFacts(
                testGroupId,
                testCredentialId,
                testVatNumber
              )(Future.failed(new InternalServerException(allocateEnrolmentFailureMessage)))

              intercept[InternalServerException] {
                await(TestClaimSubscriptionService.claimSubscription(
                  testVatNumber,
                  Some(testPostCode),
                  testDateOfRegistration,
                  isFromBta = true
                ))

                verifyAudit(ClaimSubscriptionAuditModel(
                  testVatNumber,
                  isFromBta = true,
                  isSuccess = false,
                  allocateEnrolmentFailureMessage = Some(allocateEnrolmentFailureMessage)
                ))
              }
            }
          }
          "allocate enrolment returns a success" should {
            "return SubscriptionClaimed" in {
              mockGetVatCustomerDetails(testVatNumber)(Future.successful(Right(testVatCustomerDetails)))
              mockAuthRetrieveCredentialAndGroupId(testCredentials, Some(testGroupId))
              mockGetGroupIdForMtdVatEnrolment(testVatNumber, ignoreAssignments = true)(
                Future.successful(Right(CheckEnrolmentAllocationService.EnrolmentNotAllocated))
              )
              mockAllocateEnrolmentWithoutKnownFacts(
                testGroupId,
                testCredentialId,
                testVatNumber
              )(Future.successful(Right(EnrolSuccess)))

              val res = await(TestClaimSubscriptionService.claimSubscription(
                testVatNumber,
                Some(testPostCode),
                testDateOfRegistration,
                isFromBta = false
              ))

              res shouldBe Right(SubscriptionClaimed)

              verifyAudit(ClaimSubscriptionAuditModel(
                testVatNumber,
                isFromBta = false,
                isSuccess = true
              ))
            }
          }

          "allocate enrolment returns a failure" should {
            "throw an exception" in {
              val allocateEnrolmentErrorMessage = "allocateEnrolErr"

              mockGetVatCustomerDetails(testVatNumber)(Future.successful(Right(testVatCustomerDetails)))
              mockAuthRetrieveCredentialAndGroupId(testCredentials, Some(testGroupId))
              mockGetGroupIdForMtdVatEnrolment(testVatNumber, ignoreAssignments = true)(
                Future.successful(Right(CheckEnrolmentAllocationService.EnrolmentNotAllocated))
              )
              mockAllocateEnrolmentWithoutKnownFacts(
                testGroupId,
                testCredentialId,
                testVatNumber
              )(Future.failed(new InternalServerException(allocateEnrolmentErrorMessage)))

              intercept[InternalServerException] {
                await(TestClaimSubscriptionService.claimSubscription(
                  testVatNumber,
                  Some(testPostCode),
                  testDateOfRegistration,
                  isFromBta = true
                ))

                verifyAudit(ClaimSubscriptionAuditModel(
                  testVatNumber,
                  isFromBta = true,
                  isSuccess = false,
                  allocateEnrolmentFailureMessage = Some(allocateEnrolmentErrorMessage))
                )
              }
            }
          }
        }

        "the enrolment is already allocated, CheckEnrolmentAllocation" should {
          "return a EnrolmentAlreadyAllocated" in {
            mockAuthRetrieveCredentialAndGroupId(testCredentials, Some(testGroupId))
            mockGetGroupIdForMtdVatEnrolment(testVatNumber, ignoreAssignments = true)(
              Future.successful(Left(CheckEnrolmentAllocationService.EnrolmentAlreadyAllocated(testGroupId)))
            )

            val res = await(TestClaimSubscriptionService.claimSubscription(
              testVatNumber,
              Some(testPostCode),
              testDateOfRegistration,
              isFromBta = true
            ))

            res shouldBe Left(EnrolmentAlreadyAllocated)
          }
        }

        "CheckEnrolmentAllocation fails" should {
          "return an UnexpectedEnrolmentStoreProxyFailure and the status code" in {
            mockAuthRetrieveCredentialAndGroupId(testCredentials, Some(testGroupId))
            mockGetGroupIdForMtdVatEnrolment(testVatNumber, ignoreAssignments = true)(
              Future.successful(Left(CheckEnrolmentAllocationService.UnexpectedEnrolmentStoreProxyFailure(BAD_REQUEST)))
            )

            val res = await(TestClaimSubscriptionService.claimSubscription(
              testVatNumber,
              Some(testPostCode),
              testDateOfRegistration,
              isFromBta = true
            ))

            res shouldBe Left(CheckEnrolmentAllocationFailed(BAD_REQUEST))
          }
        }
      }

      "the supplied known facts do not match what is held on ETMP" should {
        "return KnownFactsMismatch" in {
          mockGetVatCustomerDetails(testVatNumber)(Future.successful(Right(testVatCustomerDetails)))
          mockGetGroupIdForMtdVatEnrolment(testVatNumber, ignoreAssignments = true)(Future.successful(Right(CheckEnrolmentAllocationService.EnrolmentNotAllocated)))
          mockAuthRetrieveCredentialAndGroupId(testCredentials, Some(testGroupId))

          val nonMatchingPostcode = "ZZ2 2ZZ"

          val res = await(TestClaimSubscriptionService.claimSubscription(
            testVatNumber,
            Some(nonMatchingPostcode),
            testDateOfRegistration,
            isFromBta = true
          ))

          res shouldBe Left(KnownFactsMismatch)
        }
      }
    }
    "auth does not return a valid credential" should {
      "return InvalidCredential" in {
        mockGetVatCustomerDetails(testVatNumber)(Future.successful(Right(testVatCustomerDetails)))
        mockGetGroupIdForMtdVatEnrolment(testVatNumber, ignoreAssignments = true)(Future.successful(Right(CheckEnrolmentAllocationService.EnrolmentNotAllocated)))
        mockAuthRetrieveCredentialAndGroupId(testCredentials, None)

        val res = TestClaimSubscriptionService.claimSubscription(
          testVatNumber,
          Some(testPostCode),
          testDateOfRegistration,
          isFromBta = false
        )

        intercept[ForbiddenException](await(res))
      }
    }
    "the known facts connector returns invalid VAT number" should {
      "return InvalidVatNumber" in {
        mockGetVatCustomerDetails(testVatNumber)(Future.successful(Left(VatCustomerDetailsHttpParser.InvalidVatNumber)))
        mockGetGroupIdForMtdVatEnrolment(testVatNumber, ignoreAssignments = true)(Future.successful(Right(CheckEnrolmentAllocationService.EnrolmentNotAllocated)))

        val res = await(TestClaimSubscriptionService.claimSubscription(
          testVatNumber,
          Some(testPostCode),
          testDateOfRegistration,
          isFromBta = false
        ))

        res shouldBe Left(InvalidVatNumber)
      }
    }
    "the known facts connector returns VAT number not found" should {
      "return InvalidVatNumber" in {
        mockGetVatCustomerDetails(testVatNumber)(Future.successful(Left(VatCustomerDetailsHttpParser.VatNumberNotFound)))
        mockGetGroupIdForMtdVatEnrolment(testVatNumber, ignoreAssignments = true)(Future.successful(Right(CheckEnrolmentAllocationService.EnrolmentNotAllocated)))

        val res = await(TestClaimSubscriptionService.claimSubscription(
          testVatNumber,
          Some(testPostCode),
          testDateOfRegistration,
          isFromBta = false
        ))

        res shouldBe Left(VatNumberNotFound)
      }
    }
  }

  "claimSubscriptionWithEnrolment" when {
    "the known facts connector is successful" when {
      "auth returns a valid ggw credential and group ID" when {
        "CheckEnrolmentAllocatjon returns EnrolmentNotAllocated" when {
          "tax enrolment to allocate enrolment returns a success" should {
            "return SubscriptionClaimed" in {
              mockGetVatCustomerDetails(testVatNumber)(Future.successful(Right(testVatCustomerDetails)))
              mockAuthRetrieveCredentialAndGroupId(testCredentials, Some(testGroupId))
              mockGetGroupIdForMtdVatEnrolment(testVatNumber, ignoreAssignments = true)(
                Future.successful(Right(CheckEnrolmentAllocationService.EnrolmentNotAllocated))
              )
              mockAllocateEnrolmentWithoutKnownFacts(
                testGroupId,
                testCredentialId,
                testVatNumber
              )(Future.successful(Right(EnrolSuccess)))

              val res = await(TestClaimSubscriptionService.claimSubscriptionWithEnrolment(
                testVatNumber,
                isFromBta = false
              ))

              res shouldBe Right(SubscriptionClaimed)
              verifyAudit(ClaimSubscriptionAuditModel(
                testVatNumber,
                isFromBta = false,
                isSuccess = true
              ))
            }
          }
          "the enrolment is already allocated, CheckEnrolmentAllocation" should {
            "return a EnrolmentAlreadyAllocated" in {
              mockAuthRetrieveCredentialAndGroupId(testCredentials, Some(testGroupId))
              mockGetGroupIdForMtdVatEnrolment(testVatNumber, ignoreAssignments = true)(
                Future.successful(Left(CheckEnrolmentAllocationService.EnrolmentAlreadyAllocated(testGroupId)))
              )

              val res = await(TestClaimSubscriptionService.claimSubscriptionWithEnrolment(
                vatNumber = testVatNumber,
                isFromBta = true
              ))

              res shouldBe Left(EnrolmentAlreadyAllocated)
            }
          }
        }
      }
    }
    "toTaxEnrolmentFormat" should {
      "convert the date to the correct format" in {
        "2017-01-01".toTaxEnrolmentsFormat shouldBe "01/01/17"
        "1999-01-01".toTaxEnrolmentsFormat shouldBe "01/01/99"
      }
    }
  }

}