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

import play.api.test.FakeRequest
import uk.gov.hmrc.auth.core.Enrolments
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.vatsignup.helpers.TestConstants._
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

  val requestWithKF = FakeRequest() withBody StoreVatNumberRequest(testVatNumber, Some(testTwoKnownFacts))

  "Store VAT number" when {
    "no known facts are provided" when {
      "the VAT number has been stored correctly" should {
        "return Ok" in {
          mockAuthRetrieveEnrolments(testPrincipalMtdEnrolment)
          mockStoreVatNumber(testVatNumber, enrolments, None)(Future.successful(Right(StoreMigratedVRNSuccess)))

          val res = await(TestStoreMigratedVRNController.storeVatNumber()(request))

          status(res) shouldBe OK
        }
      }
      "the VAT number has not been stored correctly" should {
        "return FORBIDDEN if the user has no enrolment" in {
          mockAuthRetrieveEnrolments()
          mockStoreVatNumber(testVatNumber, Enrolments(Set.empty))(Future.successful(Left(NoVatEnrolment)))

          val res = await(TestStoreMigratedVRNController.storeVatNumber()(request))

          status(res) shouldBe FORBIDDEN
        }
        "return FORBIDDEN if the user VRN does not match the enrolment" in {
          val vrn = UUID.randomUUID().toString
          val request = FakeRequest() withBody StoreVatNumberRequest(vrn, None)

          mockAuthRetrieveEnrolments(testPrincipalMtdEnrolment)
          mockStoreVatNumber(vrn, enrolments, None)(Future.successful(Left(DoesNotMatch)))

          val res = await(TestStoreMigratedVRNController.storeVatNumber()(request))

          status(res) shouldBe FORBIDDEN
        }
        "return INTERNAL SERVER ERROR" in {
          mockAuthRetrieveEnrolments(testPrincipalMtdEnrolment)
          mockStoreVatNumber(testVatNumber, enrolments, None)(Future.successful(Left(UpsertMigratedVRNFailure)))

          val res = await(TestStoreMigratedVRNController.storeVatNumber()(request))

          status(res) shouldBe INTERNAL_SERVER_ERROR
        }
      }
    }

    "known facts are provided" when {
      "the VAT number has been stored correctly" should {
        "return Ok" in {
          mockAuthRetrieveEnrolments(testPrincipalMtdEnrolment)
          mockStoreVatNumber(testVatNumber, enrolments, Some(testTwoKnownFacts))(Future.successful(Right(StoreMigratedVRNSuccess)))

          val res = await(TestStoreMigratedVRNController.storeVatNumber()(requestWithKF))

          status(res) shouldBe OK
        }
      }
      "the VAT number has not been stored correctly" should {
        "return FORBIDDEN if the user has no enrolment" in {
          mockAuthRetrieveEnrolments()
          mockStoreVatNumber(testVatNumber, Enrolments(Set.empty), Some(testTwoKnownFacts))(Future.successful(Left(NoVatEnrolment)))

          val res = await(TestStoreMigratedVRNController.storeVatNumber()(requestWithKF))

          status(res) shouldBe FORBIDDEN
        }
        "return FORBIDDEN if the user VRN does not match the enrolment" in {
          val vrn = UUID.randomUUID().toString
          val request = FakeRequest() withBody StoreVatNumberRequest(vrn, Some(testTwoKnownFacts))

          mockAuthRetrieveEnrolments(testPrincipalMtdEnrolment)
          mockStoreVatNumber(vrn, enrolments, Some(testTwoKnownFacts))(Future.successful(Left(DoesNotMatch)))

          val res = await(TestStoreMigratedVRNController.storeVatNumber()(request))

          status(res) shouldBe FORBIDDEN
        }
        "return INTERNAL SERVER ERROR" in {
          mockAuthRetrieveEnrolments(testPrincipalMtdEnrolment)
          mockStoreVatNumber(testVatNumber, enrolments, Some(testTwoKnownFacts))(Future.successful(Left(UpsertMigratedVRNFailure)))

          val res = await(TestStoreMigratedVRNController.storeVatNumber()(requestWithKF))

          status(res) shouldBe INTERNAL_SERVER_ERROR
        }
      }
    }
  }

}
