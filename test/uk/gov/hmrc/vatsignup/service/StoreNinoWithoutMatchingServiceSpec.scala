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

import reactivemongo.api.commands.UpdateWriteResult
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.vatsignup.helpers.TestConstants.{testNino, testVatNumber}
import uk.gov.hmrc.vatsignup.models.{AuthProfile, IRSA, SoleTrader, UserEntered}
import uk.gov.hmrc.vatsignup.repositories.mocks.MockSubscriptionRequestRepository
import uk.gov.hmrc.vatsignup.services.StoreNinoWithoutMatchingService
import uk.gov.hmrc.vatsignup.services.StoreNinoWithoutMatchingService._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class StoreNinoWithoutMatchingServiceSpec extends UnitSpec with MockSubscriptionRequestRepository {

  object TestStoreNinoWithoutMatchingService extends StoreNinoWithoutMatchingService(mockSubscriptionRequestRepository)

  "storeNino" should {
    "return StoreNinoSuccess" when {
      "the nino source is user entered" in {
        mockUpsertBusinessEntity(testVatNumber, SoleTrader(testNino))(Future.successful(mock[UpdateWriteResult]))
        mockUpsertNinoSource(testVatNumber, UserEntered)(Future.successful(mock[UpdateWriteResult]))

        val res = await(TestStoreNinoWithoutMatchingService.storeNino(testVatNumber, testNino, UserEntered))

        res shouldBe Right(StoreNinoWithoutMatchingSuccess)
      }

      "the nino source is IRSA enrolment" in {
        mockUpsertBusinessEntity(testVatNumber, SoleTrader(testNino))(Future.successful(mock[UpdateWriteResult]))
        mockUpsertNinoSource(testVatNumber, IRSA)(Future.successful(mock[UpdateWriteResult]))

        val res = await(TestStoreNinoWithoutMatchingService.storeNino(testVatNumber, testNino, IRSA))

        res shouldBe Right(StoreNinoWithoutMatchingSuccess)
      }

      "the nino source is from Auth" in {
        mockUpsertBusinessEntity(testVatNumber, SoleTrader(testNino))(Future.successful(mock[UpdateWriteResult]))
        mockUpsertNinoSource(testVatNumber, AuthProfile)(Future.successful(mock[UpdateWriteResult]))

        val res = await(TestStoreNinoWithoutMatchingService.storeNino(testVatNumber, testNino, AuthProfile))

        res shouldBe Right(StoreNinoWithoutMatchingSuccess)
      }
    }
    "return DatabaseFailureVatNumberNotFound" when {
      "the vat number is not found in the database" in {
        mockUpsertBusinessEntity(testVatNumber, SoleTrader(testNino))(Future.failed(new NoSuchElementException))

        val res = await(TestStoreNinoWithoutMatchingService.storeNino(testVatNumber, testNino, UserEntered))

        res shouldBe Left(DatabaseFailureVatNumberNotFound)
      }
    }
    "return DatabaseFailure" when {
      "the calls to mongo fail" in {
        mockUpsertBusinessEntity(testVatNumber, SoleTrader(testNino))(Future.failed(new Exception))

        val res = await(TestStoreNinoWithoutMatchingService.storeNino(testVatNumber, testNino, UserEntered))

        res shouldBe Left(DatabaseFailure)
      }
    }
  }

}