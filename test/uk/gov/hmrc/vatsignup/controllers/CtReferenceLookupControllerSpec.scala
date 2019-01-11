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

import play.api.http.Status._
import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.test.FakeRequest
import uk.gov.hmrc.auth.core.retrieve.EmptyRetrieval
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.vatsignup.connectors.mocks.{MockAuthConnector, MockGetCtReferenceConnector}
import uk.gov.hmrc.vatsignup.helpers.TestConstants._
import uk.gov.hmrc.vatsignup.service.mocks.MockCheckCtReferenceExistsService
import uk.gov.hmrc.vatsignup.services.CtReferenceLookupService.{CheckCtReferenceExistsServiceFailure, CtReferenceIsFound, CtReferenceNotFound}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CtReferenceLookupControllerSpec
  extends UnitSpec with MockAuthConnector with MockCheckCtReferenceExistsService with MockGetCtReferenceConnector {

  object TestCtReferenceLookupController extends CtReferenceLookupController(mockAuthConnector, mockCheckCtReferenceExistsService)

  "checkCtReference" when {

    val request = FakeRequest().withBody(testCompanyNumber)

    "the Ct Reference has been found" should {
      "return OK" in {
        mockAuthorise(retrievals = EmptyRetrieval)(Future.successful(Unit))
        mockCheckCtReferenceExists(testCompanyNumber)(Future.successful(Right(CtReferenceIsFound)))

        val res = await(TestCtReferenceLookupController.checkCtReferenceExists(request))

        status(res) shouldBe OK
      }
    }

    "the Ct Reference has not been found" should {
      "return NOT_FOUND" in {
        mockAuthorise(retrievals = EmptyRetrieval)(Future.successful(Unit))
        mockCheckCtReferenceExists(testCompanyNumber)(Future.successful(Left(CtReferenceNotFound)))

        val res = TestCtReferenceLookupController.checkCtReferenceExists(request)

        status(res) shouldBe NOT_FOUND
      }

      "if calls to DES failed" should {
        "return INTERNAL_SERVER_ERROR" in {
          mockAuthorise(retrievals = EmptyRetrieval)(Future.successful(Unit))
          mockCheckCtReferenceExists(testCompanyNumber)(Future.successful(Left(CheckCtReferenceExistsServiceFailure)))

          val res = TestCtReferenceLookupController.checkCtReferenceExists(request)

          status(res) shouldBe INTERNAL_SERVER_ERROR
        }
      }
    }
  }

}
