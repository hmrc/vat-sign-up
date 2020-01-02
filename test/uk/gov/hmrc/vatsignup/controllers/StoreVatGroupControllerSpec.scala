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
import play.api.mvc.Result
import play.api.test.FakeRequest
import uk.gov.hmrc.auth.core.retrieve.EmptyRetrieval
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.vatsignup.connectors.mocks.MockAuthConnector
import uk.gov.hmrc.vatsignup.helpers.TestConstants._
import uk.gov.hmrc.vatsignup.service.mocks.MockStoreVatGroupService
import uk.gov.hmrc.vatsignup.services.StoreVatGroupService.{StoreVatGroupSuccess, VatGroupDatabaseFailure, VatGroupDatabaseFailureNoVATNumber}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class StoreVatGroupControllerSpec extends UnitSpec with MockAuthConnector with MockStoreVatGroupService {

  implicit val system: ActorSystem = ActorSystem()
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  object TestStoreVatGroupController extends StoreVatGroupController(
    mockAuthConnector,
    mockStoreVatGroupService
  )


  "storeVatGroup" when {
    "is successful" should {
      "return NO_CONTENT" in {
        mockAuthorise(retrievals = EmptyRetrieval)(Future.successful(Unit))
        mockStoreVatGroup(testVatNumber)(Future.successful(Right(StoreVatGroupSuccess)))

        val result: Result = await(TestStoreVatGroupController.storeVatGroup(testVatNumber)(FakeRequest()))

        status(result) shouldBe NO_CONTENT

      }
    }
    "fails with VatGroupDatabaseFailureNoVATNumber" should {
      "return NOT_FOUND" in {
        mockAuthorise(retrievals = EmptyRetrieval)(Future.successful(Unit))
        mockStoreVatGroup(testVatNumber)(Future.successful(Left(VatGroupDatabaseFailureNoVATNumber)))

        val result: Result = await(TestStoreVatGroupController.storeVatGroup(testVatNumber)(FakeRequest()))

        status(result) shouldBe NOT_FOUND

      }
    }
    "fails with VatGroupDatabaseFailure" should {
      "return INTERNAL_SERVER_ERROR" in {
        mockAuthorise(retrievals = EmptyRetrieval)(Future.successful(Unit))
        mockStoreVatGroup(testVatNumber)(Future.successful(Left(VatGroupDatabaseFailure)))

        val result: Result = await(TestStoreVatGroupController.storeVatGroup(testVatNumber)(FakeRequest()))

        status(result) shouldBe INTERNAL_SERVER_ERROR

      }
    }
  }

}
