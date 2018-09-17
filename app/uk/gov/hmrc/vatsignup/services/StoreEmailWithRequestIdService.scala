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

package uk.gov.hmrc.vatsignup.services

import java.util.NoSuchElementException

import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.auth.core.Enrolments
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.vatsignup.config.AppConfig
import uk.gov.hmrc.vatsignup.config.Constants.AgentEnrolmentKey
import uk.gov.hmrc.vatsignup.connectors.EmailVerificationConnector
import uk.gov.hmrc.vatsignup.httpparsers.CreateEmailVerificationRequestHttpParser.{EmailAlreadyVerified, EmailVerificationRequestSent}
import uk.gov.hmrc.vatsignup.repositories.UnconfirmedSubscriptionRequestRepository
import uk.gov.hmrc.vatsignup.services.StoreEmailWithRequestIdService._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class StoreEmailWithRequestIdService @Inject()(unconfirmedSubscriptionRequestRepository: UnconfirmedSubscriptionRequestRepository,
                                  emailVerificationConnector: EmailVerificationConnector,
                                  appConfig: AppConfig
                                 )(implicit ec: ExecutionContext) {
  def storeEmail(requestId: String,
                 emailAddress: String,
                 enrolments: Enrolments
                )(implicit hc: HeaderCarrier): Future[Either[StoreEmailFailure, StoreEmailSuccess]] = {
    val continueUrl = if (enrolments.getEnrolment(AgentEnrolmentKey).isDefined) {
      appConfig.delegatedVerifyEmailContinueUrl
    } else {
      appConfig.principalVerifyEmailContinueUrl
    }

    unconfirmedSubscriptionRequestRepository.upsertEmail(requestId, emailAddress) flatMap {
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
        Left(EmailDatabaseFailureNoRequestId)
      case _ =>
        Left(EmailDatabaseFailure)
    }
  }


  def storeTransactionEmail(requestId: String,
                            emailAddress: String)
                           (implicit hc: HeaderCarrier): Future[Either[StoreEmailFailure, StoreEmailSuccess]] = {

    val continueUrl = appConfig.agentVerifyEmailContinueUrl

    unconfirmedSubscriptionRequestRepository.upsertTransactionEmail(requestId, emailAddress) flatMap {
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
        Left(EmailDatabaseFailureNoRequestId)
      case _ =>
        Left(EmailDatabaseFailure)
    }
  }

}

object StoreEmailWithRequestIdService {

  case class StoreEmailSuccess(emailVerified: Boolean)

  sealed trait StoreEmailFailure

  object EmailVerificationFailure extends StoreEmailFailure

  object EmailDatabaseFailure extends StoreEmailFailure

  object EmailDatabaseFailureNoRequestId extends StoreEmailFailure

}



