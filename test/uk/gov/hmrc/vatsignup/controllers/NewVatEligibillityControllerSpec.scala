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
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.vatsignup.connectors.mocks.MockAuthConnector
import uk.gov.hmrc.vatsignup.helpers.TestConstants._
import uk.gov.hmrc.vatsignup.service.mocks.MockVatNumberEligibilityService
import uk.gov.hmrc.vatsignup.services.VatNumberEligibilityService._
import uk.gov.hmrc.vatsignup.controllers.NewVatEligibillityController._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class NewVatEligibillityControllerSpec extends UnitSpec with MockAuthConnector with MockVatNumberEligibilityService {

  object TestNewVatEligibillityController
    extends NewVatEligibillityController(mockAuthConnector, mockVatNumberEligibilityService)

  implicit val system: ActorSystem = ActorSystem()
  implicit val materializer: ActorMaterializer = ActorMaterializer()


  "checkVatNumberEligibillity" when {
    "VatNumberEligibility service returns Already Subscribed" should {
      "return OK with mtdStatus AlreadySubscribed" in {
        mockAuthorise()(Future.successful(Unit))
        mockGetEligibilityStatus(testVatNumber)(Future.successful(AlreadySubscribed))

        val res = await(TestNewVatEligibillityController.checkVatNumberEligibillity(testVatNumber)(FakeRequest()))

        status(res) shouldBe OK
        jsonBodyOf(res) shouldBe Json.obj(MtdStatusKey -> AlreadySubscribedValue)
      }
    }
    "VatNumberElligibity service returns Eligible" should {
      "return OK with mtdStatus Eligible" in {
        mockAuthorise()(Future.successful(Unit))
        mockGetEligibilityStatus(testVatNumber)(Future.successful(Eligible(true, false)))

        val res = await(TestNewVatEligibillityController.checkVatNumberEligibillity(testVatNumber)(FakeRequest()))

        status(res) shouldBe OK
        jsonBodyOf(res) shouldBe Json.obj(MtdStatusKey -> EligibleValue,
          EligiblityDetailsKey -> Json.obj(IsMigratedKey -> true, IsOverseasKey -> false))
      }
    }
    "VatNumberElligibility service returns Ineligible" should {
      "return OK with mtdStatus Ineligible" in {
        mockAuthorise()(Future.successful(Unit))
        mockGetEligibilityStatus(testVatNumber)(Future.successful(Ineligible))

        val res = await(TestNewVatEligibillityController.checkVatNumberEligibillity(testVatNumber)(FakeRequest()))

        status(res) shouldBe OK
        jsonBodyOf(res) shouldBe Json.obj(MtdStatusKey -> IneligibleValue)
      }
    }
    "VatNumberElligibility service returns Inhibited" should {
      "return OK with mtdStatus of Inhibited" in {
        mockAuthorise()(Future.successful(Unit))
        mockGetEligibilityStatus(testVatNumber)(Future.successful(Inhibited(testMigratableDates)))

        val res = await(TestNewVatEligibillityController.checkVatNumberEligibillity(testVatNumber)(FakeRequest()))

        status(res) shouldBe OK
        jsonBodyOf(res) shouldBe Json.obj(MtdStatusKey -> InhibitedValue,  MigratableDatesKey -> Json.toJson(testMigratableDates))
      }
    }
    "VatNumberEligibillity service returns MigrationInProgress" should {
      "return OK with mtdStatus of MigrationInProgress" in {
        mockAuthorise()(Future.successful(Unit))
        mockGetEligibilityStatus(testVatNumber)(Future.successful(MigrationInProgress))

        val res = await(TestNewVatEligibillityController.checkVatNumberEligibillity(testVatNumber)(FakeRequest()))

        status(res) shouldBe OK
        jsonBodyOf(res) shouldBe Json.obj(MtdStatusKey -> MigrationInProgressValue)
      }
    }
    "VatNumberEligibility service returns VatNumberNotFound" should {
      "return NotFound" in {
        mockAuthorise()(Future.successful(Unit))
        mockGetEligibilityStatus(testVatNumber)(Future.successful(VatNumberNotFound))

        val res = await(TestNewVatEligibillityController.checkVatNumberEligibillity(testVatNumber)(FakeRequest()))

        status(res) shouldBe NOT_FOUND
      }
    }
  }
}
