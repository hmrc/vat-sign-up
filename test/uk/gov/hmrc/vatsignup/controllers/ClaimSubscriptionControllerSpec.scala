/*
 * Copyright 2020 HM Revenue & Customs
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

package uk.gov.hmrc.vatsignup.controllers

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import org.scalatest.{Matchers, WordSpec}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.Enrolments
import uk.gov.hmrc.vatsignup.connectors.mocks.MockAuthConnector
import uk.gov.hmrc.vatsignup.helpers.TestConstants._
import uk.gov.hmrc.vatsignup.models.ClaimSubscriptionRequest
import uk.gov.hmrc.vatsignup.service.mocks.MockClaimSubscriptionService
import uk.gov.hmrc.vatsignup.services.ClaimSubscriptionService._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ClaimSubscriptionControllerSpec extends WordSpec with Matchers with MockAuthConnector with MockClaimSubscriptionService {

  object TestClaimSubscriptionController
    extends ClaimSubscriptionController(mockAuthConnector, mockClaimSubscriptionService, stubControllerComponents())

  implicit val system: ActorSystem = ActorSystem()
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  val enrolments: Enrolments = Enrolments(Set(testAgentEnrolment))

  "claimSubscription" when {
    "the user has a matching VATDEC enrolment" when {
      "claim subscription is successful" should {
        "return NO_CONTENT" in {
          val isFromBta = true
          val request = FakeRequest().withBody(ClaimSubscriptionRequest(None, None, isFromBta))

          mockAuthRetrievePrincipalEnrolment()
          mockClaimSubscriptionWithEnrolment(testVatNumber, isFromBta)(Future.successful(Right(SubscriptionClaimed)))

          val res = TestClaimSubscriptionController.claimSubscription(testVatNumber)(request)

          status(res) shouldBe NO_CONTENT
        }
      }

      "claim subscription fails on the enrol call" should {
        "return BAD_GATEWAY" in {
          val isFromBta = true
          val request = FakeRequest().withBody(ClaimSubscriptionRequest(None, None, isFromBta))

          mockAuthRetrievePrincipalEnrolment()
          mockClaimSubscriptionWithEnrolment(testVatNumber, isFromBta)(Future.successful(Left(AllocationFailure)))

          val res = TestClaimSubscriptionController.claimSubscription(testVatNumber)(request)

          status(res) shouldBe BAD_GATEWAY
        }
      }

      "claim subscription fails to get known facts" should {
        "return BAD_GATEWAY" in {
          val isFromBta = true
          val request = FakeRequest().withBody(ClaimSubscriptionRequest(None, None, isFromBta))

          mockAuthRetrievePrincipalEnrolment()
          mockClaimSubscriptionWithEnrolment(testVatNumber, isFromBta)(Future.successful(Left(KnownFactsFailure)))

          val res = TestClaimSubscriptionController.claimSubscription(testVatNumber)(request)

          status(res) shouldBe BAD_GATEWAY
        }
      }
      "claim subscription finds the enrolment is already allocated" should {
        "return Conflict" in {
          val isFromBta = true
          val request = FakeRequest().withBody(ClaimSubscriptionRequest(None, None, isFromBta))
          mockAuthRetrievePrincipalEnrolment()
          mockClaimSubscriptionWithEnrolment(testVatNumber, isFromBta)(Future.successful(Left(EnrolmentAlreadyAllocated)))

          val res = TestClaimSubscriptionController.claimSubscription(testVatNumber)(request)

          status(res) shouldBe CONFLICT
        }
      }

    }

    "the user has provided known facts" when {
      "the known facts match and claim subscription is successful" should {
        "return NO_CONTENT" in {
          val isFromBta = true
          val request = FakeRequest().withBody(ClaimSubscriptionRequest(Some(testPostCode), Some(testDateOfRegistration), isFromBta))

          mockAuthRetrieveEnrolments()
          mockClaimSubscription(
            testVatNumber,
            Some(testPostCode),
            testDateOfRegistration,
            isFromBta
          )(
            Future.successful(Right(SubscriptionClaimed))
          )

          val res = TestClaimSubscriptionController.claimSubscription(testVatNumber)(request)

          status(res) shouldBe NO_CONTENT
        }
      }

      "the known facts match and claim subscription is successful for an overseas vrn" should {
        "return NO_CONTENT" in {
          val isFromBta = true
          val request = FakeRequest().withBody(ClaimSubscriptionRequest(Some(testPostCode), Some(testDateOfRegistration), isFromBta))

          mockAuthRetrieveEnrolments()
          mockClaimSubscription(
            testVatNumber,
            None,
            testDateOfRegistration,
            isFromBta
          )(
            Future.successful(Right(SubscriptionClaimed))
          )

          val res = TestClaimSubscriptionController.claimSubscription(testVatNumber)(request)

          status(res) shouldBe NO_CONTENT
        }
      }

      "the known facts do not match" should {
        "return FORBIDDEN" in {
          val isFromBta = true
          val request = FakeRequest().withBody(ClaimSubscriptionRequest(Some(testPostCode), Some(testDateOfRegistration), isFromBta))

          mockAuthRetrieveEnrolments()
          mockClaimSubscription(
            testVatNumber,
            Some(testPostCode),
            testDateOfRegistration,
            isFromBta
          )(
            Future.successful(Left(KnownFactsMismatch))
          )

          val res = TestClaimSubscriptionController.claimSubscription(testVatNumber)(request)

          status(res) shouldBe FORBIDDEN
        }
      }


      "the VAT number is invalid" should {
        "return BAD_REQUEST" in {
          val isFromBta = true
          val request = FakeRequest().withBody(ClaimSubscriptionRequest(Some(testPostCode), Some(testDateOfRegistration), isFromBta))

          mockAuthRetrieveEnrolments()
          mockClaimSubscription(
            testVatNumber,
            Some(testPostCode),
            testDateOfRegistration,
            isFromBta
          )(
            Future.successful(Left(InvalidVatNumber))
          )

          val res = TestClaimSubscriptionController.claimSubscription(testVatNumber)(request)

          status(res) shouldBe BAD_REQUEST
        }
      }


      "the VAT number is not found" should {
        "return BAD_REQUEST" in {
          val isFromBta = true
          val request = FakeRequest().withBody(ClaimSubscriptionRequest(Some(testPostCode), Some(testDateOfRegistration), isFromBta))

          mockAuthRetrieveEnrolments()
          mockClaimSubscription(
            testVatNumber,
            Some(testPostCode),
            testDateOfRegistration,
            isFromBta
          )(
            Future.successful(Left(VatNumberNotFound))
          )

          val res = TestClaimSubscriptionController.claimSubscription(testVatNumber)(request)

          status(res) shouldBe BAD_REQUEST
        }
      }
      "claim subscription finds the enrolment is already allocated" should {
        "return Conflict" in {
          val isFromBta = true
          val request = FakeRequest().withBody(ClaimSubscriptionRequest(None, None, isFromBta))
          mockAuthRetrievePrincipalEnrolment()
          mockClaimSubscription(
            testVatNumber,
            Some(testPostCode),
            testDateOfRegistration,
            isFromBta
          )(
            Future.successful(Left(EnrolmentAlreadyAllocated))
          )

          val res = TestClaimSubscriptionController.claimSubscription(testVatNumber)(request)

          status(res) shouldBe CONFLICT
        }
      }
    }
  }
}
