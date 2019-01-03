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

import cats.data._
import cats.implicits._
import javax.inject.{Inject, Singleton}
import play.api.mvc.Request
import uk.gov.hmrc.auth.core.Enrolments
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.vatsignup.config.AppConfig
import uk.gov.hmrc.vatsignup.connectors.{AgentClientRelationshipsConnector, MandationStatusConnector}
import uk.gov.hmrc.vatsignup.httpparsers.GetMandationStatusHttpParser.{MigrationInProgress, VatNumberNotFound}
import uk.gov.hmrc.vatsignup.models._
import uk.gov.hmrc.vatsignup.models.monitoring.AgentClientRelationshipAuditing.AgentClientRelationshipAuditModel
import uk.gov.hmrc.vatsignup.models.monitoring.KnownFactsAuditing.KnownFactsAuditModel
import uk.gov.hmrc.vatsignup.repositories.SubscriptionRequestRepository
import uk.gov.hmrc.vatsignup.services.ControlListEligibilityService.EligibilitySuccess
import uk.gov.hmrc.vatsignup.services.StoreVatNumberService._
import uk.gov.hmrc.vatsignup.services.monitoring.AuditService
import uk.gov.hmrc.vatsignup.utils.EnrolmentUtils._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class StoreVatNumberService @Inject()(subscriptionRequestRepository: SubscriptionRequestRepository,
                                      agentClientRelationshipsConnector: AgentClientRelationshipsConnector,
                                      mandationStatusConnector: MandationStatusConnector,
                                      controlListEligibilityService: ControlListEligibilityService,
                                      auditService: AuditService,
                                      appConfig: AppConfig
                                     )(implicit ec: ExecutionContext) {

  def storeVatNumber(vatNumber: String,
                     enrolments: Enrolments,
                     businessPostcode: Option[String],
                     vatRegistrationDate: Option[String]
                    )(implicit hc: HeaderCarrier, request: Request[_]): Future[Either[StoreVatNumberFailure, StoreVatNumberSuccess.type]] = {
    for {
      _ <- checkUserAuthority(vatNumber, enrolments, businessPostcode, vatRegistrationDate)
      _ <- checkExistingVatSubscription(vatNumber, enrolments, businessPostcode, vatRegistrationDate)
      eligibilitySuccess <- checkEligibility(vatNumber, businessPostcode, vatRegistrationDate)
      _ <- insertVatNumber(vatNumber, eligibilitySuccess.isMigratable)
    } yield StoreVatNumberSuccess
  }.value

  private def checkUserAuthority(vatNumber: String,
                                 enrolments: Enrolments,
                                 businessPostcode: Option[String],
                                 vatRegistrationDate: Option[String]
                                )(implicit request: Request[_], hc: HeaderCarrier): EitherT[Future, StoreVatNumberFailure, Any] = {
    EitherT((enrolments.vatNumber, enrolments.agentReferenceNumber) match {
      case (Some(vatNumberFromEnrolment), _) =>
        if (vatNumber == vatNumberFromEnrolment) Future.successful(Right(UserHasMatchingEnrolment))
        else Future.successful(Left(DoesNotMatchEnrolment))
      case (_, None) if businessPostcode.isDefined && vatRegistrationDate.isDefined =>
        Future.successful(Right(UserHasKnownFacts))
      case (_, Some(agentReferenceNumber)) =>
        checkAgentClientRelationship(vatNumber, agentReferenceNumber)
      case _ =>
        Future.successful(Left(InsufficientEnrolments))
    })
  }

  private def checkAgentClientRelationship(vatNumber: String,
                                           agentReferenceNumber: String
                                          )(implicit hc: HeaderCarrier,
                                            request: Request[_]) = {
    agentClientRelationshipsConnector.checkAgentClientRelationship(agentReferenceNumber, vatNumber) map {
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
                               optBusinessPostcode: Option[String],
                               optVatRegistrationDate: Option[String]
                              )(implicit hc: HeaderCarrier, request: Request[_]): EitherT[Future, StoreVatNumberFailure, EligibilitySuccess] = {
    EitherT(controlListEligibilityService.getEligibilityStatus(vatNumber)) transform {
      case Right(success@EligibilitySuccess(businessPostcode, vatRegistrationDate, _)) =>
        (optBusinessPostcode, optVatRegistrationDate) match {
          case (Some(userBusinessPostcode), Some(userVatRegistrationDate)) =>
            val knownFactsMatched =
              (userBusinessPostcode filterNot (_.isWhitespace)).equalsIgnoreCase(businessPostcode filterNot (_.isWhitespace)) &&
                (userVatRegistrationDate == vatRegistrationDate)
            auditService.audit(KnownFactsAuditModel(
              vatNumber = vatNumber,
              enteredPostCode = userBusinessPostcode,
              enteredVatRegistrationDate = userVatRegistrationDate,
              desPostCode = businessPostcode,
              desVatRegistrationDate = vatRegistrationDate,
              matched = knownFactsMatched
            ))
            if (knownFactsMatched) Right[StoreVatNumberFailure, EligibilitySuccess](success)
            else Left[StoreVatNumberFailure, EligibilitySuccess](KnownFactsMismatch)
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

  private def checkExistingVatSubscription(vatNumber: String,
                                           enrolments: Enrolments,
                                           businessPostcode: Option[String],
                                           vatRegistrationDate: Option[String]
                                          )(implicit hc: HeaderCarrier, request: Request[_]): EitherT[Future, StoreVatNumberFailure, NotSubscribed.type] =
    EitherT(mandationStatusConnector.getMandationStatus(vatNumber) flatMap {
      case Right(NonMTDfB | NonDigital) | Left(VatNumberNotFound) =>
        Future.successful(Right(NotSubscribed))
      case Right(MTDfBMandated | MTDfBVoluntary) =>
        Future.successful(Left(AlreadySubscribed))
      case Left(MigrationInProgress) =>
        Future.successful(Left(VatMigrationInProgress))
      case _ =>
        Future.successful(Left(VatSubscriptionConnectionFailure))
    })

  private def insertVatNumber(vatNumber: String,
                              isMigratable: Boolean
                             )(implicit hc: HeaderCarrier): EitherT[Future, StoreVatNumberFailure, StoreVatNumberSuccess.type] =
    EitherT(subscriptionRequestRepository.upsertVatNumber(vatNumber, isMigratable) map {
      _ => Right(StoreVatNumberSuccess)
    } recover {
      case _ => Left(VatNumberDatabaseFailure)
    })
}

object StoreVatNumberService {

  case object StoreVatNumberSuccess

  case object NotSubscribed

  case object UserHasMatchingEnrolment

  case object UserHasKnownFacts

  sealed trait StoreVatNumberFailure

  case object AlreadySubscribed extends StoreVatNumberFailure

  case object DoesNotMatchEnrolment extends StoreVatNumberFailure

  case object InsufficientEnrolments extends StoreVatNumberFailure

  case object KnownFactsMismatch extends StoreVatNumberFailure

  case class Ineligible(migratableDates: MigratableDates) extends StoreVatNumberFailure

  case object VatNotFound extends StoreVatNumberFailure

  case object VatInvalid extends StoreVatNumberFailure

  case object RelationshipNotFound extends StoreVatNumberFailure

  case object KnownFactsAndControlListInformationConnectionFailure extends StoreVatNumberFailure

  case object AgentServicesConnectionFailure extends StoreVatNumberFailure

  case object VatSubscriptionConnectionFailure extends StoreVatNumberFailure

  case object VatNumberDatabaseFailure extends StoreVatNumberFailure

  case object VatMigrationInProgress extends StoreVatNumberFailure

}


