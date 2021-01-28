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

import play.api.test.Helpers._
import play.api.test.FakeRequest
import uk.gov.hmrc.auth.core.Enrolments
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException, NotFoundException}
import org.scalatest.{WordSpec, Matchers}
import uk.gov.hmrc.vatsignup.helpers.TestConstants.testAgentEnrolment
import uk.gov.hmrc.vatsignup.service.mocks._
import uk.gov.hmrc.vatsignup.services.MigratedEnrolmentService.EnrolmentSuccess
import uk.gov.hmrc.vatsignup.services.{MigratedSignUpRequestService, MigratedSubmissionService}
import uk.gov.hmrc.vatsignup.services.MigratedSubmissionService._
import uk.gov.hmrc.vatsignup.helpers.TestConstants._
import uk.gov.hmrc.vatsignup.models.{MigratedSignUpRequest, SoleTrader}
import uk.gov.hmrc.vatsignup.services.MigratedSignUpService.MigratedSignUpSuccess
import uk.gov.hmrc.vatsignup.repositories.mocks.MockSubscriptionRequestRepository

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class MigratedSubmissionServiceSpec extends WordSpec with Matchers
  with MockMigratedSignUpRequestService
  with MockMigratedRegistrationService
  with MockMigratedSignUpService
  with MockMigratedEnrolmentService
  with MockSubscriptionRequestRepository {

  object TestMigratedSubmissionService extends MigratedSubmissionService(
    mockMigratedSignUpRequestService,
    mockMigratedRegistrationService,
    mockMigratedSignUpService,
    mockMigratedEnrolmentService,
    mockSubscriptionRequestRepository
  )

  val testSignUpRequest = MigratedSignUpRequest(
    vatNumber = testVatNumber,
    businessEntity = testSoleTrader,
    isDelegated = true,
    isMigratable = true
  )

  val enrolments = Enrolments(Set(testAgentEnrolment))
  implicit val hc = HeaderCarrier()
  implicit val req = FakeRequest()

  "submit" when {
    "the SignUpRequest is successfully retrieved" when {
      "the business entity is successfully registered" when {
        "the customer is successfully signed up" when {
          "the MTD enrolment is successfully allocated" when {
            "the sign up request is successfully deleted" should {
              s"return $SubmissionSuccess" in {
                mockGetSignUpRequest(testVatNumber, enrolments)(Future.successful(testSignUpRequest))
                mockRegisterBusinessEntity(testVatNumber, SoleTrader(testNino), Some(testAgentReferenceNumber))(Future.successful(testSafeId))
                mockSignUp(testSafeId, testVatNumber, isPartialMigration = false, Some(testAgentReferenceNumber))(Future.successful(MigratedSignUpSuccess))
                mockEnrolForMtd(testVatNumber, testSafeId)(Future.successful(EnrolmentSuccess))
                mockDeleteSignUpRequest(testVatNumber)(Future.successful(MigratedSignUpRequestService.SignUpRequestDeleted))

                val res = await(TestMigratedSubmissionService.submit(testVatNumber, enrolments))

                res shouldBe SubmissionSuccess
              }
            }
            "the sign up request deletion fails" should {
              "raise an InternalServerException" in {
                val exception = new InternalServerException("Database failure: Failed to delete MigratedSignUpRequest")

                mockGetSignUpRequest(testVatNumber, enrolments)(Future.successful(testSignUpRequest))
                mockRegisterBusinessEntity(testVatNumber, SoleTrader(testNino), Some(testAgentReferenceNumber))(Future.successful(testSafeId))
                mockSignUp(testSafeId, testVatNumber, isPartialMigration = false, Some(testAgentReferenceNumber))(Future.successful(MigratedSignUpSuccess))
                mockEnrolForMtd(testVatNumber, testSafeId)(Future.successful(EnrolmentSuccess))
                mockDeleteSignUpRequest(testVatNumber)(Future.failed(exception))

                intercept[InternalServerException] {
                  await(TestMigratedSubmissionService.submit(testVatNumber, enrolments))
                }
              }
            }
          }
          "The enrolment fails" should {
            "raise an InternalServerException" in {
              mockGetSignUpRequest(testVatNumber, enrolments)(Future.successful(testSignUpRequest))
              mockRegisterBusinessEntity(testVatNumber, SoleTrader(testNino), Some(testAgentReferenceNumber))(Future.successful(testSafeId))
              mockSignUp(testSafeId, testVatNumber, isPartialMigration = false, Some(testAgentReferenceNumber))(Future.successful(MigratedSignUpSuccess))
              mockEnrolForMtd(testVatNumber, testSafeId)(Future.failed(new InternalServerException("")))

              intercept[InternalServerException] {
                await(TestMigratedSubmissionService.submit(testVatNumber, enrolments))
              }
            }
          }
        }
        "the sign up fails" should {
          "return SignUpFailure" in {
            mockGetSignUpRequest(testVatNumber, enrolments)(Future.successful(testSignUpRequest))
            mockRegisterBusinessEntity(testVatNumber, SoleTrader(testNino), Some(testAgentReferenceNumber))(Future.successful(testSafeId))
            mockSignUp(testSafeId, testVatNumber, isPartialMigration = false, Some(testAgentReferenceNumber))(
              Future.failed(new InternalServerException(""))
            )

            intercept[InternalServerException] {
              await(TestMigratedSubmissionService.submit(testVatNumber, enrolments))
            }
          }
        }
      }
      "the business entity registration fails" should {
        "raise an InternalServerException" in {
          mockGetSignUpRequest(testVatNumber, enrolments)(Future.successful(testSignUpRequest))
          mockRegisterBusinessEntity(testVatNumber, SoleTrader(testNino), Some(testAgentReferenceNumber))(
            Future.failed(new InternalServerException(""))
          )

          intercept[InternalServerException] {
            await(TestMigratedSubmissionService.submit(testVatNumber, enrolments))
          }
        }
      }
    }
    "the SignUpRequest retrieval fails" when {
      "the sign up request cannot be found for the vat number" should {
        "raise a NotFoundException" in {
          mockGetSignUpRequest(testVatNumber, enrolments)(Future.failed(new NotFoundException("")))

          intercept[NotFoundException] {
            await(TestMigratedSubmissionService.submit(testVatNumber, enrolments))
          }
        }
      }
      s"the connection to the subscriptionRequestRepository cannot be made" should {
        "raise an InternalServerException" in {
          val exception = new InternalServerException("Database failure: Failed to retrieve MigratedSignUpRequest")

          mockGetSignUpRequest(testVatNumber, enrolments)(Future.failed(exception))

          intercept[InternalServerException] {
            await(TestMigratedSubmissionService.submit(testVatNumber, enrolments))
          }
        }
      }
      s"the response is ${MigratedSignUpRequestService.InsufficientData}" should {
        "raise an InternalServerException" in {
          mockGetSignUpRequest(testVatNumber, enrolments)(Future.failed(new InternalServerException("")))

          intercept[InternalServerException] {
            await(TestMigratedSubmissionService.submit(testVatNumber, enrolments))
          }
        }
      }
    }
  }

}
