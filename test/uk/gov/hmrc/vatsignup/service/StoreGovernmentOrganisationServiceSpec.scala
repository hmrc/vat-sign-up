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
import uk.gov.hmrc.vatsignup.models.GovernmentOrganisation
import uk.gov.hmrc.vatsignup.repositories.mocks.MockSubscriptionRequestRepository
import uk.gov.hmrc.vatsignup.services.StoreGovernmentOrganisationService
import uk.gov.hmrc.vatsignup.services.StoreGovernmentOrganisationService._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class StoreGovernmentOrganisationServiceSpec extends UnitSpec
  with MockSubscriptionRequestRepository {

  object TestStoreGovernmentOrganisationService extends StoreGovernmentOrganisationService(
    mockSubscriptionRequestRepository
  )

  implicit val hc: HeaderCarrier = HeaderCarrier()
  implicit val request: Request[_] = FakeRequest()


  "storeGovernmentOrganisation" when {
    "upsertBusinessEntity is successful" should {
      "return StoreGovernmentOrganisationSuccess" in {
        mockUpsertBusinessEntity(testVatNumber, GovernmentOrganisation)(Future.successful(mock[UpdateWriteResult]))

        val res = TestStoreGovernmentOrganisationService.storeGovernmentOrganisation(testVatNumber)

        await(res) shouldBe Right(StoreGovernmentOrganisationSuccess)

      }
    }
    "upsertBusinessEntity returns GovernmentOrganisationDatabaseFailure" should {
      "return StoreGovernmentOrganisationFailure" in {
        mockUpsertBusinessEntity(testVatNumber, GovernmentOrganisation)(Future.failed(new Exception))

        val res = TestStoreGovernmentOrganisationService.storeGovernmentOrganisation(testVatNumber)

        await(res) shouldBe Left(GovernmentOrganisationDatabaseFailure)

      }
    }
    "upsertBusinessEntity throws NoSuchElementException" should {
      "return GovernmentOrganisationDatabaseFailureNoVATNumber" in {
        mockUpsertBusinessEntity(testVatNumber, GovernmentOrganisation)(Future.failed(new NoSuchElementException))

        val res = TestStoreGovernmentOrganisationService.storeGovernmentOrganisation(testVatNumber)

        await(res) shouldBe Left(GovernmentOrganisationDatabaseFailureNoVATNumber)

      }
    }
  }

}
