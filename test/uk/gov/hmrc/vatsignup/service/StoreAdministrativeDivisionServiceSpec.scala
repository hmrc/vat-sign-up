/*
 * Copyright 2021 HM Revenue & Customs
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
import play.api.test.Helpers._
import play.api.test.FakeRequest
import reactivemongo.api.commands.UpdateWriteResult
import uk.gov.hmrc.http.HeaderCarrier
import org.scalatest.{WordSpec, Matchers}
import uk.gov.hmrc.vatsignup.helpers.TestConstants._
import uk.gov.hmrc.vatsignup.models.AdministrativeDivision
import uk.gov.hmrc.vatsignup.repositories.mocks.MockSubscriptionRequestRepository
import uk.gov.hmrc.vatsignup.services.StoreAdministrativeDivisionService
import uk.gov.hmrc.vatsignup.services.StoreAdministrativeDivisionService._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class StoreAdministrativeDivisionServiceSpec extends WordSpec with Matchers
  with MockSubscriptionRequestRepository {

  object TestStoreAdministrativeDivisionService extends StoreAdministrativeDivisionService(
    mockSubscriptionRequestRepository
  )

  implicit val hc: HeaderCarrier = HeaderCarrier()
  implicit val request: Request[_] = FakeRequest()


  "storeAdministrativeDivision" when {
    "upsertBusinessEntity is successful" should {
      "return StoreAdministrativeDivisionSuccess" in {
        mockUpsertBusinessEntity(testVatNumber, AdministrativeDivision)(Future.successful(mock[UpdateWriteResult]))

        val res = TestStoreAdministrativeDivisionService.storeAdministrativeDivision(testVatNumber)

        await(res) shouldBe Right(StoreAdministrativeDivisionSuccess)

      }
    }
    "upsertBusinessEntity returns AdministrativeDivisionDatabaseFailure" should {
      "return StoreAdministrativeDivisionFailure" in {
        mockUpsertBusinessEntity(testVatNumber, AdministrativeDivision)(Future.failed(new Exception))

        val res = TestStoreAdministrativeDivisionService.storeAdministrativeDivision(testVatNumber)

        await(res) shouldBe Left(AdministrativeDivisionDatabaseFailure)

      }
    }
    "upsertBusinessEntity throws NoSuchElementException" should {
      "return AdministrativeDivisionDatabaseFailureNoVATNumber" in {
        mockUpsertBusinessEntity(testVatNumber, AdministrativeDivision)(Future.failed(new NoSuchElementException))

        val res = TestStoreAdministrativeDivisionService.storeAdministrativeDivision(testVatNumber)

        await(res) shouldBe Left(AdministrativeDivisionDatabaseFailureNoVATNumber)

      }
    }
  }

}
