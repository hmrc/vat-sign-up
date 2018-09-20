/*
 * Copyright 2018 HM Revenue & Customs
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

import java.time.LocalDate
import java.util.UUID

import play.api.http.Status._
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.Result
import play.api.test.FakeRequest
import uk.gov.hmrc.auth.core.Enrolments
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.vatsignup.connectors.mocks.MockAuthConnector
import uk.gov.hmrc.vatsignup.helpers.TestConstants._
import uk.gov.hmrc.vatsignup.models.NinoSource._
import uk.gov.hmrc.vatsignup.models.{IRSA, UserDetailsModel, UserEntered}
import uk.gov.hmrc.vatsignup.service.mocks.MockStoreNinoWithRequestIdService
import uk.gov.hmrc.vatsignup.services.StoreNinoWithRequestIdService._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class StoreNinoWithRequestIdControllerSpec extends UnitSpec with MockAuthConnector with MockStoreNinoWithRequestIdService {

  object TestStoreNinoController
    extends StoreNinoWithRequestIdController(mockAuthConnector, mockStoreNinoWithRequestIdService)

  val testUserDetails = UserDetailsModel(
    firstName = UUID.randomUUID().toString,
    lastName = UUID.randomUUID().toString,
    dateOfBirth = LocalDate.now(),
    nino = testNino
  )

  val enrolments = Enrolments(Set(testPrincipalEnrolment))


  "storeNinoWithRequestId" when {

    "nino source is not supplied" when {
      val request = FakeRequest().withBody(Json.toJson(testUserDetails))

      "the Nino has been stored correctly" should {
        "return NO_CONTENT" in {
          mockAuthRetrievePrincipalEnrolment()
          mockStoreNino(testToken, testUserDetails, enrolments, UserEntered)(Future.successful(Right(StoreNinoSuccess)))

          val res: Result = await(TestStoreNinoController.storeNino(testToken)(request))

          status(res) shouldBe NO_CONTENT
        }
      }

      "if user doesn't match with a record in CID" should {
        "return FORBIDDEN" in {
          mockAuthRetrievePrincipalEnrolment()
          mockStoreNino(testToken, testUserDetails, enrolments, UserEntered)(Future.successful(Left(NoMatchFoundFailure)))

          val res: Result = await(TestStoreNinoController.storeNino(testToken)(request))

          status(res) shouldBe FORBIDDEN
        }
      }

      "if calls to CID failed" should {
        "return INTERNAL_SERVER_ERROR" in {
          mockAuthRetrievePrincipalEnrolment()
          mockStoreNino(testToken, testUserDetails, enrolments, UserEntered)(Future.successful(Left(AuthenticatorFailure)))

          val res: Result = await(TestStoreNinoController.storeNino(testToken)(request))

          status(res) shouldBe INTERNAL_SERVER_ERROR
        }
      }

      "if the requestId doesn't exist in mongo" should {
        "return NOT_FOUND" in {
          mockAuthRetrievePrincipalEnrolment()
          mockStoreNino(testToken, testUserDetails, enrolments, UserEntered)(Future.successful(Left(NinoDatabaseFailureNoRequestId)))

          val res: Result = await(TestStoreNinoController.storeNino(testToken)(request))

          status(res) shouldBe NOT_FOUND
        }
      }

      "the Nino storage has failed" should {
        "return INTERNAL_SERVER_ERROR" in {
          mockAuthRetrievePrincipalEnrolment()
          mockStoreNino(testToken, testUserDetails, enrolments, UserEntered)(Future.successful(Left(NinoDatabaseFailure)))

          val res: Result = await(TestStoreNinoController.storeNino(testToken)(request))

          status(res) shouldBe INTERNAL_SERVER_ERROR
        }
      }
    }

    "nino source is supplied" when {
      val request = FakeRequest().withBody(Json.toJson(testUserDetails).as[JsObject].deepMerge(Json.obj(ninoSourceFrontEndKey -> IRSA)))

      "the Nino has been stored correctly" should {
        "return NO_CONTENT" in {
          mockAuthRetrievePrincipalEnrolment()
          mockStoreNino(testToken, testUserDetails, enrolments, IRSA)(Future.successful(Right(StoreNinoSuccess)))

          val res: Result = await(TestStoreNinoController.storeNino(testToken)(request))

          status(res) shouldBe NO_CONTENT
        }
      }

      "if user doesn't match with a record in CID" should {
        "return FORBIDDEN" in {
          mockAuthRetrievePrincipalEnrolment()
          mockStoreNino(testToken, testUserDetails, enrolments, IRSA)(Future.successful(Left(NoMatchFoundFailure)))

          val res: Result = await(TestStoreNinoController.storeNino(testToken)(request))

          status(res) shouldBe FORBIDDEN
        }

        "if calls to CID failed" should {
          "return INTERNAL_SERVER_ERROR" in {
            mockAuthRetrievePrincipalEnrolment()
            mockStoreNino(testToken, testUserDetails, enrolments, IRSA)(Future.successful(Left(AuthenticatorFailure)))

            val res: Result = await(TestStoreNinoController.storeNino(testToken)(request))

            status(res) shouldBe INTERNAL_SERVER_ERROR
          }
        }

        "if the requestId doesn't exist in mongo" should {
          "return NOT_FOUND" in {
            mockAuthRetrievePrincipalEnrolment()
            mockStoreNino(testToken, testUserDetails, enrolments, IRSA)(Future.successful(Left(NinoDatabaseFailureNoRequestId)))

            val res: Result = await(TestStoreNinoController.storeNino(testToken)(request))

            status(res) shouldBe NOT_FOUND
          }
        }

        "the Nino storage has failed" should {
          "return INTERNAL_SERVER_ERROR" in {
            mockAuthRetrievePrincipalEnrolment()
            mockStoreNino(testToken, testUserDetails, enrolments, IRSA)(Future.successful(Left(NinoDatabaseFailure)))

            val res: Result = await(TestStoreNinoController.storeNino(testToken)(request))

            status(res) shouldBe INTERNAL_SERVER_ERROR
          }
        }
      }
    }
  }

}
