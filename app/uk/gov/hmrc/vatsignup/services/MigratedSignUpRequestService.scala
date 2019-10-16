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
import uk.gov.hmrc.vatsignup.models._
import uk.gov.hmrc.vatsignup.repositories.SubscriptionRequestRepository
import uk.gov.hmrc.vatsignup.services.MigratedSignUpRequestService._
import uk.gov.hmrc.vatsignup.utils.EnrolmentUtils._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class MigratedSignUpRequestService @Inject()(subscriptionRequestRepository: SubscriptionRequestRepository)
                                            (implicit ec: ExecutionContext) {

  def getSignUpRequest(vatNumber: String, enrolments: Enrolments)
                      (implicit hc: HeaderCarrier): Future[Either[SignUpRequestFailure, MigratedSignUpRequest]] = {

    val isDelegated = enrolments.agentReferenceNumber.isDefined

    subscriptionRequestRepository.findById(vatNumber) flatMap {
      case Some(subscriptionRequest) =>
        val res = for {
          businessEntity <- EitherT.fromOption[Future](subscriptionRequest.businessEntity, InsufficientData)
          _ <- EitherT.fromEither[Future](checkAuthorisation(businessEntity, isDelegated))
          isMigratable = subscriptionRequest.isMigratable
        } yield MigratedSignUpRequest(
          vatNumber = vatNumber,
          businessEntity = businessEntity,
          isDelegated = isDelegated,
          isMigratable = isMigratable
        )
        res.value
      case None =>
        Future.successful(Left(SignUpRequestNotFound))
    } recover {
      case _ => Left(DatabaseFailure)
    }
  }

  def deleteSignUpRequest(vatNumber: String)
                         (implicit hc: HeaderCarrier): Future[Either[SignUpRequestFailure, SignUpRequestDeleted.type]] =

    subscriptionRequestRepository.deleteRecord(vatNumber) map {
      case _ => Right(SignUpRequestDeleted)
    } recover {
      case _ => Left(DeleteRecordFailure)
    }

  private def checkAuthorisation(businessEntity: BusinessEntity,
                                 isDelegated: Boolean): Either[SignUpRequestFailure, RequestAuthorised.type] =
    businessEntity match {
      case _: LimitedCompany =>
        Right(RequestAuthorised)
      case _: SoleTrader =>
        Right(RequestAuthorised)
      case _: PartnershipBusinessEntity =>
        Right(RequestAuthorised)
      case VatGroup | AdministrativeDivision | UnincorporatedAssociation | Trust | Charity | GovernmentOrganisation | Overseas | JointVenture =>
        Right(RequestAuthorised)
      case _: OverseasWithUkEstablishment =>
        Right(RequestAuthorised)
      case _: RegisteredSociety =>
        Right(RequestAuthorised)
      case _ if isDelegated =>
        Right(RequestAuthorised)
      case _ =>
        Left(RequestNotAuthorised)
    }

}

object MigratedSignUpRequestService {

  type MigratedSignUpRequestResponse = Either[SignUpRequestFailure, SubscriptionRequest]

  case object SignUpRequestDeleted

  case object RequestAuthorised

  sealed trait SignUpRequestFailure

  case object SignUpRequestNotFound extends SignUpRequestFailure

  case object DatabaseFailure extends SignUpRequestFailure

  case object InsufficientData extends SignUpRequestFailure

  case object RequestNotAuthorised extends SignUpRequestFailure

  case object DeleteRecordFailure extends SignUpRequestFailure

}
