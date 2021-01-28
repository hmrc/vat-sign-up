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

import play.api.http.Status.BAD_REQUEST
import play.api.test.Helpers._
import play.api.test.FakeRequest
import reactivemongo.api.commands.WriteResult
import uk.gov.hmrc.http.HeaderCarrier
import org.scalatest.{WordSpec, Matchers}
import uk.gov.hmrc.vatsignup.config.featureswitch.AutoClaimEnrolment
import uk.gov.hmrc.vatsignup.config.mocks.MockConfig
import uk.gov.hmrc.vatsignup.connectors.mocks.MockEmailConnector
import uk.gov.hmrc.vatsignup.helpers.TestConstants._
import uk.gov.hmrc.vatsignup.httpparsers.SendEmailHttpParser.{EmailQueued, SendEmailFailure}
import uk.gov.hmrc.vatsignup.models.EmailRequest
import uk.gov.hmrc.vatsignup.models.SubscriptionState.{AuthRefreshed, Failure, Success}
import uk.gov.hmrc.vatsignup.repositories.mocks.MockEmailRequestRepository
import uk.gov.hmrc.vatsignup.service.mocks.{MockAutoClaimEnrolmentService, MockCheckEnrolmentAllocationService}
import uk.gov.hmrc.vatsignup.services.CheckEnrolmentAllocationService._
import uk.gov.hmrc.vatsignup.services.SubscriptionNotificationService._
import uk.gov.hmrc.vatsignup.services.{AutoClaimEnrolmentService, SubscriptionNotificationService}
import uk.gov.hmrc.vatsignup.service.mocks.monitoring.MockAuditService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class SubscriptionNotificationServiceSpec extends WordSpec with Matchers
  with MockEmailRequestRepository
  with MockEmailConnector
  with MockAutoClaimEnrolmentService
  with MockCheckEnrolmentAllocationService
  with MockConfig {

  object TestSubscriptionNotificationService extends SubscriptionNotificationService(
    mockEmailRequestRepository,
    mockEmailConnector,
    mockAutoClaimEnrolmentService,
    mockCheckEnrolmentAllocationService,
    mockConfig
  )

  private implicit val headerCarrier: HeaderCarrier = HeaderCarrier()
  private implicit val request = FakeRequest("POST", "testUrl")
  val agentTriggerPoint = "Agent sign-up"

  "sendEmailNotification" when {
    "the email request exists in the database" when {
      "the subscription request was for a principal user" when {

        val testEmailRequest = EmailRequest(testVatNumber, testEmail, isDelegated = false)

        "the subscription was successful" should {
          "the e-mail request is successful" when {
            "return NotificationSent" in {

              mockFindEmailRequestById(testVatNumber)(Future.successful(Some(testEmailRequest)))
              mockRemoveEmailRequest(testVatNumber)(Future.successful(mock[WriteResult]))
              mockSendEmail(testEmail, principalSuccessEmailTemplate, Some(testVatNumber))(Future.successful(Right(EmailQueued)))
              val res = await(TestSubscriptionNotificationService.sendEmailNotification(testVatNumber, Success))

              res shouldBe Right(NotificationSent)
            }
          }

          "the e-mail request fails" should {
            "return EmailServiceFailure" in {

              mockFindEmailRequestById(testVatNumber)(Future.successful(Some(testEmailRequest)))
              mockSendEmail(testEmail, principalSuccessEmailTemplate, Some(testVatNumber))(Future.successful(Left(SendEmailFailure(BAD_REQUEST, ""))))
              val res = await(TestSubscriptionNotificationService.sendEmailNotification(testVatNumber, Success))

              res shouldBe Left(EmailServiceFailure)
            }
          }
        }

        "the subscription failed" when {
          "ES1 returns EnrolmentAlreadyAllocated" when {

            "the e-mail request is successful" when {
              "return NotificationSent" in {

                mockFindEmailRequestById(testVatNumber)(Future.successful(Some(testEmailRequest)))
                mockGetGroupIdForMtdVatEnrolment(testVatNumber, ignoreAssignments = false)(Future.successful(Left(EnrolmentAlreadyAllocated(""))))
                mockSendEmail(testEmail, principalSuccessEmailTemplate, Some(testVatNumber))(Future.successful(Right(EmailQueued)))
                mockRemoveEmailRequest(testVatNumber)(Future.successful(mock[WriteResult]))

                val res = await(TestSubscriptionNotificationService.sendEmailNotification(testVatNumber, Failure))

                res shouldBe Right(NotificationSent)
              }
            }

            "the e-mail request fails" should {
              "return EmailServiceFailure" in {

                mockFindEmailRequestById(testVatNumber)(Future.successful(Some(testEmailRequest)))
                mockGetGroupIdForMtdVatEnrolment(testVatNumber, ignoreAssignments = false)(Future.successful(Left(EnrolmentAlreadyAllocated(""))))
                mockSendEmail(testEmail, principalSuccessEmailTemplate, Some(testVatNumber))(Future.successful(Left(SendEmailFailure(BAD_REQUEST, ""))))
                val res = await(TestSubscriptionNotificationService.sendEmailNotification(testVatNumber, Failure))

                res shouldBe Left(EmailServiceFailure)
              }
            }
          }

          "ES1 returns EnrolmentNotAllocated" should {
            "return no email" in {

              mockFindEmailRequestById(testVatNumber)(Future.successful(Some(testEmailRequest)))
              mockGetGroupIdForMtdVatEnrolment(testVatNumber, ignoreAssignments = false)(Future.successful(Right(EnrolmentNotAllocated)))
              mockRemoveEmailRequest(testVatNumber)(Future.successful(mock[WriteResult]))

              val res = await(TestSubscriptionNotificationService.sendEmailNotification(testVatNumber, Failure))

              res shouldBe Right(TaxEnrolmentFailure)
            }

            "ignore the new states from ETMP, treat them as a Failure and don't send the email" in {

              mockFindEmailRequestById(testVatNumber)(Future.successful(Some(testEmailRequest)))
              mockGetGroupIdForMtdVatEnrolment(testVatNumber, ignoreAssignments = false)(Future.successful(Right(EnrolmentNotAllocated)))
              mockRemoveEmailRequest(testVatNumber)(Future.successful(mock[WriteResult]))

              val res = await(TestSubscriptionNotificationService.sendEmailNotification(testVatNumber, AuthRefreshed))

              res shouldBe Right(TaxEnrolmentFailure)
            }
          }

          "ES1 returns a Failure" should {
            "return no email" in {

              mockFindEmailRequestById(testVatNumber)(Future.successful(Some(testEmailRequest)))
              mockGetGroupIdForMtdVatEnrolment(testVatNumber, ignoreAssignments = false)(Future.successful(Left(UnexpectedEnrolmentStoreProxyFailure(BAD_REQUEST))))
              mockRemoveEmailRequest(testVatNumber)(Future.successful(mock[WriteResult]))

              val res = await(TestSubscriptionNotificationService.sendEmailNotification(testVatNumber, Failure))

              res shouldBe Right(TaxEnrolmentFailure)
            }

            "ignore the new states from ETMP, treat them as a Failure and don't send the email" in {

              mockFindEmailRequestById(testVatNumber)(Future.successful(Some(testEmailRequest)))
              mockGetGroupIdForMtdVatEnrolment(testVatNumber, ignoreAssignments = false)(Future.successful(Left(UnexpectedEnrolmentStoreProxyFailure(BAD_REQUEST))))
              mockRemoveEmailRequest(testVatNumber)(Future.successful(mock[WriteResult]))

              val res = await(TestSubscriptionNotificationService.sendEmailNotification(testVatNumber, AuthRefreshed))

              res shouldBe Right(TaxEnrolmentFailure)
            }
          }
        }
      }

      "the subscription request was for a delegated user" when {

        val testEmailRequest = EmailRequest(testVatNumber, testEmail, isDelegated = true)
        "the feature switch is disabled" when {
          "the e-mail request is successful" should {
            "return DelegatedSubscription" in {
              disable(AutoClaimEnrolment)

              mockFindEmailRequestById(testVatNumber)(Future.successful(Some(testEmailRequest)))
              mockRemoveEmailRequest(testVatNumber)(Future.successful(mock[WriteResult]))
              mockSendEmail(testEmail, agentSuccessEmailTemplate, Some(testVatNumber))(Future.successful(Right(EmailQueued)))

              val res = await(TestSubscriptionNotificationService.sendEmailNotification(testVatNumber, Success))

              res shouldBe Right(DelegatedSubscription)
            }
          }
          "the e-mail request fails" should {
            "return EmailServiceFailure" in {
              disable(AutoClaimEnrolment)

              mockFindEmailRequestById(testVatNumber)(Future.successful(Some(testEmailRequest)))
              mockSendEmail(testEmail, agentSuccessEmailTemplate, Some(testVatNumber))(Future.successful(Left(SendEmailFailure(BAD_REQUEST, ""))))
              val res = await(TestSubscriptionNotificationService.sendEmailNotification(testVatNumber, Success))

              res shouldBe Left(EmailServiceFailure)
            }
          }
        }
        "the feature switch is enabled" when {
          "the auto claim enrolment is successful" when {
            "the e-mail request is successful" should {
              "return AutoClaimDelegatedSubscription" in {
                enable(AutoClaimEnrolment)

                mockFindEmailRequestById(testVatNumber)(Future.successful(Some(testEmailRequest)))
                mockRemoveEmailRequest(testVatNumber)(Future.successful(mock[WriteResult]))
                mockAutoClaimEnrolment(testVatNumber,agentTriggerPoint)(Future.successful(Right(AutoClaimEnrolmentService.EnrolmentAssigned)))
                mockSendEmail(testEmail, agentSuccessEmailTemplate, Some(testVatNumber))(Future.successful(Right(EmailQueued)))

                val res = await(TestSubscriptionNotificationService.sendEmailNotification(testVatNumber, Success))

                res shouldBe Right(AutoEnroledAndSubscribed)
              }
            }
            "the e-mail request fails" should {
              "return EmailServiceFailure" in {
                enable(AutoClaimEnrolment)

                mockFindEmailRequestById(testVatNumber)(Future.successful(Some(testEmailRequest)))
                mockAutoClaimEnrolment(testVatNumber,agentTriggerPoint)(Future.successful(Right(AutoClaimEnrolmentService.EnrolmentAssigned)))

                mockSendEmail(testEmail, agentSuccessEmailTemplate, Some(testVatNumber))(Future.successful(Left(SendEmailFailure(status = BAD_REQUEST, body = ""))))
                val res = await(TestSubscriptionNotificationService.sendEmailNotification(testVatNumber, Success))

                res shouldBe Left(EmailServiceFailure)
              }
            }
          }
          "the auto claim enrolment fails but the email request is successful" when {
            "return AutoClaimFailedDelegatedSubscription" in {
              enable(AutoClaimEnrolment)

              mockFindEmailRequestById(testVatNumber)(Future.successful(Some(testEmailRequest)))
              mockRemoveEmailRequest(testVatNumber)(Future.successful(mock[WriteResult]))
              mockAutoClaimEnrolment(testVatNumber,agentTriggerPoint)(Future.successful(Left(AutoClaimEnrolmentService.NoUsersFound)))
              mockSendEmail(testEmail, agentSuccessEmailTemplate, Some(testVatNumber))(Future.successful(Right(EmailQueued)))

              val res = await(TestSubscriptionNotificationService.sendEmailNotification(testVatNumber, Success))

              res shouldBe Right(NotAutoEnroledButSubscribed)
            }
          }
        }
      }
    }
  }

  "the e-mail request does not exist in the database" should {
    "return EmailRequestDataNotFound" in {
      mockFindEmailRequestById(testVatNumber)(Future.successful(None))
      val res = await(TestSubscriptionNotificationService.sendEmailNotification(testVatNumber, Failure))

      res shouldBe Left(EmailRequestDataNotFound)
    }
  }

}
