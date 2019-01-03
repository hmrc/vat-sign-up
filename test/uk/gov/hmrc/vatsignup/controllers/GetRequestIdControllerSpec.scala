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
import play.api.http.Status._
import play.api.libs.json.Json
import play.api.test.FakeRequest
import uk.gov.hmrc.auth.core.retrieve.Retrievals
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.vatsignup.connectors.mocks.MockAuthConnector
import uk.gov.hmrc.vatsignup.helpers.TestConstants._
import uk.gov.hmrc.vatsignup.models.PartialSignUpRequest
import uk.gov.hmrc.vatsignup.service.mocks.MockRequestIdService
import uk.gov.hmrc.vatsignup.services.RequestIdService.RequestIdDatabaseFailure

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class GetRequestIdControllerSpec extends UnitSpec
  with MockAuthConnector
  with MockRequestIdService {

  object TestGetRequestIdController extends GetRequestIdController(mockAuthConnector, mockRequestIdService)

  implicit val system: ActorSystem = ActorSystem()
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  def mockAuth(): Unit =
    mockAuthorise(retrievals = Retrievals.internalId)(Future.successful(
      Some(testCredentialId)
    ))

  "getRequestId" when {
    "request id is successfully returned from mongo" should {
      "Return 200" in {
        mockAuth()
        mockGetRequestIdByCredential(testCredentialId)(Future.successful(Right(PartialSignUpRequest(testToken))))

        val res = TestGetRequestIdController.getRequestId(FakeRequest())

        status(res) shouldBe OK
        await(bodyOf(res)) shouldBe Json.toJson(PartialSignUpRequest(testToken)).toString()
      }
    }

    "database error is returned from mongo" should {
      "Return 500" in {
        mockAuth()
        mockGetRequestIdByCredential(testCredentialId)(Future.successful(Left(RequestIdDatabaseFailure)))

        val res = TestGetRequestIdController.getRequestId(FakeRequest())

        status(res) shouldBe INTERNAL_SERVER_ERROR
      }
    }
  }

}
