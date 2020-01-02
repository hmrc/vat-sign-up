/*
 * Copyright 2020 HM Revenue & Customs
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
import play.api.mvc.Request
import uk.gov.hmrc.auth.core.Enrolments
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.vatsignup.models.VatKnownFacts
import uk.gov.hmrc.vatsignup.repositories.SubscriptionRequestRepository
import uk.gov.hmrc.vatsignup.services.AgentClientRelationshipService._
import uk.gov.hmrc.vatsignup.services.StoreMigratedVRNService._
import uk.gov.hmrc.vatsignup.utils.EnrolmentUtils._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class StoreMigratedVRNService @Inject()(subscriptionRequestRepository: SubscriptionRequestRepository,
                                        migratedKnownFactsMatchingService: MigratedKnownFactsMatchingService,
                                        agentClientRelationshipService: AgentClientRelationshipService
                                       )(implicit ec: ExecutionContext) {

  def storeVatNumber(vatNumber: String,
                     enrolments: Enrolments,
                     optKnownFacts: Option[VatKnownFacts] = None)
                    (implicit hc: HeaderCarrier,
                     request: Request[_]
                    ): Future[Either[StoreMigratedVRNFailure, StoreMigratedVRNSuccess.type]] =
    (for {
      _ <- optKnownFacts match {
        case Some(knownFacts) => EitherT(checkKnownFacts(knownFacts, vatNumber))
        case _ => EitherT(checkUserAuthority(enrolments, vatNumber))
      }
      _ <- EitherT(upsertVatNumber(vatNumber))
    } yield StoreMigratedVRNSuccess).value


  private def checkKnownFacts(knownFacts: VatKnownFacts,
                              vatNumber: String)(implicit hc: HeaderCarrier): Future[Either[StoreMigratedVRNFailure, KnownFactsSuccess.type]] =
    migratedKnownFactsMatchingService.checkKnownFactsMatch(vatNumber, knownFacts).map { knownFactsMatch =>
      if (knownFactsMatch) Right(KnownFactsSuccess)
      else Left(KnownFactsMismatch)
    }


  private def upsertVatNumber(vatNumber: String): Future[Either[StoreMigratedVRNFailure, UpsertVatNumberSuccess.type]] =
    subscriptionRequestRepository.upsertVatNumber(vatNumber, isMigratable = true, isDirectDebit = false).map {
      case result if result.ok => Right(UpsertVatNumberSuccess)
      case _ => Left(UpsertMigratedVRNFailure)
    }

  private def checkUserAuthority(enrolments: Enrolments,
                                 vatNumber: String
                                )(implicit hc: HeaderCarrier,
                                  request: Request[_]): Future[Either[StoreMigratedVRNFailure, CheckUserAuthoritySuccess.type]] =
    (enrolments.vatNumber, enrolments.agentReferenceNumber) match {
      case (_, Some(agentReferenceNumber)) =>
        agentClientRelationshipService.checkAgentClientRelationship(vatNumber, agentReferenceNumber).map {
          case Right(RelationshipCheckSuccess) =>
            Right(CheckUserAuthoritySuccess)
          case Left(RelationshipCheckNotFound) =>
            Left(AgentClientRelationshipNotFound)
          case Left(RelationshipCheckError) =>
            Left(AgentClientRelationshipFailure)
        }
      case (Right(vrn), _) =>
        if (vatNumber == vrn)
          Future.successful(Right(CheckUserAuthoritySuccess))
        else
          Future.successful(Left(VatNumberDoesNotMatch))
      case (Left(VatNumberMismatch), _) =>
        Future.successful(Left(VatNumberDoesNotMatch))
      case (Left(NoEnrolment), _) =>
        Future.successful(Left(NoVatEnrolment))
    }

}

object StoreMigratedVRNService {

  case object UpsertVatNumberSuccess

  case object KnownFactsSuccess

  case object CheckUserAuthoritySuccess

  case object StoreMigratedVRNSuccess

  sealed trait StoreMigratedVRNFailure

  case object KnownFactsMismatch extends StoreMigratedVRNFailure

  case object VatNumberDoesNotMatch extends StoreMigratedVRNFailure

  case object NoVatEnrolment extends StoreMigratedVRNFailure

  case object UpsertMigratedVRNFailure extends StoreMigratedVRNFailure

  case object AgentClientRelationshipNotFound extends StoreMigratedVRNFailure

  case object AgentClientRelationshipFailure extends StoreMigratedVRNFailure

}
