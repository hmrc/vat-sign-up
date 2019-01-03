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

import java.time.LocalDate
import java.util.UUID

import org.scalatest.EitherValues
import play.api.test.FakeRequest
import reactivemongo.api.commands.UpdateWriteResult
import uk.gov.hmrc.auth.core.Enrolments
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.HeaderCarrierConverter
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.vatsignup.connectors.mocks.MockAuthenticatorConnector
import uk.gov.hmrc.vatsignup.helpers.TestConstants
import uk.gov.hmrc.vatsignup.helpers.TestConstants._
import uk.gov.hmrc.vatsignup.models.monitoring.UserMatchingAuditing.UserMatchingAuditModel
import uk.gov.hmrc.vatsignup.models._
import uk.gov.hmrc.vatsignup.repositories.mocks.MockSubscriptionRequestRepository
import uk.gov.hmrc.vatsignup.service.mocks.monitoring.MockAuditService
import uk.gov.hmrc.vatsignup.services.StoreNinoService._
import uk.gov.hmrc.vatsignup.services._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class StoreNinoServiceSpec
  extends UnitSpec with MockAuthenticatorConnector with MockSubscriptionRequestRepository with MockAuditService with EitherValues {


  object TestStoreNinoService extends StoreNinoService(
    mockSubscriptionRequestRepository,
    mockAuthenticatorConnector,
    mockAuditService
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

  "storeNino" when {
    "Nino source is user entered" when {
      "user is matched" should {
        "store the record and return StoreNinoSuccess" in {
          mockMatchUserMatched(testUserDetails)
          mockUpsertBusinessEntity(testVatNumber, SoleTrader(testNino))(Future.successful(mock[UpdateWriteResult]))
          mockUpsertNinoSource(testVatNumber, UserEntered)(Future.successful(mock[UpdateWriteResult]))

          val res = TestStoreNinoService.storeNino(testVatNumber, testUserDetails, agentUser, UserEntered)
          await(res) shouldBe Right(StoreNinoSuccess)

          verifyAudit(UserMatchingAuditModel(testUserDetails, Some(TestConstants.testAgentReferenceNumber), isSuccess = true))
        }
      }

      "user is not matched" should {
        "return NoMatchFoundFailure" in {
          mockMatchUserNotMatched(testUserDetails)

          val res = TestStoreNinoService.storeNino(testVatNumber, testUserDetails, principalUser, UserEntered)
          await(res) shouldBe Left(NoMatchFoundFailure)

          verifyAudit(UserMatchingAuditModel(testUserDetails, None, isSuccess = false))
        }
      }

      "calls to user matching failed" should {
        "return AuthenticatorFailure" in {
          mockMatchUserFailure(testUserDetails)

          val res = TestStoreNinoService.storeNino(testVatNumber, testUserDetails, principalUser, UserEntered)
          await(res) shouldBe Left(AuthenticatorFailure)
        }
      }

      "the VAT number is not in mongo" should {
        "return NinoDatabaseFailureNoVATNumber" in {
          mockMatchUserMatched(testUserDetails)
          mockUpsertBusinessEntity(testVatNumber, SoleTrader(testNino))(Future.failed(new NoSuchElementException))

          val res = TestStoreNinoService.storeNino(testVatNumber, testUserDetails, principalUser, UserEntered)
          await(res) shouldBe Left(NinoDatabaseFailureNoVATNumber)
        }
      }

      "calls to mongo failed" should {
        "return NinoDatabaseFailure" in {
          mockMatchUserMatched(testUserDetails)
          mockUpsertBusinessEntity(testVatNumber, SoleTrader(testNino))(Future.failed(new Exception))

          val res = TestStoreNinoService.storeNino(testVatNumber, testUserDetails, principalUser, UserEntered)
          await(res) shouldBe Left(NinoDatabaseFailure)
        }
      }
    }

    "Nino source is IRSA" when {
      "store the record and return StoreNinoSuccess" in {
        mockUpsertBusinessEntity(testVatNumber, SoleTrader(testNino))(Future.successful(mock[UpdateWriteResult]))
        mockUpsertNinoSource(testVatNumber, IRSA)(Future.successful(mock[UpdateWriteResult]))

        val res = TestStoreNinoService.storeNino(testVatNumber, testUserDetails, principalUser, IRSA)
        await(res) shouldBe Right(StoreNinoSuccess)
      }
      "the VAT number is not in mongo" should {
        "return NinoDatabaseFailureNoVATNumber" in {
          mockUpsertBusinessEntity(testVatNumber, SoleTrader(testNino))(Future.failed(new NoSuchElementException))

          val res = TestStoreNinoService.storeNino(testVatNumber, testUserDetails, principalUser, IRSA)
          await(res) shouldBe Left(NinoDatabaseFailureNoVATNumber)
        }
      }

      "calls to mongo failed" should {
        "return NinoDatabaseFailure" in {
          mockUpsertBusinessEntity(testVatNumber, SoleTrader(testNino))(Future.failed(new Exception))

          val res = TestStoreNinoService.storeNino(testVatNumber, testUserDetails, principalUser, IRSA)
          await(res) shouldBe Left(NinoDatabaseFailure)
        }
      }
    }
    
    "Nino source is auth profile" should {
      "store the record and return StoreNinoSuccess" in {
        mockUpsertBusinessEntity(testVatNumber, SoleTrader(testNino))(Future.successful(mock[UpdateWriteResult]))
        mockUpsertNinoSource(testVatNumber, AuthProfile)(Future.successful(mock[UpdateWriteResult]))

        val res = TestStoreNinoService.storeNino(testVatNumber, testUserDetails, principalUser, AuthProfile)
        await(res) shouldBe Right(StoreNinoSuccess)
      }
      "the VAT number is not in mongo" should {
        "return NinoDatabaseFailureNoVATNumber" in {
          mockUpsertBusinessEntity(testVatNumber, SoleTrader(testNino))(Future.failed(new NoSuchElementException))

          val res = TestStoreNinoService.storeNino(testVatNumber, testUserDetails, principalUser, AuthProfile)
          await(res) shouldBe Left(NinoDatabaseFailureNoVATNumber)
        }
      }

      "calls to mongo failed" should {
        "return NinoDatabaseFailure" in {
          mockUpsertBusinessEntity(testVatNumber, SoleTrader(testNino))(Future.failed(new Exception))

          val res = TestStoreNinoService.storeNino(testVatNumber, testUserDetails, principalUser, AuthProfile)
          await(res) shouldBe Left(NinoDatabaseFailure)
        }
      }
    }
  }

}
