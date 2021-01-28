/*
 * Copyright 2021 HM Revenue & Customs
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

import java.util.UUID

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import play.api.test.Helpers._
import play.api.libs.json.Json
import play.api.test.FakeRequest
import uk.gov.hmrc.auth.core.Enrolments
import org.scalatest.{WordSpec, Matchers}
import uk.gov.hmrc.vatsignup.config.Constants.HttpCodeKey
import uk.gov.hmrc.vatsignup.connectors.mocks.MockAuthConnector
import uk.gov.hmrc.vatsignup.controllers.StoreMigratedVRNController._
import uk.gov.hmrc.vatsignup.helpers.TestConstants._
import uk.gov.hmrc.vatsignup.models.StoreVatNumberRequest
import uk.gov.hmrc.vatsignup.service.mocks.MockStoreMigratedVRNService
import uk.gov.hmrc.vatsignup.services.StoreMigratedVRNService._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class StoreMigratedVRNControllerSpec extends WordSpec with Matchers with MockAuthConnector with MockStoreMigratedVRNService {

  object TestStoreMigratedVRNController extends StoreMigratedVRNController(
    mockAuthConnector,
    mockStoreMigratedVRNService,
    stubControllerComponents()
  )

  val enrolments = Enrolments(Set(testPrincipalMtdEnrolment))
  val agentEnrolments = Enrolments(Set(testAgentEnrolment))

  val request = FakeRequest().withBody(StoreVatNumberRequest(testVatNumber, None))

  val requestWithKF = FakeRequest().withBody(StoreVatNumberRequest(testVatNumber, Some(testTwoKnownFacts)))

  implicit private val system: ActorSystem = ActorSystem()
  implicit private val materializer: ActorMaterializer = ActorMaterializer()

  "Store VAT number" when {
    "no known facts are provided" when {
      "the VAT number has been stored correctly" should {
        "return Ok" in {
          mockAuthRetrieveEnrolments(testPrincipalMtdEnrolment)
          mockStoreVatNumber(testVatNumber, enrolments, None)(Future.successful(Right(StoreMigratedVRNSuccess)))

          val res = TestStoreMigratedVRNController.storeVatNumber()(request)

          status(res) shouldBe OK
        }
      }
      "the VAT number has not been stored correctly" should {
        "return FORBIDDEN if the user has no enrolment" in {
          mockAuthRetrieveEnrolments()
          mockStoreVatNumber(testVatNumber, Enrolments(Set.empty))(Future.successful(Left(NoVatEnrolment)))

          val res = TestStoreMigratedVRNController.storeVatNumber()(request)

          status(res) shouldBe FORBIDDEN
          contentAsJson(res) shouldBe Json.obj(HttpCodeKey -> VatEnrolmentMissingCode)
        }
        "return FORBIDDEN if the user VRN does not match the enrolment" in {
          val vrn = UUID.randomUUID().toString
          val request = FakeRequest() withBody StoreVatNumberRequest(vrn, None)

          mockAuthRetrieveEnrolments(testPrincipalMtdEnrolment)
          mockStoreVatNumber(vrn, enrolments, None)(Future.successful(Left(VatNumberDoesNotMatch)))

          val res = TestStoreMigratedVRNController.storeVatNumber()(request)

          status(res) shouldBe FORBIDDEN
          contentAsJson(res) shouldBe Json.obj(HttpCodeKey -> VatNumberMismatchCode)
        }
        "return INTERNAL SERVER ERROR" in {
          mockAuthRetrieveEnrolments(testPrincipalMtdEnrolment)
          mockStoreVatNumber(testVatNumber, enrolments, None)(Future.successful(Left(UpsertMigratedVRNFailure)))

          val res = TestStoreMigratedVRNController.storeVatNumber()(request)

          status(res) shouldBe INTERNAL_SERVER_ERROR
          contentAsJson(res) shouldBe Json.obj(HttpCodeKey -> StoreVrnFailure)
        }
      }
    }

    "known facts are provided" when {
      "the VAT number has been stored correctly" should {
        "return Ok" in {
          mockAuthRetrieveEnrolments(testPrincipalMtdEnrolment)
          mockStoreVatNumber(testVatNumber, enrolments, Some(testTwoKnownFacts))(Future.successful(Right(StoreMigratedVRNSuccess)))

          val res = TestStoreMigratedVRNController.storeVatNumber()(requestWithKF)

          status(res) shouldBe OK
        }
      }
      "the VAT number has not been stored correctly" should {
        "return FORBIDDEN if the user has no enrolment" in {
          mockAuthRetrieveEnrolments()
          mockStoreVatNumber(testVatNumber, Enrolments(Set.empty), Some(testTwoKnownFacts))(Future.successful(Left(NoVatEnrolment)))

          val res = TestStoreMigratedVRNController.storeVatNumber()(requestWithKF)

          status(res) shouldBe FORBIDDEN
          contentAsJson(res) shouldBe Json.obj(HttpCodeKey -> VatEnrolmentMissingCode)
        }
        "return FORBIDDEN if the user VRN does not match the enrolment" in {
          val vrn = UUID.randomUUID().toString
          val request = FakeRequest() withBody StoreVatNumberRequest(vrn, Some(testTwoKnownFacts))

          mockAuthRetrieveEnrolments(testPrincipalMtdEnrolment)
          mockStoreVatNumber(vrn, enrolments, Some(testTwoKnownFacts))(Future.successful(Left(VatNumberDoesNotMatch)))

          val res = TestStoreMigratedVRNController.storeVatNumber()(request)

          status(res) shouldBe FORBIDDEN
          contentAsJson(res) shouldBe Json.obj(HttpCodeKey -> VatNumberMismatchCode)
        }
        "return INTERNAL SERVER ERROR" in {
          mockAuthRetrieveEnrolments(testPrincipalMtdEnrolment)
          mockStoreVatNumber(testVatNumber, enrolments, Some(testTwoKnownFacts))(Future.successful(Left(UpsertMigratedVRNFailure)))

          val res = TestStoreMigratedVRNController.storeVatNumber()(requestWithKF)

          status(res) shouldBe INTERNAL_SERVER_ERROR
          contentAsJson(res) shouldBe Json.obj(HttpCodeKey -> StoreVrnFailure)
        }
      }
    }
    "agent enrolment is probvided" when {
      "the VAT number has been stored correctly" should {
        "return Ok" in {
          mockAuthRetrieveEnrolments(testAgentEnrolment)
          mockStoreVatNumber(testVatNumber, agentEnrolments)(Future.successful(Right(StoreMigratedVRNSuccess)))

          val res = TestStoreMigratedVRNController.storeVatNumber()(request)

          status(res) shouldBe OK
        }
      }
      "the VAT number has not been stored correctly" should {
        "return FORBIDDEN if the agent does not have matching client relationship" in {
          mockAuthRetrieveEnrolments(testAgentEnrolment)
          mockStoreVatNumber(testVatNumber, agentEnrolments)(Future.successful(Left(AgentClientRelationshipNotFound)))

          val res = TestStoreMigratedVRNController.storeVatNumber()(request)

          status(res) shouldBe FORBIDDEN
          contentAsJson(res) shouldBe Json.obj(HttpCodeKey -> NoRelationshipCode)
        }
        "return INTERNAL SERVER ERROR" in {
          mockAuthRetrieveEnrolments(testAgentEnrolment)
          mockStoreVatNumber(testVatNumber, agentEnrolments)(Future.successful(Left(AgentClientRelationshipFailure)))

          val res = TestStoreMigratedVRNController.storeVatNumber()(request)

          status(res) shouldBe INTERNAL_SERVER_ERROR
          contentAsJson(res) shouldBe Json.obj(HttpCodeKey -> RelationshipCheckFailure)
        }
      }
    }
  }
}
