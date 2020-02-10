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
import play.api.test.Helpers._
import play.api.test.FakeRequest
import reactivemongo.api.commands.UpdateWriteResult
import uk.gov.hmrc.http.HeaderCarrier
import org.scalatest.{WordSpec, Matchers}
import uk.gov.hmrc.vatsignup.helpers.TestConstants._
import uk.gov.hmrc.vatsignup.models.UnincorporatedAssociation
import uk.gov.hmrc.vatsignup.repositories.mocks.MockSubscriptionRequestRepository
import uk.gov.hmrc.vatsignup.services.StoreUnincorporatedAssociationService
import uk.gov.hmrc.vatsignup.services.StoreUnincorporatedAssociationService._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class StoreUnincorporatedAssociationServiceSpec extends WordSpec with Matchers
  with MockSubscriptionRequestRepository {

  object TestStoreUnincorporatedAssociationService extends StoreUnincorporatedAssociationService(
    mockSubscriptionRequestRepository
  )

  implicit val hc: HeaderCarrier = HeaderCarrier()
  implicit val request: Request[_] = FakeRequest()


  "storeUnincorporatedAssociation" when {
    "upsertBusinessEntity is successful" should {
      "return StoreUnincorporatedAssociationSuccess" in {
        mockUpsertBusinessEntity(testVatNumber, UnincorporatedAssociation)(Future.successful(mock[UpdateWriteResult]))

        val res = TestStoreUnincorporatedAssociationService.storeUnincorporatedAssociation(testVatNumber)

        await(res) shouldBe Right(StoreUnincorporatedAssociationSuccess)

      }
    }
    "upsertBusinessEntity returns UnincorporatedAssociationDatabaseFailure" should {
      "return StoreUnincorporatedAssociationFailure" in {
        mockUpsertBusinessEntity(testVatNumber, UnincorporatedAssociation)(Future.failed(new Exception))

        val res = TestStoreUnincorporatedAssociationService.storeUnincorporatedAssociation(testVatNumber)

        await(res) shouldBe Left(UnincorporatedAssociationDatabaseFailure)

      }
    }
    "upsertBusinessEntity throws NoSuchElementException" should {
      "return UnincorporatedAssociationDatabaseFailureNoVATNumber" in {
        mockUpsertBusinessEntity(testVatNumber, UnincorporatedAssociation)(Future.failed(new NoSuchElementException))

        val res = TestStoreUnincorporatedAssociationService.storeUnincorporatedAssociation(testVatNumber)

        await(res) shouldBe Left(UnincorporatedAssociationDatabaseFailureNoVATNumber)

      }
    }
  }

}
