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

package uk.gov.hmrc.vatsignup.controllers

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import org.scalatest.BeforeAndAfterEach
import play.api.http.Status._
import play.api.libs.json.Json
import play.api.mvc.{Request, Result}
import play.api.test.FakeRequest
import uk.gov.hmrc.http.InternalServerException
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.vatsignup.config.featureswitch.{FeatureSwitch, FeatureSwitching, SkipPartnershipKnownFactsMismatch}
import uk.gov.hmrc.vatsignup.connectors.mocks.MockAuthConnector
import uk.gov.hmrc.vatsignup.helpers.TestConstants._
import uk.gov.hmrc.vatsignup.models.StorePartnershipRequest
import uk.gov.hmrc.vatsignup.service.mocks.MockStorePartnershipInformationService
import uk.gov.hmrc.vatsignup.services.StorePartnershipInformationService._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class StorePartnershipInformationControllerSpec extends UnitSpec with MockAuthConnector with MockStorePartnershipInformationService with FeatureSwitching with BeforeAndAfterEach {

  implicit val system: ActorSystem = ActorSystem()
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  object TestStorePartnershipInformationController extends StorePartnershipInformationController(
    mockAuthConnector,
    mockStorePartnershipInformationService
  )

  override def beforeEach() {
    super.beforeEach()
    FeatureSwitch.switches foreach disable
  }

  "storePartnershipInformation" when {

    "the user has a partnership enrolment" when {
      val request: Request[StorePartnershipRequest] = FakeRequest().withBody[StorePartnershipRequest](StorePartnershipRequest(testGeneralPartnership, postCode = None))
      "store partnership information returns StorePartnershipInformationSuccess" should {
        "return NO_CONTENT" in {
          mockAuthRetrievePartnershipEnrolment()
          mockStorePartnershipInformationWithEnrolment(
            testVatNumber,
            testGeneralPartnership,
            testUtr
          )(Future.successful(Right(StorePartnershipInformationSuccess)))

          val result: Result = await(TestStorePartnershipInformationController.storePartnershipInformation(testVatNumber)(request))

          status(result) shouldBe NO_CONTENT
        }
      }
      "store partnership information returns StorePartnershipInformationSuccess when the UTR isn't provided" should {
        val noUtrRequest: Request[StorePartnershipRequest] = FakeRequest().withBody[StorePartnershipRequest](StorePartnershipRequest(testNoUtrGeneralPartnership, postCode = None))
        "return NO_CONTENT" in {
          mockAuthRetrievePartnershipEnrolment()
          mockStorePartnershipInformationWithEnrolment(
            testVatNumber,
            testNoUtrGeneralPartnership,
            testUtr
          )(Future.successful(Right(StorePartnershipInformationSuccess)))

          val result: Result = await(TestStorePartnershipInformationController.storePartnershipInformation(testVatNumber)(noUtrRequest))

          status(result) shouldBe NO_CONTENT
        }
      }
      "store partnership information returns StorePartnershipInformationSuccess for LimitedPartnership" should {
        "return NO_CONTENT" in {
          mockAuthRetrievePartnershipEnrolment()
          mockStorePartnershipInformationWithEnrolment(
            testVatNumber,
            testLimitedPartnership,
            testUtr
          )(Future.successful(Right(StorePartnershipInformationSuccess)))

          val result: Result = await(TestStorePartnershipInformationController.storePartnershipInformation(testVatNumber)(
            FakeRequest().withBody[StorePartnershipRequest](
              StorePartnershipRequest(testLimitedPartnership, postCode = None)
            ))
          )

          status(result) shouldBe NO_CONTENT
        }
      }
      "store partnership information returns PartnershipInformationDatabaseFailureNoVATNumber" should {
        "return PRECONDITION_FAILED" in {
          mockAuthRetrievePartnershipEnrolment()
          mockStorePartnershipInformationWithEnrolment(
            testVatNumber,
            testGeneralPartnership,
            testUtr
          )(Future.successful(Left(PartnershipInformationDatabaseFailureNoVATNumber)))

          val result: Result = await(TestStorePartnershipInformationController.storePartnershipInformation(testVatNumber)(request))

          status(result) shouldBe PRECONDITION_FAILED
        }
      }
      "store partnership information returns PartnershipInformationDatabaseFailure" should {
        "return INTERNAL_SERVER_ERROR" in {
          mockAuthRetrievePartnershipEnrolment()
          mockStorePartnershipInformationWithEnrolment(
            testVatNumber,
            testGeneralPartnership,
            testUtr
          )(Future.successful(Left(PartnershipInformationDatabaseFailure)))

          val result: Result = await(TestStorePartnershipInformationController.storePartnershipInformation(testVatNumber)(request))

          status(result) shouldBe INTERNAL_SERVER_ERROR
        }
      }
    }

    "the user does not have a partnership enrolment" when {
      "post code is provided" when {
        val request: Request[StorePartnershipRequest] = FakeRequest().withBody[StorePartnershipRequest](StorePartnershipRequest(testGeneralPartnership, postCode = Some(testPostCode)))
        "store partnership information returns StorePartnershipInformationSuccess" should {
          "return NO_CONTENT" in {
            mockAuthRetrievePrincipalEnrolment()
            mockStorePartnershipInformation(
              testVatNumber,
              testGeneralPartnership,
              Some(testPostCode)
            )(Future.successful(Right(StorePartnershipInformationSuccess)))

            val result: Result = await(TestStorePartnershipInformationController.storePartnershipInformation(testVatNumber)(request))

            status(result) shouldBe NO_CONTENT
          }
        }
        "store partnership information returns StorePartnershipInformationSuccess for LimitedPartnership" should {
          "return NO_CONTENT" in {
            mockAuthRetrievePrincipalEnrolment()
            mockStorePartnershipInformation(
              testVatNumber,
              testLimitedPartnership,
              Some(testPostCode)
            )(Future.successful(Right(StorePartnershipInformationSuccess)))

            val result: Result = await(TestStorePartnershipInformationController.storePartnershipInformation(testVatNumber)(
              FakeRequest().withBody[StorePartnershipRequest](
                StorePartnershipRequest(testLimitedPartnership, postCode = Some(testPostCode))
              ))
            )

            status(result) shouldBe NO_CONTENT
          }
        }
        "store partnership information returns KnownFactsMismatch" when {
          s"the $SkipPartnershipKnownFactsMismatch feature switch is disabled" should {
            "return FORBIDDEN" in {
              mockAuthRetrievePrincipalEnrolment()
              mockStorePartnershipInformation(
                testVatNumber,
                testGeneralPartnership,
                Some(testPostCode)
              )(Future.successful(Left(KnownFactsMismatch)))

              val result: Result = await(TestStorePartnershipInformationController.storePartnershipInformation(testVatNumber)(request))

              status(result) shouldBe FORBIDDEN
            }
          }
          s"the $SkipPartnershipKnownFactsMismatch feature switch is enabled" should {
            "return NO_CONTENT" in {
              enable(SkipPartnershipKnownFactsMismatch)

              mockAuthRetrievePrincipalEnrolment()
              mockStorePartnershipInformation(
                testVatNumber,
                testGeneralPartnership,
                Some(testPostCode)
              )(Future.successful(Left(KnownFactsMismatch)))

              val result: Result = await(TestStorePartnershipInformationController.storePartnershipInformation(testVatNumber)(request))

              status(result) shouldBe NO_CONTENT
            }
          }
        }
        "store partnership information returns InsufficientData" should {
          s"the $SkipPartnershipKnownFactsMismatch feature switch is disabled" should {
            "throws Internal server exception" in {
              mockAuthRetrievePrincipalEnrolment()
              mockStorePartnershipInformation(
                testVatNumber,
                testGeneralPartnership,
                Some(testPostCode)
              )(Future.successful(Left(InsufficientData)))

              intercept[InternalServerException] {
                await(TestStorePartnershipInformationController.storePartnershipInformation(testVatNumber)(request))
              }
            }
          }
          s"the $SkipPartnershipKnownFactsMismatch feature switch is enabled" should {
            "throws Internal server exception" in {
              enable(SkipPartnershipKnownFactsMismatch)

              mockAuthRetrievePrincipalEnrolment()
              mockStorePartnershipInformation(
                testVatNumber,
                testGeneralPartnership,
                Some(testPostCode)
              )(Future.successful(Left(InsufficientData)))

              val result: Result = await(TestStorePartnershipInformationController.storePartnershipInformation(testVatNumber)(request))

              status(result) shouldBe NO_CONTENT
            }
          }
        }
        "store partnership information returns InvalidSautr" should {
          "return PRECONDITION_FAILED" in {
            mockAuthRetrievePrincipalEnrolment()
            mockStorePartnershipInformation(
              testVatNumber,
              testGeneralPartnership,
              Some(testPostCode)
            )(Future.successful(Left(InvalidSautr)))

            val result: Result = await(TestStorePartnershipInformationController.storePartnershipInformation(testVatNumber)(request))

            status(result) shouldBe PRECONDITION_FAILED
            jsonBodyOf(result) shouldBe Json.obj("statusCode" -> PRECONDITION_FAILED, "message" -> StorePartnershipInformationController.invalidSautrKey)
          }
        }
        "store partnership information returns PartnershipInformationDatabaseFailureNoVATNumber" should {
          "return PRECONDITION_FAILED" in {
            mockAuthRetrievePrincipalEnrolment()
            mockStorePartnershipInformation(
              testVatNumber,
              testGeneralPartnership,
              Some(testPostCode)
            )(Future.successful(Left(PartnershipInformationDatabaseFailureNoVATNumber)))

            val result: Result = await(TestStorePartnershipInformationController.storePartnershipInformation(testVatNumber)(request))

            status(result) shouldBe PRECONDITION_FAILED
          }
        }
        "store partnership information returns PartnershipInformationDatabaseFailure" should {
          "return INTERNAL_SERVER_ERROR" in {
            mockAuthRetrievePrincipalEnrolment()
            mockStorePartnershipInformation(
              testVatNumber,
              testGeneralPartnership,
              Some(testPostCode)
            )(Future.successful(Left(PartnershipInformationDatabaseFailure)))

            val result: Result = await(TestStorePartnershipInformationController.storePartnershipInformation(testVatNumber)(request))

            status(result) shouldBe INTERNAL_SERVER_ERROR
          }
        }
      }
    }

    "post code and sautr is not provided" should {
      "return No Content" in {
        mockAuthRetrievePrincipalEnrolment()
        mockStorePartnershipInformation(
          testVatNumber,
          testNoUtrGeneralPartnership,
          None
        )(Future.successful(Right(StorePartnershipInformationSuccess)))

        val request: Request[StorePartnershipRequest] = FakeRequest().withBody[StorePartnershipRequest](
          StorePartnershipRequest(testNoUtrGeneralPartnership, postCode = None)
        )

        mockAuthRetrievePrincipalEnrolment()
        val result: Result = await(TestStorePartnershipInformationController.storePartnershipInformation(testVatNumber)(request))

        status(result) shouldBe NO_CONTENT
      }
    }
  }

}
