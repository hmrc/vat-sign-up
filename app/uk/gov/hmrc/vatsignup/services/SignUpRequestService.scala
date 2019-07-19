/*
 * Copyright 2019 HM Revenue & Customs
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

import cats.data.EitherT
import cats.implicits._
import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.auth.core.Enrolments
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.vatsignup.config.featureswitch.FeatureSwitching
import uk.gov.hmrc.vatsignup.connectors.EmailVerificationConnector
import uk.gov.hmrc.vatsignup.httpparsers.GetEmailVerificationStateHttpParser._
import uk.gov.hmrc.vatsignup.models.SignUpRequest.EmailAddress
import uk.gov.hmrc.vatsignup.models._
import uk.gov.hmrc.vatsignup.repositories.SubscriptionRequestRepository
import uk.gov.hmrc.vatsignup.services.SignUpRequestService.{RequestAuthorised, _}
import uk.gov.hmrc.vatsignup.utils.EnrolmentUtils._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SignUpRequestService @Inject()(subscriptionRequestRepository: SubscriptionRequestRepository,
                                     emailVerificationConnector: EmailVerificationConnector
                                    )(implicit ec: ExecutionContext)
  extends FeatureSwitching {
  def getSignUpRequest(vatNumber: String, enrolments: Enrolments)(implicit hc: HeaderCarrier): Future[Either[GetSignUpRequestFailure, SignUpRequest]] = {
    val isDelegated = enrolments.agentReferenceNumber.isDefined
    val hasPartnershipEnrolment = enrolments.partnershipUtr.isDefined

    subscriptionRequestRepository.findById(vatNumber) flatMap {
      case Some(subscriptionRequest) =>
        val hasVatEnrolment = enrolments.vatNumber contains subscriptionRequest.vatNumber

        val res: EitherT[Future, GetSignUpRequestFailure, SignUpRequest] = for {
          businessEntity    <- EitherT.fromOption[Future](subscriptionRequest.businessEntity, InsufficientData)
          contactPreference <- EitherT.fromOption[Future](subscriptionRequest.contactPreference, InsufficientData)
          _ <- EitherT.fromEither[Future](checkAuthorisation(
            businessEntity,
            subscriptionRequest,
            isDelegated,
            hasVatEnrolment,
            hasPartnershipEnrolment
          ))
          optSignUpEmail    <- EitherT(getSignUpEmail(subscriptionRequest, isDelegated))
          transactionEmail  <- EitherT(getTransactionEmail(subscriptionRequest, optSignUpEmail))
          isMigratable = subscriptionRequest.isMigratable
        } yield SignUpRequest(
          subscriptionRequest.vatNumber,
          businessEntity,
          optSignUpEmail,
          transactionEmail,
          isDelegated,
          isMigratable,
          contactPreference
        )
        res.value
      case None =>
        Future.successful(Left(SignUpRequestNotFound))
    } recover {
      case _ => Left(DatabaseFailure)
    }
  }

  private def checkAuthorisation(businessEntity: BusinessEntity,
                                 subscriptionRequest: SubscriptionRequest,
                                 isDelegated: Boolean,
                                 hasVatEnrolment: Boolean,
                                 hasPartnershipEnrolment: Boolean): Either[GetSignUpRequestFailure, RequestAuthorised.type] =
    businessEntity match {
      case _: LimitedCompany =>
        Right(RequestAuthorised)
      case _: SoleTrader =>
        Right(RequestAuthorised)
      case _: PartnershipBusinessEntity =>
        Right(RequestAuthorised)
      case VatGroup | AdministrativeDivision | UnincorporatedAssociation | Trust | Charity | GovernmentOrganisation | Overseas | JointVenture =>
        Right(RequestAuthorised)
      case _: OverseasWithUkEstablishment if subscriptionRequest.ctReference.isDefined =>
        Right(RequestAuthorised)
      case _: RegisteredSociety =>
        Right(RequestAuthorised)
      case _ if isDelegated =>
        Right(RequestAuthorised)
      case _ =>
        Left(RequestNotAuthorised)
    }

  private def getSignUpEmail(subscriptionRequest: SubscriptionRequest,
                             isDelegated: Boolean)(implicit hc: HeaderCarrier): Future[Either[GetSignUpRequestFailure, Option[EmailAddress]]] = {
    subscriptionRequest.email match {
      case Some(signUpEmail) =>
        emailVerificationConnector.getEmailVerificationState(signUpEmail) map {
          case Right(EmailVerified) =>
            Right(Some(EmailAddress(signUpEmail, isVerified = true)))
          case Right(EmailNotVerified) if isDelegated =>
            Right(Some(EmailAddress(signUpEmail, isVerified = false)))
          case Right(EmailNotVerified) =>
            Left(EmailVerificationRequired)
          case Left(_) =>
            Left(EmailVerificationFailure)
        }
      case None if !subscriptionRequest.isDirectDebit && (subscriptionRequest.contactPreference contains Paper) =>
        Future.successful(Right(None))
      case None if isDelegated =>
        Future.successful(Right(None))
      case None =>
        Future.successful(Left(InsufficientData))
    }
  }

  private def getTransactionEmail(subscriptionRequest: SubscriptionRequest,
                                  optSignUpEmail: Option[EmailAddress]
                                 )(implicit hc: HeaderCarrier): Future[Either[GetSignUpRequestFailure, EmailAddress]] = {
    (subscriptionRequest.transactionEmail, optSignUpEmail) match {
      case (Some(transactionEmail), _) =>
        emailVerificationConnector.getEmailVerificationState(transactionEmail) map {
          case Right(EmailVerified) =>
            Right(EmailAddress(transactionEmail, isVerified = true))
          case Right(EmailNotVerified) =>
            Left(EmailVerificationRequired)
          case Left(_) =>
            Left(EmailVerificationFailure)
        }
      case (None, Some(signUpEmail)) =>
        Future.successful(Right(signUpEmail))
      case _ =>
        Future.successful(Left(InsufficientData))

    }
  }
}

object SignUpRequestService {

  case object RequestAuthorised

  sealed trait GetSignUpRequestFailure

  case object SignUpRequestNotFound extends GetSignUpRequestFailure

  case object DatabaseFailure extends GetSignUpRequestFailure

  case object InsufficientData extends GetSignUpRequestFailure

  case object RequestNotAuthorised extends GetSignUpRequestFailure

  case object EmailVerificationRequired extends GetSignUpRequestFailure

  case object EmailVerificationFailure extends GetSignUpRequestFailure

}
