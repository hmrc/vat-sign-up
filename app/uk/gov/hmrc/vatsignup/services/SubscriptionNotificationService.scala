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

import cats.data.EitherT
import cats.implicits._
import javax.inject.{Inject, Singleton}
import reactivemongo.api.commands.WriteResult
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.vatsignup.connectors.EmailConnector
import uk.gov.hmrc.vatsignup.httpparsers.SendEmailHttpParser
import uk.gov.hmrc.vatsignup.models.SubscriptionState._
import uk.gov.hmrc.vatsignup.models.{EmailRequest, SubscriptionState}
import uk.gov.hmrc.vatsignup.repositories.EmailRequestRepository
import uk.gov.hmrc.vatsignup.services.SubscriptionNotificationService._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SubscriptionNotificationService @Inject()(emailRequestRepository: EmailRequestRepository,
                                                emailConnector: EmailConnector
                                               )(implicit ec: ExecutionContext) {
  def sendEmailNotification(vatNumber: String,
                            subscriptionState: SubscriptionState
                           )(implicit hc: HeaderCarrier): Future[Either[NotificationFailure, NotificationSuccess]] = {
    for {
      emailRequest <- getEmailRequest(vatNumber)
      notificationResult <- sendEmail(emailRequest.email, subscriptionState, emailRequest.isDelegated)
      _ <- deleteEmailRequest(vatNumber)
    } yield notificationResult
  }.value

  private def getEmailRequest(vatNumber: String
                             ): EitherT[Future, NotificationFailure, EmailRequest] =
    EitherT.fromOptionF(emailRequestRepository.findById(vatNumber), EmailRequestDataNotFound)

  private def deleteEmailRequest(vatNumber: String): EitherT[Future, NotificationFailure, WriteResult] =
    EitherT.liftF(emailRequestRepository.removeById(vatNumber))


  private def sendEmail(emailAddress: String,
                        subscriptionState: SubscriptionState,
                        isDelegated: Boolean
                       )(implicit hc: HeaderCarrier): EitherT[Future, NotificationFailure, NotificationSuccess] = {
    if (isDelegated) {
      EitherT.rightT(DelegatedSubscription)
    } else {
      val emailTemplate = subscriptionState match {
        case Success => principalSuccessEmailTemplate
        case Failure => principalFailureEmailTemplate
      }

      EitherT(emailConnector.sendEmail(emailAddress, emailTemplate)) bimap(
        _ => EmailServiceFailure,
        _ => NotificationSent
      )
    }
  }
}

object SubscriptionNotificationService {
  val principalSuccessEmailTemplate = "mtdfb_vat_principal_sign_up_successful"
  val principalFailureEmailTemplate = "mtdfb_vat_principal_sign_up_failure"

  sealed trait NotificationSuccess

  case object NotificationSent extends NotificationSuccess

  case object DelegatedSubscription extends NotificationSuccess

  sealed trait NotificationFailure

  case object EmailRequestDataNotFound extends NotificationFailure

  case object EmailServiceFailure extends NotificationFailure

}