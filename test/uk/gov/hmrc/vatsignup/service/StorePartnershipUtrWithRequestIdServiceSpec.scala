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

package uk.gov.hmrc.vatsignup.service

import play.api.mvc.Request
import play.api.test.FakeRequest
import reactivemongo.api.commands.UpdateWriteResult
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.vatsignup.helpers.TestConstants._
import uk.gov.hmrc.vatsignup.models.GeneralPartnership
import uk.gov.hmrc.vatsignup.repositories.mocks.MockUnconfirmedSubscriptionRequestRepository
import uk.gov.hmrc.vatsignup.services.StorePartnershipUtrWithRequestIdService
import uk.gov.hmrc.vatsignup.services.StorePartnershipUtrWithRequestIdService._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class StorePartnershipUtrWithRequestIdServiceSpec extends UnitSpec
  with MockUnconfirmedSubscriptionRequestRepository {

  object TestStorePartnershipUtrWithRequestIdService extends StorePartnershipUtrWithRequestIdService(
    mockUnconfirmedSubscriptionRequestRepository
  )

  implicit val hc: HeaderCarrier = HeaderCarrier()
  implicit val request: Request[_] = FakeRequest()

  "storePartnershipUtr" when {
    "upsertPartnershipUtr is successful" should {
      "return StorePartnershipUtrSuccess" in {
        mockUpsertPartnershipUtr(testToken, GeneralPartnership, testUtr)(Future.successful(mock[UpdateWriteResult]))

        val res = TestStorePartnershipUtrWithRequestIdService.storePartnershipUtr(testToken, GeneralPartnership, testUtr)

        await(res) shouldBe Right(StorePartnershipUtrSuccess)
      }
    }
    "upsertPartnershipUtr thrown a NoSuchElementException" should {
      "return PartnershipUtrDatabaseFailureNoVATNumber" in {
        mockUpsertPartnershipUtr(testToken, GeneralPartnership, testUtr)(Future.failed(new NoSuchElementException))

        val res = TestStorePartnershipUtrWithRequestIdService.storePartnershipUtr(testToken, GeneralPartnership, testUtr)

        await(res) shouldBe Left(PartnershipUtrDatabaseFailureNoVATNumber)
      }
    }
    "upsertPartnershipUtr failed any other way" should {
      "return PartnershipUtrDatabaseFailure" in {
        mockUpsertPartnershipUtr(testToken, GeneralPartnership, testUtr)(Future.failed(new Exception))

        val res = TestStorePartnershipUtrWithRequestIdService.storePartnershipUtr(testToken, GeneralPartnership, testUtr)

        await(res) shouldBe Left(PartnershipUtrDatabaseFailure)
      }
    }
  }

}
