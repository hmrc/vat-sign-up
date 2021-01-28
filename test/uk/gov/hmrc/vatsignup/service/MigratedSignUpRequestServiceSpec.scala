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
import reactivemongo.api.commands.WriteResult
import uk.gov.hmrc.auth.core.Enrolments
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException, NotFoundException, UnprocessableEntityException}
import org.scalatest.{WordSpec, Matchers}
import uk.gov.hmrc.vatsignup.helpers.TestConstants._
import uk.gov.hmrc.vatsignup.models._
import uk.gov.hmrc.vatsignup.repositories.mocks.MockSubscriptionRequestRepository
import uk.gov.hmrc.vatsignup.services.MigratedSignUpRequestService
import uk.gov.hmrc.vatsignup.services.MigratedSignUpRequestService._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class MigratedSignUpRequestServiceSpec extends WordSpec with Matchers with MockSubscriptionRequestRepository {

  object TestMigratedSignUpRequestService extends MigratedSignUpRequestService(mockSubscriptionRequestRepository)

  implicit val hc = HeaderCarrier()

  val testSubscriptionRequest = SubscriptionRequest(
    vatNumber = testVatNumber,
    ctReference = None,
    businessEntity = Some(SoleTrader(testNino)),
    email = None,
    transactionEmail = None,
    isMigratable = true,
    isDirectDebit = false,
    contactPreference = None
  )

  val testMigratedSignUpRequest = MigratedSignUpRequest(
    vatNumber = testVatNumber,
    businessEntity = SoleTrader(testNino),
    isDelegated = false,
    isMigratable = true
  )

  val testEnrolments = Enrolments(Set(testPrincipalEnrolment))

  "getSignUpRequest" when {
    "the repository contains a SubsciptionRequest for the vat number" when {
      "the business entity exists" when {
        "the user has the authority to submit" should {
          s"return a $MigratedSignUpRequest" in {
            mockFindById(testVatNumber)(Future.successful(Some(testSubscriptionRequest)))

            val res = await(TestMigratedSignUpRequestService.getSignUpRequest(testVatNumber, testEnrolments))

            res shouldBe testMigratedSignUpRequest
          }
        }
      }
      "the business entity does not exist" should {
        s"return $InsufficientData" in {
          mockFindById(testVatNumber)(Future.successful(Some(testSubscriptionRequest.copy(businessEntity = None))))

          intercept[UnprocessableEntityException] {
            await(TestMigratedSignUpRequestService.getSignUpRequest(testVatNumber, testEnrolments))
          }
        }
      }
    }
    "the repository doesn't contain a SubscriptionRequest for the vat number" should {
      s"return $SignUpRequestNotFound" in {
        mockFindById(testVatNumber)(Future.successful(None))

        intercept[NotFoundException] {
          await(TestMigratedSignUpRequestService.getSignUpRequest(testVatNumber, testEnrolments))
        }
      }
    }
    "the database connection fails" should {
      "throw an InternalServerException" in {
        mockFindById(testVatNumber)(Future.failed(new InternalServerException("")))

        intercept[InternalServerException] {
          await(TestMigratedSignUpRequestService.getSignUpRequest(testVatNumber, testEnrolments))
        }
      }
    }
  }

  "deleteSignUpRequest" when {
    "the SignUpRequest is successfully deleted" should {
      "return SignUpRequestDeleted" in {
        mockDeleteRecord(testVatNumber)(Future.successful(mock[WriteResult]))

        val res = await(TestMigratedSignUpRequestService.deleteSignUpRequest(testVatNumber))

        res shouldBe SignUpRequestDeleted
      }
    }
    "the SignUpRequest cannot be deleted" should {
      "throw an internal server exception" in {
        mockDeleteRecord(testVatNumber)(Future.failed(new InternalServerException("")))

        intercept[InternalServerException] {
          await(TestMigratedSignUpRequestService.deleteSignUpRequest(testVatNumber))
        }
      }
    }
  }

}
