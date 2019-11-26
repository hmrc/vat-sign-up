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
import play.api.Logger
import play.api.mvc.Request
import reactivemongo.api.commands.WriteResult
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.vatsignup.config.AppConfig
import uk.gov.hmrc.vatsignup.config.featureswitch.{AutoClaimEnrolment, FeatureSwitching}
import uk.gov.hmrc.vatsignup.connectors.EmailConnector
import uk.gov.hmrc.vatsignup.httpparsers.SendEmailHttpParser
import uk.gov.hmrc.vatsignup.models.SubscriptionState._
import uk.gov.hmrc.vatsignup.models.monitoring.AutoClaimEnrolementAuditing.AutoClaimEnrolementAuditingModel
import uk.gov.hmrc.vatsignup.models.{EmailRequest, SubscriptionState}
import uk.gov.hmrc.vatsignup.repositories.EmailRequestRepository
import uk.gov.hmrc.vatsignup.services.SubscriptionNotificationService._
import uk.gov.hmrc.vatsignup.services.monitoring.AuditService

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SubscriptionNotificationService @Inject()(emailRequestRepository: EmailRequestRepository,
                                                emailConnector: EmailConnector,
                                                autoClaimEnrolmentService: AutoClaimEnrolmentService,
                                                checkEnrolmentAllocationService: CheckEnrolmentAllocationService,
                                                appConfig: AppConfig,
                                                auditService: AuditService
                                               )(implicit ec: ExecutionContext)
  extends FeatureSwitching {
  def sendEmailNotification(vatNumber: String,
                            subscriptionState: SubscriptionState
                           )(implicit hc: HeaderCarrier,request:  Request[_]): Future[Either[NotificationFailure, NotificationSuccess]] = {
    (for {
      emailRequest <- getEmailRequest(vatNumber)
      notificationResult <- EitherT(sendEmail(emailRequest.email, vatNumber, subscriptionState, emailRequest.isDelegated))
      _ <- deleteEmailRequest(vatNumber)
    } yield notificationResult).value
  }


  private def getEmailRequest(vatNumber: String): EitherT[Future, NotificationFailure, EmailRequest] =
    EitherT.fromOptionF(emailRequestRepository.findById(vatNumber), EmailRequestDataNotFound)

  private def deleteEmailRequest(vatNumber: String): EitherT[Future, NotificationFailure, WriteResult] =
    EitherT.liftF(emailRequestRepository.removeById(vatNumber))


  private def autoEnrolment(vatNumber: String)
                           (implicit hc: HeaderCarrier,request:  Request[_]): Future[Either[NotificationFailure, NotificationSuccess]] = {
    autoClaimEnrolmentService.autoClaimEnrolment(vatNumber) map {
      case Right(_) =>
        auditService.audit(AutoClaimEnrolementAuditingModel(vatNumber, isSuccess = true, isAgent = true))
        Right(AutoClaimEnrol)
      case Left(error) =>
        auditService.audit(AutoClaimEnrolementAuditingModel(vatNumber, isSuccess = false, isAgent = true, Some(error.toString)))
        Left(AutoClaimEnrolmentFailure)
    }
  }

  private def sendEmailDelegated(emailAddress: String, vatNumber: String, subscriptionState: SubscriptionState)
                                (implicit hc: HeaderCarrier): Future[Either[NotificationFailure, NotificationSuccess]] = {
    emailConnector.sendEmail(emailAddress, agentSuccessEmailTemplate, Some(vatNumber)) map {
      case Left(SendEmailHttpParser.SendEmailFailure(_, _)) => Left(EmailServiceFailure)
      case Right(_) => Right(DelegatedSubscription)
    }
  }

  private def sendEmailIndividual(emailAddress: String, vatNumber: String, subscriptionState: SubscriptionState)
                                 (implicit hc: HeaderCarrier): Future[Either[NotificationFailure, NotificationSuccess]] = {
    subscriptionState match {
      case Success => emailConnector.sendEmail(emailAddress, principalSuccessEmailTemplate, None) map {
        case Left(_) => Left(EmailServiceFailure)
        case Right(_) => Right(NotificationSent)
      }
      case _ => checkEnrolmentAllocationService.getGroupIdForMtdVatEnrolment(vatNumber) flatMap {
        case Left(CheckEnrolmentAllocationService.EnrolmentAlreadyAllocated(_)) =>
          emailConnector.sendEmail(emailAddress, principalSuccessEmailTemplate, None) map {
            case Left(_) => Left(EmailServiceFailure)
            case Right(_) => Right(NotificationSent)
          }
        case _ =>
          Logger.error(s"Tax Enrolment Failure vrn=$vatNumber")
          Future.successful(Right(TaxEnrolmentFailure))
      }
    }
  }

  private def sendEmail(emailAddress: String,
                        vatNumber: String,
                        subscriptionState: SubscriptionState,
                        isDelegated: Boolean
                       )(implicit hc: HeaderCarrier, request:  Request[_]): Future[Either[NotificationFailure, NotificationSuccess]] = {
    if (isDelegated) {
      if (isEnabled(AutoClaimEnrolment)) {
        sendEmailDelegated(emailAddress, vatNumber, subscriptionState).flatMap {
          case Right(DelegatedSubscription) =>
            autoEnrolment(vatNumber) map {
              case Right(_) => Right(AutoEnroledAndSubscribed)
              case Left(_) => Right(NotAutoEnroledButSubscribed)
            }
          case _ => Future.successful(Left(EmailServiceFailure))
        }
      }
      else sendEmailDelegated(emailAddress, vatNumber, subscriptionState)
    }
    else sendEmailIndividual(emailAddress, vatNumber, subscriptionState)
  }
}

object SubscriptionNotificationService {
  val principalSuccessEmailTemplate = "mtdfb_vat_principal_sign_up_successful"
  val agentSuccessEmailTemplate = "mtdfb_vat_agent_sign_up_successful"

  sealed trait NotificationSuccess

  case object FeatureSwitchDisabled extends NotificationSuccess

  case object NotificationSent extends NotificationSuccess

  case object DelegatedSubscription extends NotificationSuccess

  case object TaxEnrolmentFailure extends NotificationSuccess

  case object AutoClaimEnrol extends NotificationSuccess

  case object NotAutoEnroledButSubscribed extends NotificationSuccess

  case object AutoEnroledAndSubscribed extends NotificationSuccess

  sealed trait NotificationFailure

  case object EmailRequestDataNotFound extends NotificationFailure

  case object EmailServiceFailure extends NotificationFailure

  case object AutoClaimEnrolmentFailure extends NotificationFailure

}