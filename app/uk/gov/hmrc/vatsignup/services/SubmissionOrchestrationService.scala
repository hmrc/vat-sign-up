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

import cats.data._
import cats.implicits._
import javax.inject.{Inject, Singleton}
import play.api.mvc.Request
import uk.gov.hmrc.auth.core.Enrolments
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.vatsignup.repositories.{EmailRequestRepository, SubscriptionRequestRepository}
import uk.gov.hmrc.vatsignup.services.SubmissionOrchestrationService._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SubmissionOrchestrationService @Inject()(signUpRequestService: SignUpRequestService,
                                               submissionService: SubmissionService,
                                               subscriptionRequestRepository: SubscriptionRequestRepository,
                                               emailRequestRepository: EmailRequestRepository
                                              )(implicit ec: ExecutionContext) {

  def submitSignUpRequest(vatNumber: String,
                          enrolments: Enrolments
                         )(implicit hc: HeaderCarrier,
                           request: Request[_]): Future[SubmissionResponse] = {
    val result: EitherT[Future, SignUpRequestSubmissionFailure, SignUpRequestSubmitted.type] = for {
      signUpRequest <- EitherT(signUpRequestService.getSignUpRequest(vatNumber, enrolments)) leftMap {
        case SignUpRequestService.SignUpRequestNotFound => InsufficientData
        case SignUpRequestService.DatabaseFailure => DatabaseFailure
        case SignUpRequestService.InsufficientData => InsufficientData
        case SignUpRequestService.RequestNotAuthorised => InsufficientData
        case SignUpRequestService.EmailVerificationRequired => EmailVerificationRequired
        case SignUpRequestService.EmailVerificationFailure => EmailVerificationFailure
      }
      _ <- EitherT(submissionService.submitSignUpRequest(signUpRequest, enrolments)) leftMap {
        case _: SubmissionService.SignUpFailure => SignUpFailure
        case _: SubmissionService.RegistrationFailure => RegistrationFailure
        case SubmissionService.EnrolmentFailure => EnrolmentFailure
      }
      _ <- saveTemporarySubscriptionData(vatNumber, signUpRequest.transactionEmail.emailAddress, signUpRequest.isDelegated)
      _ <- deleteRecord(vatNumber)
    } yield SignUpRequestSubmitted

    result.value
  }


  private def deleteRecord(vatNumber: String): EitherT[Future, SignUpRequestSubmissionFailure, SignUpRequestDeleted.type] = {
    val delete: Future[Either[SignUpRequestSubmissionFailure, SignUpRequestDeleted.type]] =
      subscriptionRequestRepository.deleteRecord(vatNumber)
        .map(_ => Right(SignUpRequestDeleted))
        .recover {
          case _ => Left(DeleteRecordFailure)
        }
    EitherT(delete)
  }

  private def saveTemporarySubscriptionData(vatNumber: String, transactionEmail: String, isDelegated: Boolean):
  EitherT[Future, SignUpRequestSubmissionFailure, StoreTemporarySubscriptionDataSuccess.type] = {
    val saveEmail: Future[Either[SignUpRequestSubmissionFailure, StoreTemporarySubscriptionDataSuccess.type]] =
      emailRequestRepository.upsertEmail(vatNumber, transactionEmail, isDelegated)
        .map(_ => Right(StoreTemporarySubscriptionDataSuccess))
        .recoverWith { case _ =>
          Future.successful(Left(DeleteRecordFailure))
        }
    EitherT(saveEmail)
  }
}


object SubmissionOrchestrationService {

  type SubmissionResponse = Either[SignUpRequestSubmissionFailure, SignUpRequestSubmitted.type]

  case object SignUpRequestDeleted

  case object StoreTemporarySubscriptionDataSuccess

  case object SignUpRequestSubmitted

  sealed trait SignUpRequestSubmissionFailure

  case object InsufficientData extends SignUpRequestSubmissionFailure

  case object DatabaseFailure extends SignUpRequestSubmissionFailure

  case object EmailVerificationFailure extends SignUpRequestSubmissionFailure

  case object EmailVerificationRequired extends SignUpRequestSubmissionFailure

  case object SignUpFailure extends SignUpRequestSubmissionFailure

  case object RegistrationFailure extends SignUpRequestSubmissionFailure

  case object EnrolmentFailure extends SignUpRequestSubmissionFailure

  case object DeleteRecordFailure extends SignUpRequestSubmissionFailure

}


