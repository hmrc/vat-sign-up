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

package uk.gov.hmrc.vatsignup.service

import play.api.mvc.Request
import play.api.test.FakeRequest
import reactivemongo.api.commands.UpdateWriteResult
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.vatsignup.helpers.TestConstants._
import uk.gov.hmrc.vatsignup.models.JointVenture
import uk.gov.hmrc.vatsignup.repositories.mocks.MockSubscriptionRequestRepository
import uk.gov.hmrc.vatsignup.services.StoreJointVentureService
import uk.gov.hmrc.vatsignup.services.StoreJointVentureService._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class StoreJointVentureServiceSpec extends UnitSpec
  with MockSubscriptionRequestRepository {

  object TestStoreJointVentureService extends StoreJointVentureService(
    mockSubscriptionRequestRepository
  )

  implicit val hc: HeaderCarrier = HeaderCarrier()
  implicit val request: Request[_] = FakeRequest()


  "storeJointVenture" when {
    "upsertBusinessEntity is successful" should {
      "return StoreJointVentureSuccess" in {
        mockUpsertBusinessEntity(testVatNumber, JointVenture)(Future.successful(mock[UpdateWriteResult]))

        val res = TestStoreJointVentureService.storeJointVenture(testVatNumber)

        await(res) shouldBe Right(StoreJointVentureSuccess)

      }
    }
    "upsertBusinessEntity returns JointVentureDatabaseFailure" should {
      "return StoreJointVentureFailure" in {
        mockUpsertBusinessEntity(testVatNumber, JointVenture)(Future.failed(new Exception))

        val res = TestStoreJointVentureService.storeJointVenture(testVatNumber)

        await(res) shouldBe Left(JointVentureDatabaseFailure)

      }
    }
    "upsertBusinessEntity throws NoSuchElementException" should {
      "return JointVentureDatabaseFailureNoVATNumber" in {
        mockUpsertBusinessEntity(testVatNumber, JointVenture)(Future.failed(new NoSuchElementException))

        val res = TestStoreJointVentureService.storeJointVenture(testVatNumber)

        await(res) shouldBe Left(JointVentureDatabaseFailureNoVATNumber)

      }
    }
  }

}
