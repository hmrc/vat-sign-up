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

import javax.inject.{Inject, Singleton}

import cats.data.EitherT
import cats.implicits._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.vatsignup.connectors.EmailVerificationConnector
import uk.gov.hmrc.vatsignup.httpparsers.GetEmailVerificationStateHttpParser.{EmailNotVerified, EmailVerified}
import uk.gov.hmrc.vatsignup.services.EmailRequirementService._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EmailRequirementService @Inject()(emailVerificationConnector: EmailVerificationConnector)
                                       (implicit ec: ExecutionContext) {


  def checkRequirements(optPrincipalEmail: Option[String], optTransactionEmail: Option[String], isDelegated: Boolean)(implicit hc: HeaderCarrier):
  Future[Either[InsufficientEmailRequirement, Email]] = {

    lazy val checkPrincipal = optPrincipalEmail match {
      case Some(email) => {
        isVerified(email)
      }
      case None => Future.successful(Left(EmailNotSupplied))
    }


    lazy val checkAgent = optTransactionEmail match {
      case Some(email) => {
        isVerified(email)
      }
      case None => optPrincipalEmail match {
        case Some(email) => {
          val result = for {
            emailVerified <- isEmailAddressVerified(email)
          } yield Email(email, emailVerified)
          result.value
        }
        case None => Future.successful(Left(EmailNotSupplied))
      }
    }

    if (isDelegated) checkAgent else checkPrincipal
  }

  private def isVerified(emailAddress: String)(implicit hc: HeaderCarrier): Future[Either[InsufficientEmailRequirement, Email]] = {

    val result = for {
      emailVerified <- isEmailAddressVerified(emailAddress)
      emailOrchestrationResponse <- {
        EitherT.fromEither(if (emailVerified) Right(Email(emailAddress, emailVerified))
        else Left(UnVerifiedEmail)): EitherT[Future, InsufficientEmailRequirement, Email]
      }
    } yield emailOrchestrationResponse
    result.value
  }

  private def isEmailAddressVerified(emailAddress: String
                                    )(implicit hc: HeaderCarrier): EitherT[Future, InsufficientEmailRequirement, Boolean] =
    EitherT(emailVerificationConnector.getEmailVerificationState(emailAddress)) bimap( {
      _ => GetEmailVerificationFailure
    }, {
      case EmailVerified => true
      case EmailNotVerified => false
    })

}

object EmailRequirementService {

  case class Email(address: String, isVerified: Boolean)

  trait InsufficientEmailRequirement

  case object EmailNotSupplied extends InsufficientEmailRequirement

  case object GetEmailVerificationFailure extends InsufficientEmailRequirement

  case object UnVerifiedEmail extends InsufficientEmailRequirement

}


