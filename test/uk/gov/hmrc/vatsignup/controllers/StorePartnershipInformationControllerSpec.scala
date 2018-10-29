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

import java.util.UUID

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import play.api.http.Status._
import play.api.mvc.{Request, Result}
import play.api.test.FakeRequest
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.vatsignup.connectors.mocks.MockAuthConnector
import uk.gov.hmrc.vatsignup.helpers.TestConstants._
import uk.gov.hmrc.vatsignup.models.{GeneralPartnership, PartnershipBusinessEntity}
import uk.gov.hmrc.vatsignup.service.mocks.MockStorePartnershipInformationService
import uk.gov.hmrc.vatsignup.services.StorePartnershipInformationService._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class StorePartnershipInformationControllerSpec extends UnitSpec with MockAuthConnector with MockStorePartnershipInformationService {

  implicit val system: ActorSystem = ActorSystem()
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  object TestStorePartnershipInformationController extends StorePartnershipInformationController(
    mockAuthConnector,
    mockStorePartnershipInformationService
  )

  val request: Request[PartnershipBusinessEntity] = FakeRequest().withBody[PartnershipBusinessEntity](testGeneralPartnership)

  "storePartnershipInformation" when {
    "store partnership information returns StorePartnershipInformationSuccess" should {
      "return NO_CONTENT" in {
        mockAuthRetrievePartnershipEnrolment()
        mockStorePartnershipInformation(
          testVatNumber,
          testGeneralPartnership,
          testUtr
        )(Future.successful(Right(StorePartnershipInformationSuccess)))

        val result: Result = await(TestStorePartnershipInformationController.storePartnershipInformation(testVatNumber)(request))

        status(result) shouldBe NO_CONTENT
      }
    }
    "store partnership information returns StorePartnershipInformationSuccess for LimitedPartnership" should {
      "return NO_CONTENT" in {
        mockAuthRetrievePartnershipEnrolment()
        mockStorePartnershipInformation(
          testVatNumber,
          testLimitedPartnership,
          testUtr
        )(Future.successful(Right(StorePartnershipInformationSuccess)))

        val result: Result = await(TestStorePartnershipInformationController.storePartnershipInformation(testVatNumber)(
          FakeRequest().withBody[PartnershipBusinessEntity](
            testLimitedPartnership
          ))
        )

        status(result) shouldBe NO_CONTENT
      }
    }
    "store partnership information returns PartnershipInformationDatabaseFailureNoVATNumber" should {
      "return NOT_FOUND" in {
        mockAuthRetrievePartnershipEnrolment()
        mockStorePartnershipInformation(
          testVatNumber,
          testGeneralPartnership,
          testUtr
        )(Future.successful(Left(PartnershipInformationDatabaseFailureNoVATNumber)))

        val result: Result = await(TestStorePartnershipInformationController.storePartnershipInformation(testVatNumber)(request))

        status(result) shouldBe NOT_FOUND
      }
    }
    "store partnership information returns PartnershipInformationDatabaseFailure" should {
      "return INTERNAL_SERVER_ERROR" in {
        mockAuthRetrievePartnershipEnrolment()
        mockStorePartnershipInformation(
          testVatNumber,
          testGeneralPartnership,
          testUtr
        )(Future.successful(Left(PartnershipInformationDatabaseFailure)))

        val result: Result = await(TestStorePartnershipInformationController.storePartnershipInformation(testVatNumber)(request))

        status(result) shouldBe INTERNAL_SERVER_ERROR
      }
    }

  }

}
