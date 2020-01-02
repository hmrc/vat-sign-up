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

package uk.gov.hmrc.vatsignup.service

import play.api.mvc.Request
import play.api.test.FakeRequest
import reactivemongo.api.commands.UpdateWriteResult
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.vatsignup.helpers.TestConstants._
import uk.gov.hmrc.vatsignup.models.VatGroup
import uk.gov.hmrc.vatsignup.repositories.mocks.MockSubscriptionRequestRepository
import uk.gov.hmrc.vatsignup.services.StoreVatGroupService
import uk.gov.hmrc.vatsignup.services.StoreVatGroupService._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class StoreVatGroupServiceSpec extends UnitSpec
  with MockSubscriptionRequestRepository {

  object TestStoreVatGroupService extends StoreVatGroupService(
    mockSubscriptionRequestRepository
  )

  implicit val hc: HeaderCarrier = HeaderCarrier()
  implicit val request: Request[_] = FakeRequest()


  "storeVatGroup" when {
    "upsertBusinessEntity is successful" should {
      "return StoreVatGroupSuccess" in {
        mockUpsertBusinessEntity(testVatNumber, VatGroup)(Future.successful(mock[UpdateWriteResult]))

        val res = TestStoreVatGroupService.storeVatGroup(testVatNumber)

        await(res) shouldBe Right(StoreVatGroupSuccess)

      }
    }
    "upsertBusinessEntity returns VatGroupDatabaseFailure" should {
      "return StoreVatGroupFailure" in {
        mockUpsertBusinessEntity(testVatNumber, VatGroup)(Future.failed(new Exception))

        val res = TestStoreVatGroupService.storeVatGroup(testVatNumber)

        await(res) shouldBe Left(VatGroupDatabaseFailure)

      }
    }
    "upsertBusinessEntity throws NoSuchElementException" should {
      "return VatGroupDatabaseFailureNoVATNumber" in {
        mockUpsertBusinessEntity(testVatNumber, VatGroup)(Future.failed(new NoSuchElementException))

        val res = TestStoreVatGroupService.storeVatGroup(testVatNumber)

        await(res) shouldBe Left(VatGroupDatabaseFailureNoVATNumber)

      }
    }
  }

}
