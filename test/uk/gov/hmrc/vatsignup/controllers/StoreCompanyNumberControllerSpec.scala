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

package uk.gov.hmrc.vatsignup.controllers

import play.api.http.Status._
import play.api.mvc.Result
import play.api.test.FakeRequest
import uk.gov.hmrc.auth.core.retrieve.EmptyRetrieval
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.vatsignup.connectors.mocks.MockAuthConnector
import uk.gov.hmrc.vatsignup.helpers.TestConstants._
import uk.gov.hmrc.vatsignup.service.mocks.MockStoreCompanyNumberService
import uk.gov.hmrc.vatsignup.services.{CompanyNumberDatabaseFailure, StoreCompanyNumberSuccess, CompanyNumberDatabaseFailureNoVATNumber}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class StoreCompanyNumberControllerSpec extends UnitSpec with MockAuthConnector with MockStoreCompanyNumberService {

  object TestStoreCompanyNumberController
    extends StoreCompanyNumberController(mockAuthConnector, mockStoreCompanyNumberService)

  "storeCompanyNumber" when {
    "the CRN has been stored correctly" should {
      "return NO_CONTENT" in {
        mockAuthorise(retrievals = EmptyRetrieval)(Future.successful(Unit))
        mockStoreCompanyNumber(testVatNumber, testCompanyNumber)(Future.successful(Right(StoreCompanyNumberSuccess)))

        val request = FakeRequest() withBody testCompanyNumber

        val res: Result = await(TestStoreCompanyNumberController.storeCompanyNumber(testVatNumber)(request))

        status(res) shouldBe NO_CONTENT
      }
    }

    "if vat doesn't exist" should {
      "return NOT_FOUND" in {
        mockAuthorise(retrievals = EmptyRetrieval)(Future.successful(Unit))
        mockStoreCompanyNumber(testVatNumber, testCompanyNumber)(Future.successful(Left(CompanyNumberDatabaseFailureNoVATNumber)))

        val request = FakeRequest() withBody testCompanyNumber

        val res: Result = await(TestStoreCompanyNumberController.storeCompanyNumber(testVatNumber)(request))

        status(res) shouldBe NOT_FOUND
      }
    }

    "the CRN storage has failed" should {
      "return INTERNAL_SERVER_ERROR" in {
        mockAuthorise(retrievals = EmptyRetrieval)(Future.successful(Unit))
        mockStoreCompanyNumber(testVatNumber, testCompanyNumber)(Future.successful(Left(CompanyNumberDatabaseFailure)))

        val request = FakeRequest() withBody testCompanyNumber

        val res: Result = await(TestStoreCompanyNumberController.storeCompanyNumber(testVatNumber)(request))

        status(res) shouldBe INTERNAL_SERVER_ERROR
      }
    }
  }

}
