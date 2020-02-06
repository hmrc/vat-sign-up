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
import uk.gov.hmrc.vatsignup.service.mocks.MockStoreOverseasService
import uk.gov.hmrc.vatsignup.services.StoreOverseasService._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class StoreOverseasControllerSpec extends WordSpec
  with Matchers
  with MockAuthConnector
  with MockStoreOverseasService {

  implicit val system: ActorSystem = ActorSystem()
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  object TestStoreOverseasController extends StoreOverseasController(
    mockAuthConnector,
    mockStoreOverseasService,
    stubControllerComponents()
  )


  "storeOverseas" when {
    "is successful" should {
      "return NO_CONTENT" in {
        mockAuthorise(retrievals = EmptyRetrieval)(Future.successful(Unit))
        mockStoreOverseas(testVatNumber)(Future.successful(Right(StoreOverseasSuccess)))

        val result = TestStoreOverseasController.storeOverseas(testVatNumber)(FakeRequest())

        status(result) shouldBe NO_CONTENT
      }
    }
    "fails with OverseasDatabaseFailureNoVATNumber" should {
      "return NOT_FOUND" in {
        mockAuthorise(retrievals = EmptyRetrieval)(Future.successful(Unit))
        mockStoreOverseas(testVatNumber)(Future.successful(Left(OverseasDatabaseFailureNoVATNumber)))

        val result = TestStoreOverseasController.storeOverseas(testVatNumber)(FakeRequest())

        status(result) shouldBe NOT_FOUND
      }
    }
    "fails with OverseasDatabaseFailure" should {
      "return INTERNAL_SERVER_ERROR" in {
        mockAuthorise(retrievals = EmptyRetrieval)(Future.successful(Unit))
        mockStoreOverseas(testVatNumber)(Future.successful(Left(OverseasDatabaseFailure)))

        val result = TestStoreOverseasController.storeOverseas(testVatNumber)(FakeRequest())

        status(result) shouldBe INTERNAL_SERVER_ERROR
      }
    }
  }

}
