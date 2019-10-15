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

import play.api.test.FakeRequest
import reactivemongo.api.commands.UpdateWriteResult
import uk.gov.hmrc.auth.core.Enrolments
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.HeaderCarrierConverter
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.vatsignup.helpers.TestConstants._
import uk.gov.hmrc.vatsignup.repositories.mocks.MockSubscriptionRequestRepository
import uk.gov.hmrc.vatsignup.services.StoreMigratedVRNService.{DoesNotMatch, NoVatEnrolment, StoreMigratedVRNSuccess, UpsertMigratedVRNFailure}
import uk.gov.hmrc.vatsignup.services._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class StoreMigratedVRNServiceSpec
  extends UnitSpec with MockSubscriptionRequestRepository {

  object TestStoreMigratedVRNService extends StoreMigratedVRNService(mockSubscriptionRequestRepository)

  implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromHeadersAndSession(FakeRequest().headers)
  implicit val request = FakeRequest("POST", "testUrl")

  val successfulWriteResult: UpdateWriteResult = UpdateWriteResult(ok = true, 0, 0, Seq(), Seq(), None, None, None)
  val failedWriteResult: UpdateWriteResult = UpdateWriteResult(ok = false, 0, 0, Seq(), Seq(), None, None, None)

  val vatEnrolment = Enrolments(Set(testPrincipalMtdEnrolment))
  val freshUser = Enrolments(Set.empty)

  ".storeVatNumber" when {
    "the provided VRN matches the enrolment VRN" should {

      "return a StoreMigratedVRNSuccess when the VRN is stored successfully" in {

        mockUpsertVatNumber(
          testVatNumber, isMigratable = true,
          isDirectDebit = false)(response = Future.successful(successfulWriteResult))

        await(TestStoreMigratedVRNService.storeVatNumber(
          testVatNumber, vatEnrolment)) shouldBe Right(StoreMigratedVRNSuccess)
      }

      "return an UpsertMigratedVRNFailure when the VRN is not stored successfully" in {

        mockUpsertVatNumber(
          testVatNumber, isMigratable = true, isDirectDebit = false)(response = Future.successful(failedWriteResult)
        )

        await(TestStoreMigratedVRNService.storeVatNumber(
          testVatNumber, vatEnrolment)) shouldBe Left(UpsertMigratedVRNFailure)
      }
    }

    "the provided VRN does not match the enrolment VRN" should {
      "return a StoreMigratedVRNFailure" in {

        await(TestStoreMigratedVRNService.storeVatNumber(
          testVatNumber + 1, vatEnrolment)) shouldBe Left(DoesNotMatch)
      }
    }

    "the user does not have a Vat enrolment" should {
      "return a NoVatEnrolment" in {

        await(TestStoreMigratedVRNService.storeVatNumber(
          testVatNumber, freshUser)) shouldBe Left(NoVatEnrolment)
      }
    }

  }
}
