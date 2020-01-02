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
import reactivemongo.api.commands.UpdateWriteResult
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.vatsignup.helpers.TestConstants._
import uk.gov.hmrc.vatsignup.models.{Trust, VatGroup}
import uk.gov.hmrc.vatsignup.repositories.mocks.MockSubscriptionRequestRepository
import uk.gov.hmrc.vatsignup.services.{StoreTrustService, StoreVatGroupService}
import uk.gov.hmrc.vatsignup.services.StoreVatGroupService._
import play.api.test.FakeRequest
import uk.gov.hmrc.vatsignup.services.StoreTrustService.{StoreTrustSuccess, TrustDatabaseFailure, TrustDatabaseFailureNoVATNumber}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class StoreTrustServiceSpec extends UnitSpec
  with MockSubscriptionRequestRepository {

  object TestStoreTrustService extends StoreTrustService(
    mockSubscriptionRequestRepository
  )

  implicit val hc: HeaderCarrier = HeaderCarrier()
  implicit val request: Request[_] = FakeRequest()


  "storeTrust" when {
    "upsertBusinessEntity is successful" should {
      "return StoreTrustSuccess" in {
        mockUpsertBusinessEntity(testVatNumber, Trust)(Future.successful(mock[UpdateWriteResult]))

        val res = TestStoreTrustService.storeTrust(testVatNumber)

        await(res) shouldBe Right(StoreTrustSuccess)

      }
    }
    "upsertBusinessEntity returns StoreTrustDatabaseFailure" should {
      "return StoreTrustFailure" in {
        mockUpsertBusinessEntity(testVatNumber, Trust)(Future.failed(new Exception))

        val res = TestStoreTrustService.storeTrust(testVatNumber)

        await(res) shouldBe Left(TrustDatabaseFailure)

      }
    }
    "upsertBusinessEntity throws NoSuchElementException" should {
      "return TrustDatabaseFailureNoVATNumber" in {
        mockUpsertBusinessEntity(testVatNumber, Trust)(Future.failed(new NoSuchElementException))

        val res = TestStoreTrustService.storeTrust(testVatNumber)

        await(res) shouldBe Left(TrustDatabaseFailureNoVATNumber)

      }
    }
  }

}
