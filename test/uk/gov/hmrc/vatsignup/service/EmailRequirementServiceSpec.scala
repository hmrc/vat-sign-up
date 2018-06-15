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

import org.scalatest.EitherValues
import play.api.http.Status._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.vatsignup.connectors.mocks._
import uk.gov.hmrc.vatsignup.helpers.TestConstants._
import uk.gov.hmrc.vatsignup.httpparsers.GetEmailVerificationStateHttpParser.{EmailNotVerified, EmailVerified, GetEmailVerificationStateErrorResponse}
import uk.gov.hmrc.vatsignup.services.EmailRequirementService.{Email, EmailNotSupplied, GetEmailVerificationFailure, UnVerifiedEmail}
import uk.gov.hmrc.vatsignup.services._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class EmailRequirementServiceSpec extends UnitSpec with EitherValues with MockEmailVerificationConnector {

  object TestEmailRequirementService extends EmailRequirementService(
    mockEmailVerificationConnector
  )

  private implicit val hc: HeaderCarrier = HeaderCarrier()


  "Checking user email requiremnets" when {

    "user is principal" when {

      "email address is verified" should {
        "return email and verification state" in {
          mockGetEmailVerificationState(testEmail)(Future.successful(Right(EmailVerified)))
          val res = TestEmailRequirementService.checkRequirements(Some(testEmail), None, false)
          await(res) shouldBe Right(Email(testEmail, isVerified = true, shouldSubmit = true))

        }
      }

      "email address is not verified" should {
        "return UnVerifiedEmail" in {
          mockGetEmailVerificationState(testEmail)(Future.successful(Right(EmailNotVerified)))
          val res = TestEmailRequirementService.checkRequirements(Some(testEmail), None, false)
          await(res) shouldBe Left(UnVerifiedEmail)
        }
      }

      "email address does not exist" should {
        "return EmailNotSupplied" in {
          val res = TestEmailRequirementService.checkRequirements(None, None, false)
          await(res) shouldBe Left(EmailNotSupplied)
        }
      }
    }


    "user is agent" when {

      "transaction email has been supplied" when {

        "email address is verified" should {
          "return email and verification state" in {
            mockGetEmailVerificationState(testEmail)(Future.successful(Right(EmailVerified)))
            val res = TestEmailRequirementService.checkRequirements(None, Some(testEmail), true)
            await(res) shouldBe Right(Email(testEmail, isVerified = true, shouldSubmit = false))

          }
        }

        "email address is not verified" should {
          "return UnVerifiedEmail" in {
            mockGetEmailVerificationState(testEmail)(Future.successful(Right(EmailNotVerified)))
            val res = TestEmailRequirementService.checkRequirements(None, Some(testEmail), true)
            await(res) shouldBe Left(UnVerifiedEmail)
          }
        }

        "email address does not exist" should {
          "return EmailNotSupplied" in {
            val res = TestEmailRequirementService.checkRequirements(None, None, true)
            await(res) shouldBe Left(EmailNotSupplied)
          }
        }
      }


      "transaction email has not been supplied" when {

        "principal email has been supplied" when {
          "principal email address is verified" should {
            "return email and verification state" in {
              mockGetEmailVerificationState(testEmail)(Future.successful(Right(EmailVerified)))
              val res = TestEmailRequirementService.checkRequirements(Some(testEmail), None, true)
              await(res) shouldBe Right(Email(testEmail, isVerified = true, shouldSubmit = true))

            }
          }

          "principal email address is not verified" should {
            "return email and failed verification state " in {
              mockGetEmailVerificationState(testEmail)(Future.successful(Right(EmailNotVerified)))
              val res = TestEmailRequirementService.checkRequirements(Some(testEmail), None, true)
              await(res) shouldBe Right(Email(testEmail, isVerified = false, shouldSubmit = true))

            }
          }
        }

        "principal email has also not been supplied" should {
          "return EmailNotSupplied" in {
            val res = TestEmailRequirementService.checkRequirements(None, None, true)
            await(res) shouldBe Left(EmailNotSupplied)
          }
        }
      }
    }

    "connection to email verification service fails" in {
      mockGetEmailVerificationState(testEmail)(Future.successful(Left(GetEmailVerificationStateErrorResponse(INTERNAL_SERVER_ERROR, ""))))
      val res = TestEmailRequirementService.checkRequirements(Some(testEmail), None, true)
      await(res) shouldBe Left(GetEmailVerificationFailure)
    }
  }
}