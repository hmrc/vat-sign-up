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

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import play.api.http.Status._
import play.api.mvc.Result
import play.api.test.FakeRequest
import uk.gov.hmrc.auth.core.retrieve.EmptyRetrieval
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.vatsignup.connectors.mocks.MockAuthConnector
import uk.gov.hmrc.vatsignup.helpers.TestConstants._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class StoreUnincorporatedAssociationControllerSpec extends UnitSpec with MockAuthConnector {

  implicit val system: ActorSystem = ActorSystem()
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  object TestStoreUnincorporatedAssociationController extends StoreUnincorporatedAssociationController(
    mockAuthConnector
  )

  "storeUnincorporatedAssociation" should {
    "return NOT_IMPLEMENTED" in {
      mockAuthorise(retrievals = EmptyRetrieval)(Future.successful(Unit))

      val result: Result = await(TestStoreUnincorporatedAssociationController.storeUnincorporatedAssociation(testVatNumber)(FakeRequest()))

      status(result) shouldBe NOT_IMPLEMENTED

    }
  }

}
