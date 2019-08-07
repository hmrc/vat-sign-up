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


import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.mvc.Http.Status._
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.vatsignup.connectors.mocks.MockAuthConnector
import uk.gov.hmrc.vatsignup.helpers.TestConstants._
import uk.gov.hmrc.vatsignup.models.IRSA
import uk.gov.hmrc.vatsignup.service.mocks.MockStoreNinoService
import uk.gov.hmrc.vatsignup.services.StoreNinoService.{NinoDatabaseFailure, NinoDatabaseFailureNoVATNumber, StoreNinoSuccess}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class StoreNinoControllerSpec extends UnitSpec with MockAuthConnector with MockStoreNinoService {

  object TestStoreNinoController extends StoreNinoController(mockAuthConnector, mockStoreNinoService)

  val testJson = Json.obj("nino" -> testNino, "ninoSource" -> IRSA)
  val testReq = FakeRequest("PUT", "/").withBody(testJson)

  "storeNinoWithoutMatching" should {
    "return OK" when {
      "Nino has been stored" in {
        mockAuthorise()()
        mockStoreNino(testVatNumber, testNino, IRSA)(Future.successful(Right(StoreNinoSuccess)))

        val res: Result = await(TestStoreNinoController.storeNino(testVatNumber)(testReq))
        status(res) shouldBe NO_CONTENT
      }
    }
    "return NotFound" when {
      "Vat number not found in Mongo" in {
        mockAuthorise()()
        mockStoreNino(testVatNumber, testNino, IRSA)(Future.successful(Left(NinoDatabaseFailureNoVATNumber)))

        val res: Result = await(TestStoreNinoController.storeNino(testVatNumber)(testReq))
        status(res) shouldBe NOT_FOUND
      }
    }
    "return Internal Server Error" when {
      "Mongo fails" in {
        mockAuthorise()()
        mockStoreNino(testVatNumber, testNino, IRSA)(Future.successful(Left(NinoDatabaseFailure)))

        val res: Result = await(TestStoreNinoController.storeNino(testVatNumber)(testReq))
        status(res) shouldBe INTERNAL_SERVER_ERROR
      }
    }
    "return Bad Request" when {
      "Invalid JSON received " in {
        mockAuthorise()()

        val testReqEmpty = FakeRequest("PUT", "/").withBody(Json.obj())
        val res: Result = await(TestStoreNinoController.storeNino(testVatNumber)(testReqEmpty))
        status(res) shouldBe BAD_REQUEST
      }
    }
  }
}
