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

import cats.data._
import cats.implicits._
import javax.inject.{Inject, Singleton}
import play.api.mvc.Request
import uk.gov.hmrc.auth.core.Enrolments
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.vatsignup.config.AppConfig
import uk.gov.hmrc.vatsignup.connectors.{AgentClientRelationshipConnector, MandationStatusConnector}
import uk.gov.hmrc.vatsignup.httpparsers.GetMandationStatusHttpParser.{MigrationInProgress, VatNumberNotFound}
import uk.gov.hmrc.vatsignup.models._
import uk.gov.hmrc.vatsignup.models.monitoring.AgentClientRelationshipAuditing.AgentClientRelationshipAuditModel
import uk.gov.hmrc.vatsignup.repositories.SubscriptionRequestRepository
import uk.gov.hmrc.vatsignup.services.AgentClientRelationshipService.LegacyRelationship
import uk.gov.hmrc.vatsignup.services.ControlListEligibilityService.EligibilitySuccess
import uk.gov.hmrc.vatsignup.services.StoreVatNumberService.{KnownFactsMismatch, _}
import uk.gov.hmrc.vatsignup.services.monitoring.AuditService
import uk.gov.hmrc.vatsignup.utils.EnrolmentUtils._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class StoreVatNumberService @Inject()(subscriptionRequestRepository: SubscriptionRequestRepository,
                                      agentClientRelationshipConnector: AgentClientRelationshipConnector,
                                      controlListEligibilityService: ControlListEligibilityService,
                                      knownFactsMatchingService: KnownFactsMatchingService,
                                      auditService: AuditService
                                     )(implicit ec: ExecutionContext) {

  def storeVatNumber(vatNumber: String,
                     enrolments: Enrolments,
                     vatKnownFacts: Option[VatKnownFacts]
                    )(implicit hc: HeaderCarrier, request: Request[_]): Future[Either[StoreVatNumberFailure, StoreVatNumberSuccess]] = {
    for {
      _ <- checkUserAuthority(
        vatNumber,
        enrolments,
        vatKnownFacts
      )
      eligibilitySuccess <- checkEligibility(
        vatNumber,
        vatKnownFacts
      )
      _ <- insertVatNumber(
        vatNumber,
        eligibilitySuccess.isMigratable,
        eligibilitySuccess.isDirectDebit
      )
    } yield StoreVatNumberSuccess(
      eligibilitySuccess.isOverseas,
      eligibilitySuccess.isDirectDebit
    )
  }.value

  private def checkUserAuthority(vatNumber: String,
                                 enrolments: Enrolments,
                                 vatKnownFacts: Option[VatKnownFacts]
                                )(implicit request: Request[_], hc: HeaderCarrier): EitherT[Future, StoreVatNumberFailure, Any] = {
    EitherT((enrolments.vatNumber, enrolments.agentReferenceNumber) match {
      case (Right(vatNumberFromEnrolment), _) =>
        if (vatNumber == vatNumberFromEnrolment) Future.successful(Right(UserHasMatchingEnrolment))
        else Future.successful(Left(DoesNotMatchEnrolment))
      case (_, None) if vatKnownFacts.isDefined =>
        Future.successful(Right(UserHasKnownFacts))
      case (_, Some(agentReferenceNumber)) =>
        checkAgentClientRelationship(vatNumber, agentReferenceNumber)
      case _ =>
        Future.successful(Left(InsufficientEnrolments))
    })
  }

  private def checkAgentClientRelationship(vatNumber: String,
                                           agentReferenceNumber: String
                                          )(implicit hc: HeaderCarrier, request: Request[_]) = {
    agentClientRelationshipConnector.checkAgentClientRelationship(agentReferenceNumber, vatNumber, LegacyRelationship) map {
      case Right(HaveRelationshipResponse) =>
        auditService.audit(AgentClientRelationshipAuditModel(vatNumber, agentReferenceNumber, haveRelationship = true))
        Right(HaveRelationshipResponse)
      case Right(NoRelationshipResponse) =>
        auditService.audit(AgentClientRelationshipAuditModel(vatNumber, agentReferenceNumber, haveRelationship = false))
        Left(RelationshipNotFound)
      case _ =>
        Left(AgentServicesConnectionFailure)
    }
  }

  private def checkEligibility(vatNumber: String,
                               optVatKnownFacts: Option[VatKnownFacts]
                              )(implicit hc: HeaderCarrier, request: Request[_]): EitherT[Future, StoreVatNumberFailure, EligibilitySuccess] = {
    EitherT(controlListEligibilityService.getEligibilityStatus(vatNumber)) transform {
      case Right(success@EligibilitySuccess(retrievedVatKnownFacts, _, isOverseas, _)) =>
        optVatKnownFacts match {
          case Some(enteredVatKnownFacts) =>
            knownFactsMatchingService.checkKnownFactsMatch(
              vatNumber = vatNumber,
              enteredKfs = enteredVatKnownFacts,
              retrievedKfs = retrievedVatKnownFacts,
              isOverseas = isOverseas
            ) match {
              case Right(_) =>
                Right(success)
              case Left(_) =>
                Left(KnownFactsMismatch)
            }
          case _ =>
            Right(success)
        }
      case Left(ControlListEligibilityService.IneligibleVatNumber(migratableDates)) =>
        Left(Ineligible(migratableDates))
      case Left(ControlListEligibilityService.InvalidVatNumber) =>
        Left(VatInvalid)
      case Left(ControlListEligibilityService.VatNumberNotFound) =>
        Left(VatNotFound)
      case Left(_) =>
        Left(KnownFactsAndControlListInformationConnectionFailure)
    }
  }

  private def insertVatNumber(vatNumber: String,
                              isMigratable: Boolean,
                              isDirectDebit: Boolean
                             )(implicit hc: HeaderCarrier): EitherT[Future, StoreVatNumberFailure, (StoreVatNumberSuccess.type)] =
    EitherT(subscriptionRequestRepository.upsertVatNumber(vatNumber, isMigratable, isDirectDebit) map {
      _ => Right(StoreVatNumberSuccess)
    } recover {
      case _ => Left(VatNumberDatabaseFailure)
    })
}

object StoreVatNumberService {

  case class StoreVatNumberSuccess(isOverseas: Boolean, isDirectDebit: Boolean)

  case object UserHasMatchingEnrolment

  case object UserHasKnownFacts

  sealed trait StoreVatNumberFailure

  case object DoesNotMatchEnrolment extends StoreVatNumberFailure

  case object InsufficientEnrolments extends StoreVatNumberFailure

  case object KnownFactsMismatch extends StoreVatNumberFailure

  case class Ineligible(migratableDates: MigratableDates) extends StoreVatNumberFailure

  case object VatNotFound extends StoreVatNumberFailure

  case object VatInvalid extends StoreVatNumberFailure

  case object RelationshipNotFound extends StoreVatNumberFailure

  case object KnownFactsAndControlListInformationConnectionFailure extends StoreVatNumberFailure

  case object AgentServicesConnectionFailure extends StoreVatNumberFailure


  case object VatNumberDatabaseFailure extends StoreVatNumberFailure


}

