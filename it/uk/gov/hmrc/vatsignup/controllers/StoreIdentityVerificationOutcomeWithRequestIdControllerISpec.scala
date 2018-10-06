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
import play.api.libs.json.Json
import uk.gov.hmrc.auth.core.ConfidenceLevel.L200
import uk.gov.hmrc.vatsignup.helpers.IntegrationTestConstants._
import uk.gov.hmrc.vatsignup.helpers._
import uk.gov.hmrc.vatsignup.helpers.servicemocks.AuthStub._
import uk.gov.hmrc.vatsignup.helpers.servicemocks.IdentityVerificationStub.stubGetIdentityVerifiedOutcome
import uk.gov.hmrc.vatsignup.models.UnconfirmedSubscriptionRequest

class StoreIdentityVerificationOutcomeWithRequestIdControllerISpec extends ComponentSpecBase with CustomMatchers with TestUnconfirmedSubmissionRequestRepository{

  override def beforeEach(): Unit = {
    import scala.concurrent.ExecutionContext.Implicits.global
    super.beforeEach()
    unconfirmedSubmissionRequestRepo.drop
    await(unconfirmedSubmissionRequestRepo.insert(UnconfirmedSubscriptionRequest(testToken)))
  }

  "POST /sign-up-request/request-id/:requestId/identity-verification" when {
    "the identity has been successfully verified" should {
      "return NoContent" in {
        stubAuth(OK, successfulAuthResponse())

        stubGetIdentityVerifiedOutcome(testJourneyLink)("Success")

        val res = post(s"/sign-up-request/request-id/$testToken/identity-verification")(Json.obj("journeyLink" -> testJourneyLink))

        res should have(
          httpStatus(NO_CONTENT)
        )
      }
    }

    "the confidence level is 200 or greater" should {
      "return NoContent" in {
        stubAuth(OK, successfulAuthResponse(L200))

        val res = post(s"/sign-up-request/request-id/$testToken/identity-verification")(Json.obj("journeyLink" -> testJourneyLink))

        res should have(
          httpStatus(NO_CONTENT)
        )
      }
    }

    "the identity has been not been successfully verified" should {
      "return Forbidden" in {
        stubAuth(OK, successfulAuthResponse())

        stubGetIdentityVerifiedOutcome(testJourneyLink)("Incomplete")

        val res = post(s"/sign-up-request/request-id/$testToken/identity-verification")(Json.obj("journeyLink" -> testJourneyLink))

        res should have(
          httpStatus(FORBIDDEN)
        )
      }
    }

    "the json is invalid" should {
      "return BAD_REQUEST" in {
        stubAuth(OK, successfulAuthResponse())

        val res = post(s"/sign-up-request/request-id/$testToken/identity-verification")(Json.obj())

        res should have(
          httpStatus(BAD_REQUEST)
        )
      }
    }
  }

}
