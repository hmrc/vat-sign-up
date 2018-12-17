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
import uk.gov.hmrc.vatsignup.models.RegisteredSociety
import uk.gov.hmrc.vatsignup.repositories.mocks.MockSubscriptionRequestRepository
import uk.gov.hmrc.vatsignup.services.StoreRegisteredSocietyService
import uk.gov.hmrc.vatsignup.services.StoreRegisteredSocietyService._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class StoreRegisteredSocietyServiceSpec extends UnitSpec with MockSubscriptionRequestRepository {

  object TestStoreRegisteredSocietyService extends StoreRegisteredSocietyService(mockSubscriptionRequestRepository)

  implicit val hc: HeaderCarrier = HeaderCarrier()
  implicit val request: Request[_] = FakeRequest()

  "storeRegisteredSociety" when {
    "there is a CRN provided" when {
      "the database stores the company number successfully" should {
        "return StoreRegisteredSocietySuccess" in {
          mockUpsertBusinessEntity(testVatNumber, RegisteredSociety(testCompanyNumber))(Future.successful(mock[UpdateWriteResult]))

          val res = TestStoreRegisteredSocietyService.storeRegisteredSociety(testVatNumber, testCompanyNumber)

          await(res) shouldBe Right(StoreRegisteredSocietySuccess)
        }
      }

      "the database returns a NoSuchElementException" should {
        "return RegisteredSocietyDatabaseFailureNoVATNumber" in {
          mockUpsertBusinessEntity(testVatNumber, RegisteredSociety(testCompanyNumber))(Future.failed(new NoSuchElementException))

          val res = TestStoreRegisteredSocietyService.storeRegisteredSociety(testVatNumber, testCompanyNumber)

          await(res) shouldBe Left(RegisteredSocietyDatabaseFailureNoVATNumber)
        }
      }

      "the database returns any other failure" should {
        "return RegisteredSocietyDatabaseFailure" in {
          mockUpsertBusinessEntity(testVatNumber, RegisteredSociety(testCompanyNumber))(Future.failed(new Exception))

          val res = TestStoreRegisteredSocietyService.storeRegisteredSociety(testVatNumber, testCompanyNumber)

          await(res) shouldBe Left(RegisteredSocietyDatabaseFailure)
        }
      }
    }
  }

}
