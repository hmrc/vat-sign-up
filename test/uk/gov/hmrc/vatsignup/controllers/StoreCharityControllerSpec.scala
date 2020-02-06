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
import uk.gov.hmrc.auth.core.retrieve.EmptyRetrieval
import uk.gov.hmrc.vatsignup.connectors.mocks.MockAuthConnector
import uk.gov.hmrc.vatsignup.helpers.TestConstants._
import uk.gov.hmrc.vatsignup.service.mocks.MockStoreCharityService
import uk.gov.hmrc.vatsignup.services.StoreCharityService.{CharityDatabaseFailure, CharityDatabaseFailureNoVATNumber, StoreCharitySuccess}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class StoreCharityControllerSpec extends WordSpec
  with Matchers
  with MockAuthConnector
  with MockStoreCharityService {

  implicit val system: ActorSystem = ActorSystem()
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  object TestStoreCharityController extends StoreCharityController(
    mockAuthConnector,
    mockStoreCharityService,
    stubControllerComponents()
  )

  "storeCharity" when {
    "is successful" should {
      "return NO_CONTENT" in {
        mockAuthorise(retrievals = EmptyRetrieval)(Future.successful(Unit))
        mockStoreCharity(testVatNumber)(Future.successful(Right(StoreCharitySuccess)))

        val result = TestStoreCharityController.storeCharity(testVatNumber)(FakeRequest())

        status(result) shouldBe NO_CONTENT
      }
    }
    "fails with CharityDatabaseFailureNoVATNumber" should {
      "return NOT_FOUND" in {
        mockAuthorise(retrievals = EmptyRetrieval)(Future.successful(Unit))
        mockStoreCharity(testVatNumber)(Future.successful(Left(CharityDatabaseFailureNoVATNumber)))

        val result = TestStoreCharityController.storeCharity(testVatNumber)(FakeRequest())

        status(result) shouldBe NOT_FOUND
      }
    }
    "fails with CharityDatabaseFailure" should {
      "return INTERNAL_SERVER_ERROR" in {
        mockAuthorise(retrievals = EmptyRetrieval)(Future.successful(Unit))
        mockStoreCharity(testVatNumber)(Future.successful(Left(CharityDatabaseFailure)))

        val result = TestStoreCharityController.storeCharity(testVatNumber)(FakeRequest())

        status(result) shouldBe INTERNAL_SERVER_ERROR
      }
    }
  }

}
