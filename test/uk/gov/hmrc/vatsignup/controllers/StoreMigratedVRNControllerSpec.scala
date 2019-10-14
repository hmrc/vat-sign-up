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

import java.util.UUID

import play.api.mvc.Result
import play.api.test.FakeRequest
import uk.gov.hmrc.auth.core.Enrolments
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.vatsignup.helpers.TestConstants.{testPrincipalMtdEnrolment, testVatNumber}
import uk.gov.hmrc.vatsignup.models.StoreVatNumberRequest
import play.api.http.Status._
import uk.gov.hmrc.vatsignup.connectors.mocks.MockAuthConnector
import uk.gov.hmrc.vatsignup.service.mocks.MockStoreMigratedVRNService
import uk.gov.hmrc.vatsignup.services.StoreMigratedVRNService._

import scala.concurrent.Future

import scala.concurrent.ExecutionContext.Implicits.global

class StoreMigratedVRNControllerSpec extends UnitSpec with MockAuthConnector with MockStoreMigratedVRNService {

  object TestStoreMigratedVRNController extends StoreMigratedVRNController(
    mockAuthConnector,
    mockStoreMigratedVRNService
  )


  val enrolments = Enrolments(Set(testPrincipalMtdEnrolment))

  val request = FakeRequest() withBody StoreVatNumberRequest(testVatNumber, None)


  "the VAT number has been stored correctly" should {


    "return Ok" in {
      mockAuthRetrieveAgentEnrolment()
      mockStoreVatNumber(testVatNumber, enrolments)(Future.successful(Right(StoreMigratedVRNSuccess)))

      val res: Result = await(TestStoreMigratedVRNController.storeVatNumber()(request))

      status(res) shouldBe OK
    }
  }

  "the VAT number has not been stored correctly" should {


    "return FORBIDDEN if the user has no enrolment" in {

      mockAuthRetrieveAgentEnrolment()
      mockStoreVatNumber(testVatNumber, Enrolments(Set()))(Future.successful(Left(NoVatEnrolment)))

      val res: Result = await(TestStoreMigratedVRNController.storeVatNumber()(request))

      status(res) shouldBe FORBIDDEN
    }

    "return FORBIDDEN if the user VRN does not match the enrolment" in {
      val request = FakeRequest() withBody StoreVatNumberRequest(UUID.randomUUID().toString, None)

      mockAuthRetrieveAgentEnrolment()
      mockStoreVatNumber(testVatNumber, enrolments)(Future.successful(Left(DoesNotMatch)))

      val res: Result = await(TestStoreMigratedVRNController.storeVatNumber()(request))

      status(res) shouldBe FORBIDDEN
    }

    "return INTERNAL SERVER ERROR" in {
      val request = FakeRequest() withBody StoreVatNumberRequest(UUID.randomUUID().toString, None)

      mockAuthRetrieveAgentEnrolment()
      mockStoreVatNumber(testVatNumber, enrolments)(Future.successful(Left(UpsertMigratedVRNFailure)))

      val res: Result = await(TestStoreMigratedVRNController.storeVatNumber()(request))

      status(res) shouldBe INTERNAL_SERVER_ERROR
    }
  }
}
