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
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.vatsignup.connectors.EmailPasscodeVerificationConnector
import uk.gov.hmrc.vatsignup.httpparsers.EmailPasscodeVerificationHttpParser._
import uk.gov.hmrc.vatsignup.models.StoreTransactionEmailRequest
import uk.gov.hmrc.vatsignup.repositories.SubscriptionRequestRepository
import uk.gov.hmrc.vatsignup.services.NewStoreEmailService._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class NewStoreEmailService @Inject()(subscriptionRequestRepository: SubscriptionRequestRepository,
                                     emailPasscodeVerificationConnector: EmailPasscodeVerificationConnector
                                    )(implicit ec: ExecutionContext) {

  def storeTransactionEmail(vatNumber: String,
                            request: StoreTransactionEmailRequest
                           )(implicit hc: HeaderCarrier): Future[Either[StoreEmailFailure, StoreEmailSuccess.type]] = {

    emailPasscodeVerificationConnector.verifyEmailPasscode(request.transactionEmail, request.passCode) flatMap {
      case EmailVerifiedSuccessfully | EmailAlreadyVerified =>
        subscriptionRequestRepository.upsertTransactionEmail(vatNumber, request.transactionEmail) map { _ =>
          Right(StoreEmailSuccess)
        } recover {
          case e: NoSuchElementException =>
            Left(EmailDatabaseFailureNoVATNumber)
          case _ =>
            Left(EmailDatabaseFailure)
        }
      case PasscodeMismatch =>
        Future.successful(Left(EmailVerificationFailure(passcodeMismatchKey)))
      case PasscodeNotFound =>
        Future.successful(Left(EmailVerificationFailure(passcodeNotFoundKey)))
      case MaxAttemptsExceeded =>
        Future.successful(Left(EmailVerificationFailure(maxAttemptsExceededKey)))
    }
  }

}

object NewStoreEmailService {

  object StoreEmailSuccess

  sealed trait StoreEmailFailure

  case class EmailVerificationFailure(reason: String) extends StoreEmailFailure

  object EmailDatabaseFailure extends StoreEmailFailure

  object EmailDatabaseFailureNoVATNumber extends StoreEmailFailure

}
