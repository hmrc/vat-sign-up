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

import java.util.UUID

import play.api.mvc.Request
import play.api.test.FakeRequest
import reactivemongo.api.commands.UpdateWriteResult
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.vatsignup.helpers.TestConstants._
import uk.gov.hmrc.vatsignup.models.{GeneralPartnership, LimitedPartnership}
import uk.gov.hmrc.vatsignup.repositories.mocks.MockSubscriptionRequestRepository
import uk.gov.hmrc.vatsignup.services.StorePartnershipInformationService
import uk.gov.hmrc.vatsignup.services.StorePartnershipInformationService._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class StorePartnershipInformationServiceSpec extends UnitSpec
  with MockSubscriptionRequestRepository {

  object TestStorePartnershipInformationService extends StorePartnershipInformationService(
    mockSubscriptionRequestRepository
  )

  implicit val hc: HeaderCarrier = HeaderCarrier()
  implicit val request: Request[_] = FakeRequest()


  "storePartnership" when {
    "the UTR exists on the enrolment and matches the provided UTR" when {
      "upsertPartnership is successful" should {
        "return StorePartnershipUtrSuccess" in {
          mockUpsertBusinessEntity(testVatNumber, GeneralPartnership(testUtr))(Future.successful(mock[UpdateWriteResult]))

          val res = TestStorePartnershipInformationService.storePartnershipInformation(testVatNumber, GeneralPartnership(testUtr), testUtr)

          await(res) shouldBe Right(StorePartnershipInformationSuccess)
        }
      }

      "upsertPartnership thrown a NoSuchElementException" should {
        "return PartnershipUtrDatabaseFailureNoVATNumber" in {
          mockUpsertBusinessEntity(testVatNumber, GeneralPartnership(testUtr))(Future.failed(new NoSuchElementException))

          val res = TestStorePartnershipInformationService.storePartnershipInformation(testVatNumber, GeneralPartnership(testUtr), testUtr)

          await(res) shouldBe Left(PartnershipInformationDatabaseFailureNoVATNumber)
        }
      }
      "upsertPartnership failed any other way" should {
        "return PartnershipInformationDatabaseFailure" in {
          mockUpsertBusinessEntity(testVatNumber, GeneralPartnership(testUtr))(Future.failed(new Exception))

          val res = TestStorePartnershipInformationService.storePartnershipInformation(testVatNumber, GeneralPartnership(testUtr), testUtr)

          await(res) shouldBe Left(PartnershipInformationDatabaseFailure)
        }
      }
    }
    "the UTR exists on the enrolment but does not match" should {
      "return EnrolmentMatchFailure" in {
        val nonMatchingUtr = UUID.randomUUID().toString

        val res = TestStorePartnershipInformationService.storePartnershipInformation(testVatNumber, GeneralPartnership(testUtr), nonMatchingUtr)

        await(res) shouldBe Left(EnrolmentMatchFailure)
      }
    }
  }

}
