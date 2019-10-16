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

import play.api.test.FakeRequest
import uk.gov.hmrc.auth.core.Enrolments
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.vatsignup.helpers.TestConstants.testAgentEnrolment
import uk.gov.hmrc.vatsignup.service.mocks._
import uk.gov.hmrc.vatsignup.services.MigratedEnrolmentService.{EnrolmentFailure, EnrolmentSuccess}
import uk.gov.hmrc.vatsignup.services.{MigratedSignUpRequestService, MigratedSubmissionService}
import uk.gov.hmrc.vatsignup.services.MigratedSubmissionService._
import uk.gov.hmrc.vatsignup.helpers.TestConstants._
import uk.gov.hmrc.vatsignup.models.{MigratedSignUpRequest, SoleTrader}
import uk.gov.hmrc.vatsignup.services.MigratedSignUpService.{MigratedSignUpFailure, MigratedSignUpSuccess}
import play.api.http.Status._
import uk.gov.hmrc.vatsignup.repositories.mocks.MockSubscriptionRequestRepository
import uk.gov.hmrc.vatsignup.services.MigratedRegistrationService.RegisterWithMultipleIdsFailure

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class MigratedSubmissionServiceSpec extends UnitSpec
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
                mockGetSignUpRequest(testVatNumber, enrolments)(Future.successful(Right(testSignUpRequest)))
                mockRegisterBusinessEntity(testVatNumber, SoleTrader(testNino), Some(testAgentReferenceNumber))(Future.successful(Right(testSafeId)))
                mockSignUp(testSafeId, testVatNumber, Some(testAgentReferenceNumber), false)(Future.successful(Right(MigratedSignUpSuccess)))
                mockEnrolForMtd(testVatNumber, testSafeId)(Future.successful(Right(EnrolmentSuccess)))
                mockDeleteSignUpRequest(testVatNumber)(Future.successful(Right(MigratedSignUpRequestService.SignUpRequestDeleted)))

                val res = await(TestMigratedSubmissionService.submit(testVatNumber, enrolments))

                res shouldBe Right(SubmissionSuccess)
              }
            }
            "the sign up request deletion fails" should {
              "return DeleteRecordFailure" in {
                mockGetSignUpRequest(testVatNumber, enrolments)(Future.successful(Right(testSignUpRequest)))
                mockRegisterBusinessEntity(testVatNumber, SoleTrader(testNino), Some(testAgentReferenceNumber))(Future.successful(Right(testSafeId)))
                mockSignUp(testSafeId, testVatNumber, Some(testAgentReferenceNumber), false)(Future.successful(Right(MigratedSignUpSuccess)))
                mockEnrolForMtd(testVatNumber, testSafeId)(Future.successful(Right(EnrolmentSuccess)))
                mockDeleteSignUpRequest(testVatNumber)(Future.successful(Left(MigratedSignUpRequestService.DeleteRecordFailure)))

                val res = await(TestMigratedSubmissionService.submit(testVatNumber, enrolments))

                res shouldBe Left(MigratedSubmissionService.DeleteRecordFailure)
              }
            }
          }
          "The enrolment fails" should {
            s"return $EnrolmentFailure" in {
              mockGetSignUpRequest(testVatNumber, enrolments)(Future.successful(Right(testSignUpRequest)))
              mockRegisterBusinessEntity(testVatNumber, SoleTrader(testNino), Some(testAgentReferenceNumber))(Future.successful(Right(testSafeId)))
              mockSignUp(testSafeId, testVatNumber, Some(testAgentReferenceNumber),false)(Future.successful(Right(MigratedSignUpSuccess)))
              mockEnrolForMtd(testVatNumber, testSafeId)(Future.successful(Left(EnrolmentFailure(BAD_REQUEST))))

              val res = await(TestMigratedSubmissionService.submit(testVatNumber, enrolments))

              res shouldBe Left(MigratedSubmissionService.EnrolmentFailure)
            }
          }
        }
        "the sign up fails" should {
          "return SignUpFailure" in {
            mockGetSignUpRequest(testVatNumber, enrolments)(Future.successful(Right(testSignUpRequest)))
            mockRegisterBusinessEntity(testVatNumber, SoleTrader(testNino), Some(testAgentReferenceNumber))(Future.successful(Right(testSafeId)))
            mockSignUp(testSafeId, testVatNumber, Some(testAgentReferenceNumber),false)(
              Future.successful(Left(MigratedSignUpFailure(BAD_REQUEST, "")))
            )

            val res = await(TestMigratedSubmissionService.submit(testVatNumber, enrolments))

            res shouldBe Left(SignUpFailure)
          }
        }
      }
      "the business entity registration fails" should {
        "return RegistrationFailure" in {
          mockGetSignUpRequest(testVatNumber, enrolments)(Future.successful(Right(testSignUpRequest)))
          mockRegisterBusinessEntity(testVatNumber, SoleTrader(testNino), Some(testAgentReferenceNumber))(
            Future.successful(Left(RegisterWithMultipleIdsFailure(BAD_REQUEST, "")))
          )

          val res = await(TestMigratedSubmissionService.submit(testVatNumber, enrolments))

          res shouldBe Left(RegistrationFailure)
        }
      }
    }
    "the SignUpRequest retrieval fails" when {
      s"the response is ${MigratedSignUpRequestService.SignUpRequestNotFound}" should {
        s"return $InsufficientData" in {
          mockGetSignUpRequest(testVatNumber, enrolments)(Future.successful(Left(MigratedSignUpRequestService.SignUpRequestNotFound)))

          val res = await(TestMigratedSubmissionService.submit(testVatNumber, enrolments))

          res shouldBe Left(InsufficientData)
        }
      }
      s"the response is ${MigratedSignUpRequestService.DatabaseFailure}" should {
        s"return $DatabaseFailure" in {
          mockGetSignUpRequest(testVatNumber, enrolments)(Future.successful(Left(MigratedSignUpRequestService.DatabaseFailure)))

          val res = await(TestMigratedSubmissionService.submit(testVatNumber, enrolments))

          res shouldBe Left(DatabaseFailure)
        }
      }
      s"the response is ${MigratedSignUpRequestService.InsufficientData}" should {
        s"return $InsufficientData" in {
          mockGetSignUpRequest(testVatNumber, enrolments)(Future.successful(Left(MigratedSignUpRequestService.InsufficientData)))

          val res = await(TestMigratedSubmissionService.submit(testVatNumber, enrolments))

          res shouldBe Left(InsufficientData)
        }
      }
      s"the response is ${MigratedSignUpRequestService.RequestNotAuthorised}" should {
        s"return $InsufficientData" in {
          mockGetSignUpRequest(testVatNumber, enrolments)(Future.successful(Left(MigratedSignUpRequestService.RequestNotAuthorised)))

          val res = await(TestMigratedSubmissionService.submit(testVatNumber, enrolments))

          res shouldBe Left(InsufficientData)
        }
      }
    }
  }

}
