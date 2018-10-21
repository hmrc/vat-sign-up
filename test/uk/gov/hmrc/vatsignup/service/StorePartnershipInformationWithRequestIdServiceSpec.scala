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
import uk.gov.hmrc.vatsignup.models.ExplicitEntityType.{GeneralPartnership, LimitedLiabilityPartnership, LimitedPartnership}
import uk.gov.hmrc.vatsignup.models.PartnershipInformation
import uk.gov.hmrc.vatsignup.repositories.mocks.MockUnconfirmedSubscriptionRequestRepository
import uk.gov.hmrc.vatsignup.services.StorePartnershipInformationWithRequestIdService
import uk.gov.hmrc.vatsignup.services.StorePartnershipInformationWithRequestIdService._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class StorePartnershipInformationWithRequestIdServiceSpec extends UnitSpec
  with MockUnconfirmedSubscriptionRequestRepository {

  object TestStorePartnershipInformationWithRequestIdService extends StorePartnershipInformationWithRequestIdService(
    mockUnconfirmedSubscriptionRequestRepository
  )

  implicit val hc: HeaderCarrier = HeaderCarrier()
  implicit val request: Request[_] = FakeRequest()

  val testPartnershipInfo = PartnershipInformation(GeneralPartnership, testUtr, crn = None)
  val testLimitedPartnershipInfo =  PartnershipInformation(LimitedPartnership, testUtr, Some(testCompanyNumber))

  "storePartnershipUtr" when {
    "upsertPartnership is successful" should {
      "return StorePartnershipUtrSuccess" in {
        mockUpsertPartnership(testToken, testUtr, GeneralPartnership)(Future.successful(mock[UpdateWriteResult]))

        val res = TestStorePartnershipInformationWithRequestIdService.storePartnershipInformation(testToken, testPartnershipInfo)

        await(res) shouldBe Right(StorePartnershipInformationSuccess)
      }
    }

    "upsertPartnership thrown a NoSuchElementException" should {
      "return PartnershipUtrDatabaseFailureNoToken" in {
        mockUpsertPartnership(testToken, testUtr, GeneralPartnership)(Future.failed(new NoSuchElementException))

        val res = TestStorePartnershipInformationWithRequestIdService.storePartnershipInformation(testToken, testPartnershipInfo)

        await(res) shouldBe Left(PartnershipInformationDatabaseFailureNoToken)
      }
    }
    "upsertPartnership failed any other way" should {
      "return PartnershipUtrDatabaseFailure" in {
        mockUpsertPartnership(testToken, testUtr, GeneralPartnership)(Future.failed(new Exception))

        val res = TestStorePartnershipInformationWithRequestIdService.storePartnershipInformation(testToken, testPartnershipInfo)

        await(res) shouldBe Left(PartnershipInformationDatabaseFailure)
      }
    }

    "upsertPartnership with Limited Partnership is successful" should {
      "return StorePartnershipUtrSuccess" in {
        mockUpsertPartnershipLimited(testToken, testUtr, testCompanyNumber, LimitedPartnership)(
          Future.successful(mock[UpdateWriteResult])
        )

        val res = TestStorePartnershipInformationWithRequestIdService.storePartnershipInformation(
          requestId = testToken,
          partnershipInformation = PartnershipInformation(LimitedPartnership, testUtr, Some(testCompanyNumber))
        )

        await(res) shouldBe Right(StorePartnershipInformationSuccess)
      }
    }


    "upsertPartnershipLimited thrown a NoSuchElementException" should {
      "return PartnershipUtrDatabaseFailureNoToken" in {
        mockUpsertPartnershipLimited(testToken, testUtr, testCompanyNumber, LimitedLiabilityPartnership)(
          Future.failed(new NoSuchElementException)
        )

        val res = TestStorePartnershipInformationWithRequestIdService.storePartnershipInformation(
          requestId = testToken,
          partnershipInformation = PartnershipInformation(LimitedLiabilityPartnership, testUtr, Some(testCompanyNumber))
        )
        await(res) shouldBe Left(PartnershipInformationDatabaseFailureNoToken)
      }
    }
    "upsertPartnershipLimited failed any other way" should {
      "return PartnershipUtrDatabaseFailure" in {
        mockUpsertPartnershipLimited(testToken, testUtr, testCompanyNumber, LimitedPartnership)(
          Future.failed(new Exception)
        )

        val res = TestStorePartnershipInformationWithRequestIdService.storePartnershipInformation(
          requestId = testToken,
          partnershipInformation = PartnershipInformation(LimitedPartnership, testUtr, Some(testCompanyNumber))
        )
        await(res) shouldBe Left(PartnershipInformationDatabaseFailure)
      }
    }
  }

}
