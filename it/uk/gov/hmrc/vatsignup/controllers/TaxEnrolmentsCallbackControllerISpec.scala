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

import org.scalatest.BeforeAndAfterEach
import play.api.http.Status._
import play.api.libs.json.Json
import uk.gov.hmrc.vatsignup.config.featureswitch.AutoClaimEnrolment
import uk.gov.hmrc.vatsignup.helpers.IntegrationTestConstants._
import uk.gov.hmrc.vatsignup.helpers.servicemocks.KnownFactsStub.stubFailureVatNumberNotFound
import uk.gov.hmrc.vatsignup.helpers.servicemocks.{EmailStub, EnrolmentStoreProxyStub, KnownFactsStub, TaxEnrolmentsStub}
import uk.gov.hmrc.vatsignup.helpers.{ComponentSpecBase, CustomMatchers, TestEmailRequestRepository}
import uk.gov.hmrc.vatsignup.models.EmailRequest
import uk.gov.hmrc.vatsignup.services.SubscriptionNotificationService._

import scala.concurrent.ExecutionContext.Implicits.global

class TaxEnrolmentsCallbackControllerISpec extends ComponentSpecBase with BeforeAndAfterEach with CustomMatchers with TestEmailRequestRepository {

  "/subscription-request/vat-number/callback" when {
    "the user is not delegated" when {
      "callback is successful" should {
        "return NO_CONTENT with the status" in {
          await(emailRequestRepo.insert(EmailRequest(testVatNumber, testEmail, isDelegated = false)))

          EmailStub.stubSendEmail(testEmail, principalSuccessEmailTemplate)(ACCEPTED)

          val res = post(s"/subscription-request/vat-number/$testVatNumber/callback")(Json.obj("state" -> "SUCCEEDED"))
          res should have(
            httpStatus(NO_CONTENT)
          )
        }

        "callback is unsuccessful" when {
          "EnrolmentStoreProxy returns OK (200)" should {
            "Send Email is successful" should {
              "return NO_CONTENT (204) with the status" in {
                await(emailRequestRepo.insert(EmailRequest(testVatNumber, testEmail, isDelegated = false)))

                EnrolmentStoreProxyStub.stubGetAllocatedMtdVatEnrolmentStatus(testVatNumber)(OK)

                EmailStub.stubSendEmail(testEmail, principalSuccessEmailTemplate)(ACCEPTED)

                val res = post(s"/subscription-request/vat-number/$testVatNumber/callback")(Json.obj("state" -> "ERROR"))

                res should have(
                  httpStatus(NO_CONTENT)
                )
              }
            }
          }
        }

        "callback is unsuccessful" when {
          "EnrolmentStoreProxy returns NO_CONTENT (204)" should {
            "return no Email" in {
              await(emailRequestRepo.insert(EmailRequest(testVatNumber, testEmail, isDelegated = false)))

              EnrolmentStoreProxyStub.stubGetAllocatedMtdVatEnrolmentStatus(testVatNumber)(NO_CONTENT)

              val res = post(s"/subscription-request/vat-number/$testVatNumber/callback")(Json.obj("state" -> "ERROR"))
              res should have(
                httpStatus(NO_CONTENT)
              )
            }
          }
        }

        "callback is unsuccessful with EnrolmentError" should {
          "EnrolmentStoreProxy returns an unsuccessful status" should {
            "return no Email" in {
              await(emailRequestRepo.insert(EmailRequest(testVatNumber, testEmail, isDelegated = false)))

              EnrolmentStoreProxyStub.stubGetAllocatedMtdVatEnrolmentStatus(testVatNumber)(SERVICE_UNAVAILABLE)

              val res = post(s"/subscription-request/vat-number/$testVatNumber/callback")(
                Json.obj("state" -> "EnrolmentError")
              )
              res should have(
                httpStatus(NO_CONTENT)
              )
            }
          }
        }
      }
    }
    "the user is delegated" when {
      "the feature switch is enabled" when {
        "callback is successful" when {
          "and auto claim enrolment is successful" should {
            "return SUCCEEDED with the status" in {
              enable(AutoClaimEnrolment)

              await(emailRequestRepo.insert(EmailRequest(testVatNumber, testEmail, isDelegated = true)))

              EnrolmentStoreProxyStub.stubGetAllocatedMtdVatEnrolmentStatus(testVatNumber)(OK)
              EnrolmentStoreProxyStub.stubGetUserIds(testVatNumber)(OK)
              KnownFactsStub.stubSuccessGetKnownFacts(testVatNumber)
              TaxEnrolmentsStub.stubUpsertEnrolment(testVatNumber, testPostCode, testDateOfRegistration)(OK)
              TaxEnrolmentsStub.stubAllocateEnrolmentWithoutKnownFacts(testVatNumber, testGroupId, testCredentialId)(OK)
              TaxEnrolmentsStub.stubAssignEnrolment(testVatNumber, testCredentialId)(OK)

              EmailStub.stubSendEmailDelegated(testEmail, agentSuccessEmailTemplate, testVatNumber)(ACCEPTED)

              val res = post(s"/subscription-request/vat-number/$testVatNumber/callback")(Json.obj("state" -> "SUCCEEDED"))
              res should have(
                httpStatus(NO_CONTENT)
              )
            }
          }

          "and auto claim enrolment fails" should {
            "return SUCCEEDED with the status" in {
              enable(AutoClaimEnrolment)

              await(emailRequestRepo.insert(EmailRequest(testVatNumber, testEmail, isDelegated = true)))

              EnrolmentStoreProxyStub.stubGetAllocatedMtdVatEnrolmentStatus(testVatNumber)(OK)
              EnrolmentStoreProxyStub.stubGetUserIds(testVatNumber)(OK)
              KnownFactsStub.stubSuccessGetKnownFacts(testVatNumber)
              TaxEnrolmentsStub.stubUpsertEnrolment(testVatNumber, testPostCode, testDateOfRegistration)(OK)
              TaxEnrolmentsStub.stubAllocateEnrolmentWithoutKnownFacts(testVatNumber, testGroupId, testCredentialId)(OK)
              TaxEnrolmentsStub.stubAssignEnrolment(testVatNumber, testCredentialId)(BAD_GATEWAY)

              EmailStub.stubSendEmailDelegated(testEmail, agentSuccessEmailTemplate, testVatNumber)(ACCEPTED)

              val res = post(s"/subscription-request/vat-number/$testVatNumber/callback")(Json.obj("state" -> "SUCCEEDED"))
              res should have(
                httpStatus(NO_CONTENT)
              )
            }
          }
        }
        "callback is unsuccessful" when {
          "Send Email is successful" should {
            "return ERROR with the status" in {
              await(emailRequestRepo.insert(EmailRequest(testVatNumber, testEmail, isDelegated = true)))

              EmailStub.stubSendEmailDelegated(testEmail, agentSuccessEmailTemplate, testVatNumber)(ACCEPTED)

              val res = post(s"/subscription-request/vat-number/$testVatNumber/callback")(Json.obj("state" -> "ERROR"))

              res should have
              httpStatus(BAD_GATEWAY)
            }
          }
        }
      }

      "the feature switch is disabled" when {
        "callback is successful" should {
          "return SUCCEEDED with the status" in {
            disable(AutoClaimEnrolment)

            await(emailRequestRepo.insert(EmailRequest(testVatNumber, testEmail, isDelegated = true)))

            EmailStub.stubSendEmailDelegated(testEmail, agentSuccessEmailTemplate, testVatNumber)(ACCEPTED)

            val res = post(s"/subscription-request/vat-number/$testVatNumber/callback")(Json.obj("state" -> "SUCCEEDED"))
            res should have(
              httpStatus(BAD_GATEWAY)
            )
          }
        }
        "callback is unsuccessful" when {
          "Send Email is successful" should {
            "return ERROR with the status" in {
              disable(AutoClaimEnrolment)

              await(emailRequestRepo.insert(EmailRequest(testVatNumber, testEmail, isDelegated = true)))

              EmailStub.stubSendEmailDelegated(testEmail, agentSuccessEmailTemplate, testVatNumber)(ACCEPTED)

              val res = post(s"/subscription-request/vat-number/$testVatNumber/callback")(Json.obj("state" -> "ERROR"))

              res should have
              httpStatus(BAD_GATEWAY)
            }
          }
        }

        "callback is unsuccessful with EnrolmentError" should {
          "EnrolmentStoreProxy returns an unsuccessful status" should {
            "return EnrolmentError" in {
              disable(AutoClaimEnrolment)

              await(emailRequestRepo.insert(EmailRequest(testVatNumber, testEmail, isDelegated = true)))

              val res = post(s"/subscription-request/vat-number/$testVatNumber/callback")(
                Json.obj("state" -> "EnrolmentError")
              )
              res should have(
                httpStatus(BAD_GATEWAY)
              )
            }
          }
        }
      }
    }
  }
}
