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

import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.vatsignup.helpers.TestConstants._
import uk.gov.hmrc.vatsignup.models.{PartialSignUpRequest, UnconfirmedSubscriptionRequest}
import uk.gov.hmrc.vatsignup.repositories.mocks.MockUnconfirmedSubscriptionRequestRepository
import uk.gov.hmrc.vatsignup.services.RequestIdService
import uk.gov.hmrc.vatsignup.services.RequestIdService.RequestIdDatabaseFailure

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class RequestIdServiceSpec extends UnitSpec
  with MockUnconfirmedSubscriptionRequestRepository {

  object TestRequestIdService extends RequestIdService(
    mockUnconfirmedSubscriptionRequestRepository
  )

  "getRequestIdByCredential" when {
    "repo returned request id" should {
      "return RequestIdSuccess(requestId)" in {
        mockGetRequestIdByCredential(testCredentialId)(Future.successful(UnconfirmedSubscriptionRequest(testToken)))

        val res = TestRequestIdService.getRequestIdByCredential(testCredentialId)

        await(res) shouldBe Right(PartialSignUpRequest(testToken))
      }
    }

    "repo errored" should {
      "return RequestIdFailure" in {
        mockGetRequestIdByCredential(testCredentialId)(Future.failed(new Exception("")))

        val res = TestRequestIdService.getRequestIdByCredential(testCredentialId)

        await(res) shouldBe Left(RequestIdDatabaseFailure)
      }
    }
  }

}
