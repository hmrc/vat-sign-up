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

package uk.gov.hmrc.vatsignup.service

import com.github.tomakehurst.wiremock.core.WireMockApp
import play.api.http.Status.BAD_REQUEST
import reactivemongo.api.commands.WriteResult
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.vatsignup.config.featureswitch.EmailNotification
import uk.gov.hmrc.vatsignup.config.mocks.MockConfig
import uk.gov.hmrc.vatsignup.connectors.mocks.MockEmailConnector
import uk.gov.hmrc.vatsignup.repositories.mocks.MockEmailRequestRepository
import uk.gov.hmrc.vatsignup.services.SubscriptionNotificationService
import uk.gov.hmrc.vatsignup.helpers.TestConstants._
import uk.gov.hmrc.vatsignup.httpparsers.SendEmailHttpParser.{EmailQueued, SendEmailFailure}
import uk.gov.hmrc.vatsignup.models.EmailRequest
import uk.gov.hmrc.vatsignup.models.SubscriptionState.{Failure, Success}
import uk.gov.hmrc.vatsignup.services.SubscriptionNotificationService._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class SubscriptionNotificationServiceSpec extends UnitSpec
  with MockEmailRequestRepository
  with MockEmailConnector
  with MockConfig {

  object TestSubscriptionNotificationService extends SubscriptionNotificationService(
    mockEmailRequestRepository,
    mockEmailConnector,
    mockConfig
  )

  private implicit val headerCarrier: HeaderCarrier = HeaderCarrier()

  "sendEmailNotification" when {
    "the EmailNotification feature is enabled" when {
      "the email request exists in the database" when {
        "the subscription request was for a principal user" when {
          "the e-mail request is successful" when {
            "the subscription was successful" should {
              "return NotificationSent" in {
                enable(EmailNotification)

                val testEmailRequest = EmailRequest(testVatNumber, testEmail, isDelegated = false)

                mockFindEmailRequestById(testVatNumber)(Future.successful(Some(testEmailRequest)))
                mockRemoveEmailRequest(testVatNumber)(Future.successful(mock[WriteResult]))
                mockSendEmail(testEmail, principalSuccessEmailTemplate)(Future.successful(Right(EmailQueued)))
                val res = await(TestSubscriptionNotificationService.sendEmailNotification(testVatNumber, Success))

                res shouldBe Right(NotificationSent)
              }
            }
            "the subscription failed" should {
              "return no email" in {
                enable(EmailNotification)

                val testEmailRequest = EmailRequest(testVatNumber, testEmail, isDelegated = false)

                mockFindEmailRequestById(testVatNumber)(Some(testEmailRequest))
             //   mockSendEmail(testEmail, principalSuccessEmailTemplate)(Future.successful(Left(SendEmailFailure(PRECONDITION_FAILED, ""))))
                val res = await(TestSubscriptionNotificationService.sendEmailNotification(testVatNumber, Failure))

                res shouldBe Left(EmailRequestDataNotFound)
              }
            }
          }
          "the e-mail request fails" should {
            "return EmailServiceFailure" in {
              enable(EmailNotification)

              val testEmailRequest = EmailRequest(testVatNumber, testEmail, isDelegated = false)

              mockFindEmailRequestById(testVatNumber)(Some(testEmailRequest))
              mockSendEmail(testEmail, principalSuccessEmailTemplate)(Future.successful(Left(SendEmailFailure(BAD_REQUEST, ""))))
              val res = await(TestSubscriptionNotificationService.sendEmailNotification(testVatNumber, Success))

              res shouldBe Left(EmailServiceFailure)
            }
          }
        }
        "the subscription request was for a delegated user" when {
          "return DelegatedSubscription" in {
            enable(EmailNotification)

            val testEmailRequest = EmailRequest(testVatNumber, testEmail, isDelegated = true)

            mockFindEmailRequestById(testVatNumber)(Some(testEmailRequest))
            mockRemoveEmailRequest(testVatNumber)(Future.successful(mock[WriteResult]))

            val res = await(TestSubscriptionNotificationService.sendEmailNotification(testVatNumber, Success))

            res shouldBe Right(DelegatedSubscription)
          }
        }
      }
      "the e-mail request does not exist in the database" should {
        "return EmailRequestDataNotFound" in {
          enable(EmailNotification)

          mockFindEmailRequestById(testVatNumber)(None)
          val res = await(TestSubscriptionNotificationService.sendEmailNotification(testVatNumber, Failure))

          res shouldBe Left(EmailRequestDataNotFound)
        }
      }
    }
    "the EmailNotification feature is disabled" should {
      "return FeatureSwitchDisabled" in {
        disable(EmailNotification)

        val res = await(TestSubscriptionNotificationService.sendEmailNotification(testVatNumber, Success))

        res shouldBe Right(FeatureSwitchDisabled)
      }
    }
  }
}
