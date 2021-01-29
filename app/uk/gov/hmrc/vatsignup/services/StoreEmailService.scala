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

package uk.gov.hmrc.vatsignup.services

import java.util.NoSuchElementException

import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.auth.core.Enrolments
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.vatsignup.config.AppConfig
import uk.gov.hmrc.vatsignup.config.Constants.AgentEnrolmentKey
import uk.gov.hmrc.vatsignup.connectors.EmailVerificationConnector
import uk.gov.hmrc.vatsignup.httpparsers.CreateEmailVerificationRequestHttpParser.{EmailAlreadyVerified, EmailVerificationRequestSent}
import uk.gov.hmrc.vatsignup.repositories.SubscriptionRequestRepository
import uk.gov.hmrc.vatsignup.services.StoreEmailService._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class StoreEmailService @Inject()(subscriptionRequestRepository: SubscriptionRequestRepository,
                                  emailVerificationConnector: EmailVerificationConnector,
                                  appConfig: AppConfig
                                 )(implicit ec: ExecutionContext) {
  def storeEmail(vatNumber: String,
                 emailAddress: String,
                 enrolments: Enrolments
                )(implicit hc: HeaderCarrier): Future[Either[StoreEmailFailure, StoreEmailSuccess]] = {
    val continueUrl = if (enrolments.getEnrolment(AgentEnrolmentKey).isDefined) {
      appConfig.delegatedVerifyEmailContinueUrl
    } else {
      appConfig.principalVerifyEmailContinueUrl
    }

    subscriptionRequestRepository.upsertEmail(vatNumber, emailAddress) flatMap {
      _ =>
        emailVerificationConnector.createEmailVerificationRequest(emailAddress, continueUrl) map {
          case Right(EmailVerificationRequestSent) =>
            Right(StoreEmailSuccess(emailVerified = false))
          case Right(EmailAlreadyVerified) =>
            Right(StoreEmailSuccess(emailVerified = true))
          case _ =>
            Left(EmailVerificationFailure)
        }
    } recover {
      case e: NoSuchElementException =>
        Left(EmailDatabaseFailureNoVATNumber)
      case _ =>
        Left(EmailDatabaseFailure)
    }
  }


  def storeTransactionEmail(vatNumber: String,
                            emailAddress: String,
                            enrolments: Enrolments)
                           (implicit hc: HeaderCarrier): Future[Either[StoreEmailFailure, StoreEmailSuccess]] = {
    val continueUrl = if (enrolments.getEnrolment(AgentEnrolmentKey).isDefined) {
      appConfig.agentVerifyEmailContinueUrl
    } else {
      appConfig.principalVerifyEmailContinueUrl
    }
    subscriptionRequestRepository.upsertTransactionEmail(vatNumber, emailAddress) flatMap {
      _ =>
        emailVerificationConnector.createEmailVerificationRequest(emailAddress, continueUrl) map {
          case Right(EmailVerificationRequestSent) =>
            Right(StoreEmailSuccess(emailVerified = false))
          case Right(EmailAlreadyVerified) =>
            Right(StoreEmailSuccess(emailVerified = true))
          case _ =>
            Left(EmailVerificationFailure)
        }
    } recover {
      case e: NoSuchElementException =>
        Left(EmailDatabaseFailureNoVATNumber)
      case _ =>
        Left(EmailDatabaseFailure)
    }
  }

}

object StoreEmailService {

  case class StoreEmailSuccess(emailVerified: Boolean)

  sealed trait StoreEmailFailure

  object EmailVerificationFailure extends StoreEmailFailure

  object EmailDatabaseFailure extends StoreEmailFailure

  object EmailDatabaseFailureNoVATNumber extends StoreEmailFailure

}
