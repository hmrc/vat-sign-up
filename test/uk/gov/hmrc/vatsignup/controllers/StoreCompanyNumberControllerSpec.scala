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

package uk.gov.hmrc.vatsignup.controllers

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import play.api.http.Status._
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.test.FakeRequest
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.vatsignup.config.Constants
import uk.gov.hmrc.vatsignup.connectors.mocks.MockAuthConnector
import uk.gov.hmrc.vatsignup.helpers.TestConstants._
import uk.gov.hmrc.vatsignup.service.mocks.MockStoreCompanyNumberService
import uk.gov.hmrc.vatsignup.services.StoreCompanyNumberService._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class StoreCompanyNumberControllerSpec extends UnitSpec with MockAuthConnector with MockStoreCompanyNumberService {

  object TestStoreCompanyNumberController
    extends StoreCompanyNumberController(mockAuthConnector, mockStoreCompanyNumberService)

  implicit val system: ActorSystem = ActorSystem()
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  "storeCompanyNumber" when {
    "the user is principal" should {
      "the provided CT reference matches what is returned from DES" should {
        "return NO_CONTENT" in {
          mockAuthRetrievePrincipalEnrolment()
          mockStoreCompanyNumber(testVatNumber, testCompanyNumber, testCtReference)(Future.successful(Right(StoreCompanyNumberSuccess)))

          val request = FakeRequest() withBody(testCompanyNumber, Some(testCtReference))

          val res: Result = await(TestStoreCompanyNumberController.storeCompanyNumber(testVatNumber)(request))

          status(res) shouldBe NO_CONTENT
        }
      }

      "the CTUTR is not provided" should {
        "return FORBIDDEN" in {
          mockAuthRetrievePrincipalEnrolment()

          val request = FakeRequest() withBody(testCompanyNumber, None)

          val res: Result = await(TestStoreCompanyNumberController.storeCompanyNumber(testVatNumber)(request))

          status(res) shouldBe FORBIDDEN
        }
      }

      "the provided CT reference does not match" should {
        "return BAD_REQUEST with the CtReferenceMismatch code" in {
          mockAuthRetrievePrincipalEnrolment()
          mockStoreCompanyNumber(testVatNumber, testCompanyNumber, testCtReference)(Future.successful(Left(CtReferenceMismatch)))

          val request = FakeRequest() withBody(testCompanyNumber, Some(testCtReference))

          val res: Result = await(TestStoreCompanyNumberController.storeCompanyNumber(testVatNumber)(request))

          status(res) shouldBe BAD_REQUEST
          jsonBodyOf(res) shouldBe Json.obj(Constants.HttpCodeKey -> StoreCompanyNumberController.CtReferenceMismatchCode)
        }
      }

      "if vat doesn't exist" should {
        "return NOT_FOUND" in {
          mockAuthRetrievePrincipalEnrolment()
          mockStoreCompanyNumber(testVatNumber, testCompanyNumber, testCtReference)(Future.successful(Left(DatabaseFailureNoVATNumber)))

          val request = FakeRequest() withBody(testCompanyNumber, Some(testCtReference))

          val res: Result = await(TestStoreCompanyNumberController.storeCompanyNumber(testVatNumber)(request))

          status(res) shouldBe NOT_FOUND
        }
      }

      "the CRN storage has failed" should {
        "return INTERNAL_SERVER_ERROR" in {
          mockAuthRetrievePrincipalEnrolment()
          mockStoreCompanyNumber(testVatNumber, testCompanyNumber, testCtReference)(Future.successful(Left(CompanyNumberDatabaseFailure)))

          val request = FakeRequest() withBody(testCompanyNumber, Some(testCtReference))

          val res: Result = await(TestStoreCompanyNumberController.storeCompanyNumber(testVatNumber)(request))

          status(res) shouldBe INTERNAL_SERVER_ERROR
        }
      }

      "the CT reference storage has failed" should {
        "return INTERNAL_SERVER_ERROR" in {
          mockAuthRetrievePrincipalEnrolment()
          mockStoreCompanyNumber(testVatNumber, testCompanyNumber, testCtReference)(Future.successful(Left(CtReferenceDatabaseFailure)))

          val request = FakeRequest() withBody(testCompanyNumber, Some(testCtReference))

          val res: Result = await(TestStoreCompanyNumberController.storeCompanyNumber(testVatNumber)(request))

          status(res) shouldBe INTERNAL_SERVER_ERROR
        }
      }
    }
    "the user is agent" should {
      "the company number is stored correctly" should {
        "return NO_CONTENT" in {
          mockAuthRetrieveAgentEnrolment()
          mockStoreCompanyNumber(testVatNumber, testCompanyNumber)(Future.successful(Right(StoreCompanyNumberSuccess)))

          val request = FakeRequest() withBody(testCompanyNumber, None)

          val res: Result = await(TestStoreCompanyNumberController.storeCompanyNumber(testVatNumber)(request))

          status(res) shouldBe NO_CONTENT
        }
      }

      "the call to match CT reference fails" should {
        "return BAD_GATEWAY" in {
          mockAuthRetrieveAgentEnrolment()
          mockStoreCompanyNumber(testVatNumber, testCompanyNumber)(Future.successful(Left(MatchCtReferenceFailure)))

          val request = FakeRequest() withBody(testCompanyNumber, None)

          val res: Result = await(TestStoreCompanyNumberController.storeCompanyNumber(testVatNumber)(request))

          status(res) shouldBe BAD_GATEWAY
        }
      }
    }
  }

}
