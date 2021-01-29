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

import play.api.test.Helpers._
import org.scalatest.EitherValues
import reactivemongo.api.commands.UpdateWriteResult
import uk.gov.hmrc.http.HeaderCarrier
import org.scalatest.{WordSpec, Matchers}
import uk.gov.hmrc.vatsignup.config.mocks.MockConfig
import uk.gov.hmrc.vatsignup.helpers.TestConstants._
import uk.gov.hmrc.vatsignup.models.Digital
import uk.gov.hmrc.vatsignup.repositories.mocks.MockSubscriptionRequestRepository
import uk.gov.hmrc.vatsignup.services.StoreContactPreferenceService
import uk.gov.hmrc.vatsignup.services.StoreContactPreferenceService._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class StoreContactPreferenceServiceSpec extends WordSpec with Matchers with MockSubscriptionRequestRepository
  with MockConfig with EitherValues {

  object TestStoreContactPreferenceService extends StoreContactPreferenceService(
    mockSubscriptionRequestRepository
  )

  private implicit val headerCarrier: HeaderCarrier = HeaderCarrier()

  "storeContactPreference" when {
    "the contactPreference stores successfully" should {
      "return a StoreContactPreferenceSuccess" in {
        mockUpsertContactPreference(testVatNumber, Digital)(Future.successful(mock[UpdateWriteResult]))

        val res = await(TestStoreContactPreferenceService.storeContactPreference(testVatNumber, Digital))

        res.right.value shouldBe ContactPreferenceStored
      }
    }

    "the vat number has not previously been stored" should {
      "return an ContactPreferenceDatabaseFailureNoVATNumber" in {
        mockUpsertContactPreference(testVatNumber, Digital)(Future.failed(new NoSuchElementException))

        val res = await(TestStoreContactPreferenceService.storeContactPreference(testVatNumber, Digital))

        res.left.value shouldBe ContactPreferenceNoVatFound
      }
    }

    "the contactPreference fails to store" should {
      "return an ContactPreferenceDatabaseFailure" in {
        mockUpsertContactPreference(testVatNumber, Digital)(Future.failed(new Exception))

        val res = await(TestStoreContactPreferenceService.storeContactPreference(testVatNumber, Digital))

        res.left.value shouldBe ContactPreferenceDatabaseFailure
      }
    }

  }

}
