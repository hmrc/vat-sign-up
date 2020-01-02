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

import java.util.UUID

import org.scalatest.BeforeAndAfterEach
import play.api.http.Status._
import play.api.mvc.Request
import play.api.test.FakeRequest
import reactivemongo.api.commands.UpdateWriteResult
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.vatsignup.config.featureswitch.{FeatureSwitch, FeatureSwitching, SkipPartnershipKnownFactsMismatch}
import uk.gov.hmrc.vatsignup.helpers.TestConstants._
import uk.gov.hmrc.vatsignup.models.GeneralPartnership
import uk.gov.hmrc.vatsignup.repositories.mocks.MockSubscriptionRequestRepository
import uk.gov.hmrc.vatsignup.service.mocks.MockPartnershipKnownFactsService
import uk.gov.hmrc.vatsignup.services.PartnershipKnownFactsService.{NoPostCodesReturned, PartnershipPostCodeMatched, PostCodeDoesNotMatch}
import uk.gov.hmrc.vatsignup.services.StorePartnershipInformationService._
import uk.gov.hmrc.vatsignup.services.{PartnershipKnownFactsService, StorePartnershipInformationService}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class StorePartnershipInformationServiceSpec extends UnitSpec
  with MockSubscriptionRequestRepository with MockPartnershipKnownFactsService with FeatureSwitching with BeforeAndAfterEach {

  object TestStorePartnershipInformationService extends StorePartnershipInformationService(
    mockSubscriptionRequestRepository,
    mockPartnershipKnownFactsService
  )

  implicit val hc: HeaderCarrier = HeaderCarrier()
  implicit val request: Request[_] = FakeRequest()


  override def beforeEach() {
    super.beforeEach()
    FeatureSwitch.switches foreach disable
  }

  "storePartnership" when {
    "the UTR exists on the enrolment and matches the provided UTR" when {
      "upsertPartnership is successful" should {
        "return StorePartnershipUtrSuccess" in {
          mockUpsertBusinessEntity(testVatNumber, GeneralPartnership(Some(testUtr)))(Future.successful(mock[UpdateWriteResult]))

          val res = TestStorePartnershipInformationService.storePartnershipInformationWithEnrolment(testVatNumber, GeneralPartnership(Some(testUtr)), testUtr)

          await(res) shouldBe Right(StorePartnershipInformationSuccess)
        }
      }

      "upsertPartnership thrown a NoSuchElementException" should {
        "return PartnershipUtrDatabaseFailureNoVATNumber" in {
          mockUpsertBusinessEntity(testVatNumber, GeneralPartnership(Some(testUtr)))(Future.failed(new NoSuchElementException))

          val res = TestStorePartnershipInformationService.storePartnershipInformationWithEnrolment(testVatNumber, GeneralPartnership(Some(testUtr)), testUtr)

          await(res) shouldBe Left(PartnershipInformationDatabaseFailureNoVATNumber)
        }
      }
      "upsertPartnership failed any other way" should {
        "return PartnershipInformationDatabaseFailure" in {
          mockUpsertBusinessEntity(testVatNumber, GeneralPartnership(Some(testUtr)))(Future.failed(new Exception))

          val res = TestStorePartnershipInformationService.storePartnershipInformationWithEnrolment(testVatNumber, GeneralPartnership(Some(testUtr)), testUtr)

          await(res) shouldBe Left(PartnershipInformationDatabaseFailure)
        }
      }
    }
    "the UTR exists on the enrolment but does not match" should {
      "return EnrolmentMatchFailure" in {
        val nonMatchingUtr = UUID.randomUUID().toString

        val res = TestStorePartnershipInformationService.storePartnershipInformationWithEnrolment(testVatNumber, GeneralPartnership(Some(testUtr)), nonMatchingUtr)

        await(res) shouldBe Left(EnrolmentMatchFailure)
      }
    }

    "the UTR does not exist on the enrolment but the provided postcode matches" when {
      "upsertPartnership is successful" should {
        "return StorePartnershipUtrSuccess" in {
          mockCheckKnownFactsMatch(testVatNumber, testUtr, testPostCode)(Future.successful(Right(PartnershipPostCodeMatched)))
          mockUpsertBusinessEntity(testVatNumber, GeneralPartnership(Some(testUtr)))(Future.successful(mock[UpdateWriteResult]))

          val res = TestStorePartnershipInformationService.storePartnershipInformation(testVatNumber, GeneralPartnership(Some(testUtr)), Some(testPostCode))

          await(res) shouldBe Right(StorePartnershipInformationSuccess)

        }
      }
    }

    "the UTR does not exist on the enrolment and the provided postcode does not match" should {
      "return Known Facts Mismatch" in {
        mockCheckKnownFactsMatch(testVatNumber, testUtr, testPostCode)(Future.successful(Left(PostCodeDoesNotMatch)))

        val res = TestStorePartnershipInformationService.storePartnershipInformation(testVatNumber, GeneralPartnership(Some(testUtr)), Some(testPostCode))

        await(res) shouldBe Left(KnownFactsMismatch)

      }

    }

    "the UTR does not exist on the enrolment and there is not enough data to check the known facts" when {
      "the SkipPartnershipKnownFactsMismatch feature switch is disabled" should {
        "return Insufficient Data" in {
          mockCheckKnownFactsMatch(testVatNumber, testUtr, testPostCode)(Future.successful(Left(NoPostCodesReturned)))

          val res = TestStorePartnershipInformationService.storePartnershipInformation(testVatNumber, GeneralPartnership(Some(testUtr)), Some(testPostCode))

          await(res) shouldBe Left(InsufficientData)

        }
      }

      "the SkipPartnershipKnownFactsMismatch feature switch is enabled" should {
        "return StorePartnershipInformationSuccess" in {
          enable(SkipPartnershipKnownFactsMismatch)
          mockCheckKnownFactsMatch(testVatNumber, testUtr, testPostCode)(Future.successful(Left(NoPostCodesReturned)))
          mockUpsertBusinessEntity(testVatNumber, GeneralPartnership(None))(Future.successful(mock[UpdateWriteResult]))

          val res = TestStorePartnershipInformationService.storePartnershipInformation(testVatNumber, GeneralPartnership(Some(testUtr)), Some(testPostCode))

          await(res) shouldBe Right(StorePartnershipInformationSuccess)

        }
      }
    }

    "the UTR does not exist on the enrolment and it is an invalid SAUTR" should {
      "return InvalidSautr" in {
        mockCheckKnownFactsMatch(testVatNumber, testUtr, testPostCode)(Future.successful(Left(PartnershipKnownFactsService.InvalidSautr)))

        val res = TestStorePartnershipInformationService.storePartnershipInformation(testVatNumber, GeneralPartnership(Some(testUtr)), Some(testPostCode))

        await(res) shouldBe Left(InvalidSautr)
      }
    }

    "the UTR does not exist on the enrolment and the call to get Known Facts fails" should {
      "return GetPartnershipKnownFactsFailure" in {
        mockCheckKnownFactsMatch(
          testVatNumber,
          testUtr,
          testPostCode
        )(Future.successful(Left(PartnershipKnownFactsService.GetPartnershipKnownFactsFailure(BAD_REQUEST, ""))))

        val res = TestStorePartnershipInformationService.storePartnershipInformation(testVatNumber, GeneralPartnership(Some(testUtr)), Some(testPostCode))

        await(res) shouldBe Left(GetPartnershipKnownFactsFailure)
      }
    }

    "The utr is provided but there is no postcode provided" should {
      "return Insufficient Data" in {
        val res = TestStorePartnershipInformationService.storePartnershipInformation(testVatNumber, GeneralPartnership(Some(testUtr)), None)

        await(res) shouldBe Left(InsufficientData)
      }
    }
  }

}
