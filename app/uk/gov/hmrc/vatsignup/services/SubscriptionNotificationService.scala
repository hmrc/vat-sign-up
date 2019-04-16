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
import reactivemongo.api.commands.WriteResult
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.vatsignup.config.AppConfig
import uk.gov.hmrc.vatsignup.connectors.{EmailConnector, EnrolmentStoreProxyConnector}
import uk.gov.hmrc.vatsignup.models.SubscriptionState._
import uk.gov.hmrc.vatsignup.models.{EmailRequest, SubscriptionState}
import uk.gov.hmrc.vatsignup.repositories.EmailRequestRepository
import uk.gov.hmrc.vatsignup.services.SubscriptionNotificationService._
import uk.gov.hmrc.vatsignup.httpparsers.EnrolmentStoreProxyHttpParser.EnrolmentAlreadyAllocated


import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SubscriptionNotificationService @Inject()(emailRequestRepository: EmailRequestRepository,
                                                emailConnector: EmailConnector,
                                                enrolmentStoreProxyConnector: EnrolmentStoreProxyConnector,
                                                appConfig: AppConfig
                                               )(implicit ec: ExecutionContext) {
  def sendEmailNotification(vatNumber: String,
                            subscriptionState: SubscriptionState
                           )(implicit hc: HeaderCarrier): Future[Either[NotificationFailure, NotificationSuccess]] = {
    (for {
      emailRequest <- getEmailRequest(vatNumber)
      notificationResult <- sendEmail(emailRequest.email, vatNumber, subscriptionState, emailRequest.isDelegated)
      _ <- deleteEmailRequest(vatNumber)
    } yield notificationResult).value
  }


  private def getEmailRequest(vatNumber: String): EitherT[Future, NotificationFailure, EmailRequest] =
    EitherT.fromOptionF(emailRequestRepository.findById(vatNumber), EmailRequestDataNotFound)

  private def deleteEmailRequest(vatNumber: String): EitherT[Future, NotificationFailure, WriteResult] =
    EitherT.liftF(emailRequestRepository.removeById(vatNumber))


  private def sendEmail(emailAddress: String,
                        vatNumber: String,
                        subscriptionState: SubscriptionState,
                        isDelegated: Boolean
                       )(implicit hc: HeaderCarrier): EitherT[Future, NotificationFailure, NotificationSuccess] = {
    if (isDelegated) {
      EitherT(emailConnector.sendEmail(emailAddress, agentSuccessEmailTemplate, Some(vatNumber))) bimap(
        _ => EmailServiceFailure,
        _ => DelegatedSubscription
      )
    } else {
      subscriptionState match {
        case Success => EitherT(emailConnector.sendEmail(emailAddress, principalSuccessEmailTemplate, None)) bimap(
          _ => EmailServiceFailure,
          _ => NotificationSent
        )
        case _ => EitherT[Future, NotificationFailure, NotificationSuccess](
          enrolmentStoreProxyConnector.getAllocatedEnrolments(vatNumber).flatMap {
            case Right(EnrolmentAlreadyAllocated) => emailConnector.sendEmail(emailAddress, principalSuccessEmailTemplate, None).map {
              case Right(_) => Right(NotificationSent)
              case Left(_) => Left(EmailServiceFailure)
            }
            case _ =>
              Logger.error(s"Tax Enrolment Failure vrn=$vatNumber")
              Future.successful(Right(TaxEnrolmentFailure))
          }
        )
      }
    }
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

  sealed trait NotificationFailure

  case object EmailRequestDataNotFound extends NotificationFailure

  case object EmailServiceFailure extends NotificationFailure

}