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
import play.api.http.Status._
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.test.FakeRequest
import uk.gov.hmrc.auth.core.retrieve.EmptyRetrieval
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.vatsignup.config.Constants
import uk.gov.hmrc.vatsignup.connectors.mocks.MockAuthConnector
import uk.gov.hmrc.vatsignup.helpers.TestConstants._
import uk.gov.hmrc.vatsignup.service.mocks.MockStoreRegisteredSocietyService
import uk.gov.hmrc.vatsignup.services.StoreRegisteredSocietyService._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class StoreRegisteredSocietyControllerSpec extends UnitSpec with MockAuthConnector with MockStoreRegisteredSocietyService {

  object TestStoreRegisteredSocietyController
    extends StoreRegisteredSocietyController(mockAuthConnector, mockStoreRegisteredSocietyService)

  implicit val system: ActorSystem = ActorSystem()
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  "storeCompanyNumber" when {
    "a ct reference is provided" should {
      "the provided CT reference matches what is returned from DES" should {
        "return NO_CONTENT" in {
          mockAuthorise(retrievals = EmptyRetrieval)(Future.successful(Unit))
          mockStoreRegisteredSociety(testVatNumber, testCompanyNumber, Some(testCtReference))(
            Future.successful(Right(StoreRegisteredSocietySuccess))
          )

          val request = FakeRequest() withBody(testCompanyNumber, Some(testCtReference))

          val res: Result = await(TestStoreRegisteredSocietyController.storeRegisteredSociety(testVatNumber)(request))

          status(res) shouldBe NO_CONTENT
        }
      }

      "the provided CT reference does not match" should {
        "return BAD_REQUEST with the CtReferenceMismatch code" in {
          mockAuthorise(retrievals = EmptyRetrieval)(Future.successful(Unit))
          mockStoreRegisteredSociety(testVatNumber, testCompanyNumber, Some(testCtReference))(Future.successful(Left(CtReferenceMismatch)))

          val request = FakeRequest() withBody(testCompanyNumber, Some(testCtReference))

          val res: Result = await(TestStoreRegisteredSocietyController.storeRegisteredSociety(testVatNumber)(request))

          status(res) shouldBe BAD_REQUEST
          jsonBodyOf(res) shouldBe Json.obj(
            Constants.HttpCodeKey -> StoreRegisteredSocietyController.CtReferenceMismatchCode
          )
        }
      }

      "if vat doesn't exist" should {
        "return NOT_FOUND" in {
          mockAuthorise(retrievals = EmptyRetrieval)(Future.successful(Unit))
          mockStoreRegisteredSociety(testVatNumber, testCompanyNumber, Some(testCtReference))(
            Future.successful(Left(DatabaseFailureNoVATNumber))
          )

          val request = FakeRequest() withBody(testCompanyNumber, Some(testCtReference))

          val res: Result = await(TestStoreRegisteredSocietyController.storeRegisteredSociety(testVatNumber)(request))

          status(res) shouldBe NOT_FOUND
        }
      }

      "the registered society storage has failed" should {
        "return INTERNAL_SERVER_ERROR" in {
          mockAuthorise(retrievals = EmptyRetrieval)(Future.successful(Unit))
          mockStoreRegisteredSociety(testVatNumber, testCompanyNumber, Some(testCtReference))(
            Future.successful(Left(RegisteredSocietyDatabaseFailure))
          )

          val request = FakeRequest() withBody(testCompanyNumber, Some(testCtReference))

          val res: Result = await(TestStoreRegisteredSocietyController.storeRegisteredSociety(testVatNumber)(request))

          status(res) shouldBe INTERNAL_SERVER_ERROR
        }
      }
    }
    "a ct reference is not provided" should {
      "the registered society is stored correctly" should {
        "return NO_CONTENT" in {
          mockAuthorise(retrievals = EmptyRetrieval)(Future.successful(Unit))
          mockStoreRegisteredSociety(testVatNumber, testCompanyNumber, None)(
            Future.successful(Right(StoreRegisteredSocietySuccess))
          )

          val request = FakeRequest() withBody(testCompanyNumber, None)

          val res: Result = await(TestStoreRegisteredSocietyController.storeRegisteredSociety(testVatNumber)(request))

          status(res) shouldBe NO_CONTENT
        }
      }

      "the registered society storage has failed" should {
        "return INTERNAL_SERVER_ERROR" in {
          mockAuthorise(retrievals = EmptyRetrieval)(Future.successful(Unit))
          mockStoreRegisteredSociety(testVatNumber, testCompanyNumber, None)(
            Future.successful(Left(RegisteredSocietyDatabaseFailure))
          )

          val request = FakeRequest() withBody(testCompanyNumber, None)

          val res: Result = await(TestStoreRegisteredSocietyController.storeRegisteredSociety(testVatNumber)(request))

          status(res) shouldBe INTERNAL_SERVER_ERROR
        }
      }
    }
  }

}
