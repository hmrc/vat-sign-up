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

import java.time.LocalDate
import java.util.UUID
import play.api.test.Helpers._
import org.scalatest.EitherValues
import play.api.test.FakeRequest
import reactivemongo.api.commands.UpdateWriteResult
import uk.gov.hmrc.auth.core.Enrolments
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.HeaderCarrierConverter
import org.scalatest.{WordSpec, Matchers}
import uk.gov.hmrc.vatsignup.helpers.TestConstants._
import uk.gov.hmrc.vatsignup.models._
import uk.gov.hmrc.vatsignup.repositories.mocks.MockSubscriptionRequestRepository
import uk.gov.hmrc.vatsignup.service.mocks.monitoring.MockAuditService
import uk.gov.hmrc.vatsignup.services.StoreNinoService._
import uk.gov.hmrc.vatsignup.services._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class StoreNinoServiceSpec
  extends WordSpec with Matchers with MockSubscriptionRequestRepository with MockAuditService with EitherValues {


  object TestStoreNinoService extends StoreNinoService(
    mockSubscriptionRequestRepository
  )

  val agentUser = Enrolments(Set(testAgentEnrolment))
  val principalUser = Enrolments(Set(testPrincipalEnrolment))

  implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromHeadersAndSession(FakeRequest().headers)
  implicit val request = FakeRequest("POST", "testUrl")

  val testUserDetails = UserDetailsModel(
    firstName = UUID.randomUUID().toString,
    lastName = UUID.randomUUID().toString,
    dateOfBirth = LocalDate.now(),
    nino = testNino
  )

  "storeNino" should {
    "return StoreNinoSuccess" when {
      "nino source is Auth Profile" in {
        mockUpsertBusinessEntity(testVatNumber, SoleTrader(testNino))(Future.successful(mock[UpdateWriteResult]))

        val res = TestStoreNinoService.storeNino(testVatNumber, testNino)

        await(res) shouldBe Right(StoreNinoSuccess)
      }

      "nino source is IRSA" in {
        mockUpsertBusinessEntity(testVatNumber, SoleTrader(testNino))(Future.successful(mock[UpdateWriteResult]))

        val res = TestStoreNinoService.storeNino(testVatNumber, testNino)

        await(res) shouldBe Right(StoreNinoSuccess)
      }

      "nino source is User Entered" in {
        mockUpsertBusinessEntity(testVatNumber, SoleTrader(testNino))(Future.successful(mock[UpdateWriteResult]))

        val res = TestStoreNinoService.storeNino(testVatNumber, testNino)

        await(res) shouldBe Right(StoreNinoSuccess)
      }
    }

    "return NinoDatabaseFailureNoVATNumber" when {
      "vat number is not found in mongo" in {
        mockUpsertBusinessEntity(testVatNumber, SoleTrader(testNino))(Future.failed(new NoSuchElementException))

        val res = TestStoreNinoService.storeNino(testVatNumber, testNino)
        await(res) shouldBe Left(NinoDatabaseFailureNoVATNumber)
      }
    }
    "return NinoDatabaseFailure" when {
      "calls to mongo fail" in {
        mockUpsertBusinessEntity(testVatNumber, SoleTrader(testNino))(Future.failed(new Exception))

        val res = TestStoreNinoService.storeNino(testVatNumber, testNino)
        await(res) shouldBe Left(NinoDatabaseFailure)
      }
    }
  }

}
