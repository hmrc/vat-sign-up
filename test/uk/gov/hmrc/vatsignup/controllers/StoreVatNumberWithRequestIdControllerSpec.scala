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
import play.api.http.Status.{PRECONDITION_FAILED, UNPROCESSABLE_ENTITY, _}
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.test.FakeRequest
import uk.gov.hmrc.auth.core.Enrolments
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.vatsignup.config.Constants.HttpCodeKey
import uk.gov.hmrc.vatsignup.connectors.mocks.MockAuthConnector
import uk.gov.hmrc.vatsignup.helpers.TestConstants._
import uk.gov.hmrc.vatsignup.httpparsers.AgentClientRelationshipsHttpParser
import uk.gov.hmrc.vatsignup.models.StoreVatNumberWithRequestIdRequest
import uk.gov.hmrc.vatsignup.service.mocks.MockStoreVatNumberWithRequestIdService
import uk.gov.hmrc.vatsignup.services.StoreVatNumberWithRequestIdService._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class StoreVatNumberWithRequestIdControllerSpec extends UnitSpec
  with MockAuthConnector with MockStoreVatNumberWithRequestIdService {

  object TestStoreVatNumberWithRequestIdController
    extends StoreVatNumberWithRequestIdController(mockAuthConnector, mockStoreVatNumberWithRequestIdService)

  implicit val system: ActorSystem = ActorSystem()
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  val enrolments = Enrolments(Set(testAgentEnrolment))

  "storeVatNumber" when {

    //n.b. since the service is mocked, we don't care what the input is only how we handle what the service returns
    val request = FakeRequest() withBody StoreVatNumberWithRequestIdRequest(testToken, testVatNumber, None, None)

    "the VAT number has been stored correctly" should {
      "return CREATED" in {
        mockAuthRetrieveAgentEnrolment()
        mockStoreVatNumber(testToken, testVatNumber, enrolments)(Future.successful(Right(StoreVatNumberSuccess)))

        val res: Result = await(TestStoreVatNumberWithRequestIdController.storeVatNumber(testToken)(request))

        status(res) shouldBe CREATED
      }
    }

    "the VAT number storage has failed" should {
      "return INTERNAL_SERVER_ERROR" in {
        mockAuthRetrieveAgentEnrolment()
        mockStoreVatNumber(testToken, testVatNumber, enrolments)(Future.successful(Left(VatNumberDatabaseFailure)))

        val res: Result = await(TestStoreVatNumberWithRequestIdController.storeVatNumber(testToken)(request))

        status(res) shouldBe INTERNAL_SERVER_ERROR
      }
    }

    "the vat number does not match enrolment" should {
      "return FORBIDDEN" in {
        mockAuthRetrieveAgentEnrolment()
        mockStoreVatNumber(testToken, testVatNumber, enrolments)(Future.successful(Left(DoesNotMatchEnrolment)))

        val res: Result = await(TestStoreVatNumberWithRequestIdController.storeVatNumber(testToken)(request))

        status(res) shouldBe FORBIDDEN
        jsonBodyOf(res) shouldBe Json.obj(HttpCodeKey -> "DoesNotMatchEnrolment")
      }
    }

    "insufficient enrolment" should {
      "return FORBIDDEN" in {
        mockAuthRetrieveAgentEnrolment()
        mockStoreVatNumber(testToken, testVatNumber, enrolments)(Future.successful(Left(InsufficientEnrolments)))

        val res: Result = await(TestStoreVatNumberWithRequestIdController.storeVatNumber(testToken)(request))

        status(res) shouldBe FORBIDDEN
        jsonBodyOf(res) shouldBe Json.obj(HttpCodeKey -> "InsufficientEnrolments")
      }
    }

    "no agent client relationship exists for a delegated call" should {
      "return FORBIDDEN" in {
        mockAuthRetrieveAgentEnrolment()
        mockStoreVatNumber(testToken, testVatNumber, enrolments)(Future.successful(Left(RelationshipNotFound)))

        val res: Result = await(TestStoreVatNumberWithRequestIdController.storeVatNumber(testToken)(request))

        status(res) shouldBe FORBIDDEN
        jsonBodyOf(res) shouldBe Json.obj(HttpCodeKey -> AgentClientRelationshipsHttpParser.NoRelationshipCode)
      }
    }

    "Known facts mismatch" should {
      "return FORBIDDEN" in {
        mockAuthRetrieveAgentEnrolment()
        mockStoreVatNumber(testToken, testVatNumber, enrolments)(Future.successful(Left(KnownFactsMismatch)))

        val res: Result = await(TestStoreVatNumberWithRequestIdController.storeVatNumber(testToken)(request))

        status(res) shouldBe FORBIDDEN
        jsonBodyOf(res) shouldBe Json.obj(HttpCodeKey -> "KNOWN_FACTS_MISMATCH")
      }
    }

    "the user is ineligible" should {
      "return UNPROCESSABLE_ENTITY" in {
        mockAuthRetrieveAgentEnrolment()
        mockStoreVatNumber(testToken, testVatNumber, enrolments)(Future.successful(Left(Ineligible)))

        val res: Result = await(TestStoreVatNumberWithRequestIdController.storeVatNumber(testToken)(request))

        status(res) shouldBe UNPROCESSABLE_ENTITY
      }
    }

    "the vat number is not found" should {
      "return PRECONDITION_FAILED" in {
        mockAuthRetrieveAgentEnrolment()
        mockStoreVatNumber(testToken, testVatNumber, enrolments)(Future.successful(Left(VatNotFound)))

        val res: Result = await(TestStoreVatNumberWithRequestIdController.storeVatNumber(testToken)(request))

        status(res) shouldBe PRECONDITION_FAILED
      }
    }

    "the vat number is invalid" should {
      "return PRECONDITION_FAILED" in {
        mockAuthRetrieveAgentEnrolment()
        mockStoreVatNumber(testToken, testVatNumber, enrolments)(Future.successful(Left(VatInvalid)))

        val res: Result = await(TestStoreVatNumberWithRequestIdController.storeVatNumber(testToken)(request))

        status(res) shouldBe PRECONDITION_FAILED
      }
    }

    "the call to agent services failed" should {
      "return FORBIDDEN" in {
        mockAuthRetrieveAgentEnrolment()
        mockStoreVatNumber(testToken, testVatNumber, enrolments)(
          Future.successful(Left(AgentServicesConnectionFailure))
        )

        val res: Result = await(TestStoreVatNumberWithRequestIdController.storeVatNumber(testToken)(request))

        status(res) shouldBe BAD_GATEWAY
      }
    }
  }

}
