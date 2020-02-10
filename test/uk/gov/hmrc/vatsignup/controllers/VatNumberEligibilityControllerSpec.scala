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
import play.api.test.Helpers._
import play.api.libs.json.Json
import play.api.test.FakeRequest
import org.scalatest.{WordSpec, Matchers}
import uk.gov.hmrc.vatsignup.connectors.mocks.MockAuthConnector
import uk.gov.hmrc.vatsignup.helpers.TestConstants._
import uk.gov.hmrc.vatsignup.models.MigratableDates
import uk.gov.hmrc.vatsignup.service.mocks.MockControlListEligibilityService
import uk.gov.hmrc.vatsignup.services.ControlListEligibilityService._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class VatNumberEligibilityControllerSpec extends WordSpec with Matchers with MockAuthConnector with MockControlListEligibilityService {

  object TestVatNumberEligibilityController extends VatNumberEligibilityController(
    mockAuthConnector,
    mockControlListEligibilityService,
    stubControllerComponents()
  )

  implicit val system: ActorSystem = ActorSystem()
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  "checkVatNumberEligibility" when {
    "the service returns EligibilitySuccess" should {
      "return OK with a Json body if the user is an overseas trader" in {
        mockAuthorise()(Future.successful(Unit))
        mockGetEligibilityStatus(testVatNumber)(Future.successful(Right(EligibilitySuccess(
          vatKnownFacts = testTwoKnownFacts.copy(businessPostcode = None),
          isMigratable = true,
          isOverseas = true,
          isDirectDebit = false
        ))))

        val res = TestVatNumberEligibilityController.checkVatNumberEligibility(testVatNumber)(FakeRequest())
        status(res) shouldBe OK
        contentAsJson(res) shouldBe Json.obj("isOverseas" -> true)
      }
      "return OK with a Json body if the overseas flag is set to false" in {
        mockAuthorise()(Future.successful(Unit))
        mockGetEligibilityStatus(testVatNumber)(Future.successful(Right(EligibilitySuccess(
          vatKnownFacts = testTwoKnownFacts,
          isMigratable = true,
          isOverseas = false,
          isDirectDebit = false
        ))))

        val res = TestVatNumberEligibilityController.checkVatNumberEligibility(testVatNumber)(FakeRequest())
        status(res) shouldBe OK
        contentAsJson(res) shouldBe Json.obj("isOverseas" -> false)
      }
    }
    "the service returns IneligibleVatNumber with no migratable dates" should {
      "return BAD_REQUEST with empty json" in {
        mockAuthorise()(Future.successful(Unit))
        mockGetEligibilityStatus(testVatNumber)(Future.successful(Left(IneligibleVatNumber(MigratableDates.empty))))

        val res = TestVatNumberEligibilityController.checkVatNumberEligibility(testVatNumber)(FakeRequest())
        status(res) shouldBe BAD_REQUEST
        contentAsJson(res) shouldBe Json.obj()
      }
    }
    "the service returns IneligibleVatNumber" should {
      "return BAD_REQUEST with the migratable dates" in {
        mockAuthorise()(Future.successful(Unit))
        mockGetEligibilityStatus(testVatNumber)(Future.successful(Left(IneligibleVatNumber(testMigratableDates))))

        val res = TestVatNumberEligibilityController.checkVatNumberEligibility(testVatNumber)(FakeRequest())
        status(res) shouldBe BAD_REQUEST
        contentAsJson(res) shouldBe Json.toJson(testMigratableDates)
      }
    }
    "the service returns VatNumberNotFound" should {
      "return NOT_FOUND" in {
        mockAuthorise()(Future.successful(Unit))
        mockGetEligibilityStatus(testVatNumber)(Future.successful(Left(VatNumberNotFound)))

        val res = TestVatNumberEligibilityController.checkVatNumberEligibility(testVatNumber)(FakeRequest())
        status(res) shouldBe NOT_FOUND
      }
    }
    "the service returns InvalidVatNumber" should {
      "return NOT_FOUND" in {
        mockAuthorise()(Future.successful(Unit))
        mockGetEligibilityStatus(testVatNumber)(Future.successful(Left(InvalidVatNumber)))

        val res = TestVatNumberEligibilityController.checkVatNumberEligibility(testVatNumber)(FakeRequest())
        status(res) shouldBe NOT_FOUND
      }
    }
    "the service returns anything else" should {
      "return BAD_GATEWAY" in {
        mockAuthorise()(Future.successful(Unit))
        mockGetEligibilityStatus(testVatNumber)(Future.successful(Left(KnownFactsAndControlListFailure)))

        val res = TestVatNumberEligibilityController.checkVatNumberEligibility(testVatNumber)(FakeRequest())
        status(res) shouldBe BAD_GATEWAY
      }
    }
  }

}
