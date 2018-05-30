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
import uk.gov.hmrc.vatsignup.helpers.IntegrationTestConstants._
import uk.gov.hmrc.vatsignup.helpers.servicemocks.AuthStub._
import uk.gov.hmrc.vatsignup.helpers.servicemocks.EmailVerificationStub._
import uk.gov.hmrc.vatsignup.helpers.servicemocks.RegistrationStub._
import uk.gov.hmrc.vatsignup.helpers.servicemocks.SignUpStub._
import uk.gov.hmrc.vatsignup.helpers.servicemocks.TaxEnrolmentsStub._
import uk.gov.hmrc.vatsignup.helpers.{ComponentSpecBase, CustomMatchers, TestEmailRequestRepository, TestSubmissionRequestRepository}
import uk.gov.hmrc.vatsignup.models.{SubscriptionRequest, UserEntered}

import scala.concurrent.ExecutionContext.Implicits.global

class SignUpSubmissionControllerISpec extends ComponentSpecBase with CustomMatchers
 with TestSubmissionRequestRepository with TestEmailRequestRepository {
  "/subscription-request/vat-number/:vatNumber/submit" when {
    "the user is a delegate and" when {
      "all downstream services behave as expected" should {
        "return NO_CONTENT for individual sign up" in {
          val testSubscriptionRequest = SubscriptionRequest(
            vatNumber = testVatNumber,
            nino = Some(testNino),
            ninoSource = Some(UserEntered),
            email = Some(testEmail)
          )

          stubAuth(OK, successfulAuthResponse(agentEnrolment))
          stubGetEmailVerified(testEmail)
          stubRegisterIndividual(testVatNumber, testNino)(testSafeId)
          stubSignUp(testSafeId, testVatNumber, testEmail, emailVerified = true)(OK)
          stubRegisterEnrolment(testVatNumber, testSafeId)(NO_CONTENT)

          await(submissionRequestRepo.insert(testSubscriptionRequest))
          val res = await(post(s"/subscription-request/vat-number/$testVatNumber/submit")(Json.obj()))

          res should have(
            httpStatus(NO_CONTENT),
            emptyBody
          )
        }

        "return NO_CONTENT for company sign up" in {
          val testSubscriptionRequest = SubscriptionRequest(
            vatNumber = testVatNumber,
            companyNumber = Some(testCompanyNumber),
            email = Some(testEmail)
          )

          stubAuth(OK, successfulAuthResponse(agentEnrolment))
          stubGetEmailVerified(testEmail)
          stubRegisterCompany(testVatNumber, testCompanyNumber)(testSafeId)
          stubSignUp(testSafeId, testVatNumber, testEmail, emailVerified = true)(OK)
          stubRegisterEnrolment(testVatNumber, testSafeId)(NO_CONTENT)

          await(submissionRequestRepo.insert(testSubscriptionRequest))
          val res = await(post(s"/subscription-request/vat-number/$testVatNumber/submit")(Json.obj()))

          res should have(
            httpStatus(NO_CONTENT),
            emptyBody
          )
        }
      }
    }
    "the user is principal and" when {
      "all downstream services behave as expected" should {
        "return NO_CONTENT for individual sign up" in {
          val testSubscriptionRequest = SubscriptionRequest(
            vatNumber = testVatNumber,
            nino = Some(testNino),
            ninoSource = Some(UserEntered),
            email = Some(testEmail),
            identityVerified = true
          )

          stubAuth(OK, successfulAuthResponse(vatDecEnrolment))
          stubGetEmailVerified(testEmail)
          stubRegisterIndividual(testVatNumber, testNino)(testSafeId)
          stubSignUp(testSafeId, testVatNumber, testEmail, emailVerified = true)(OK)
          stubRegisterEnrolment(testVatNumber, testSafeId)(NO_CONTENT)

          await(submissionRequestRepo.insert(testSubscriptionRequest))
          val res = await(post(s"/subscription-request/vat-number/$testVatNumber/submit")(Json.obj()))

          res should have(
            httpStatus(NO_CONTENT),
            emptyBody
          )
        }

        "return NO_CONTENT for company sign up" in {
          val testSubscriptionRequest = SubscriptionRequest(
            vatNumber = testVatNumber,
            companyNumber = Some(testCompanyNumber),
            email = Some(testEmail),
            identityVerified = true
          )

          stubAuth(OK, successfulAuthResponse(vatDecEnrolment))
          stubGetEmailVerified(testEmail)
          stubRegisterCompany(testVatNumber, testCompanyNumber)(testSafeId)
          stubSignUp(testSafeId, testVatNumber, testEmail, emailVerified = true)(OK)
          stubRegisterEnrolment(testVatNumber, testSafeId)(NO_CONTENT)

          await(submissionRequestRepo.insert(testSubscriptionRequest))
          val res = await(post(s"/subscription-request/vat-number/$testVatNumber/submit")(Json.obj()))

          res should have(
            httpStatus(NO_CONTENT),
            emptyBody
          )
        }
      }
    }
  }
}
