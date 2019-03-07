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
import javax.inject.{Inject, Singleton}
import play.api.mvc.Request
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.vatsignup.config.EligibilityConfig
import uk.gov.hmrc.vatsignup.connectors.KnownFactsAndControlListInformationConnector
import uk.gov.hmrc.vatsignup.httpparsers.KnownFactsAndControlListInformationHttpParser._
import uk.gov.hmrc.vatsignup.models.{KnownFactsAndControlListInformation, MigratableDates, VatKnownFacts}
import uk.gov.hmrc.vatsignup.models.controllist.{ControlListInformation, DirectDebit, NonStandardTaxPeriod, OverseasTrader}
import uk.gov.hmrc.vatsignup.models.controllist.ControlListInformation._
import uk.gov.hmrc.vatsignup.models.monitoring.ControlListAuditing.ControlListAuditModel
import uk.gov.hmrc.vatsignup.services.ControlListEligibilityService._
import uk.gov.hmrc.vatsignup.services.monitoring.AuditService
import cats.implicits._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ControlListEligibilityService @Inject()(knownFactsAndControlListInformationConnector: KnownFactsAndControlListInformationConnector,
                                              eligibilityConfig: EligibilityConfig,
                                              directDebitMigrationCheckService: DirectDebitMigrationCheckService,
                                              auditService: AuditService)(implicit ec: ExecutionContext) {
  def getEligibilityStatus(vatNumber: String
                          )(implicit hc: HeaderCarrier, request: Request[_]): Future[Eligibility] = {
    for {
      knownFactsAndControlListInformation <- getKnownFactsAndControlListInformation(vatNumber)
      controlListInformation = knownFactsAndControlListInformation.controlListInformation
      migratableStatus <- checkControlListEligibility(vatNumber, controlListInformation)
      _ <- checkDirectDebitMigrationRestrictions(vatNumber, controlListInformation, migratableStatus)
    } yield {
      auditService.audit(ControlListAuditModel.fromEligibilityState(vatNumber, migratableStatus))

      EligibilitySuccess(
        vatKnownFacts = knownFactsAndControlListInformation.vatKnownFacts,
        isMigratable = migratableStatus == Migratable,
        isOverseas = controlListInformation.controlList contains OverseasTrader,
        isDirectDebit = controlListInformation.controlList contains DirectDebit
      )
    }
  }.value

  private def getKnownFactsAndControlListInformation(vatNumber: String
                                                    )(implicit hc: HeaderCarrier,
                                                      request: Request[_]): EitherT[Future, EligibilityFailure, KnownFactsAndControlListInformation] =
    EitherT(knownFactsAndControlListInformationConnector.getKnownFactsAndControlListInformation(vatNumber)) leftMap {
      errorReason =>
        auditService.audit(ControlListAuditModel.fromFailure(vatNumber, errorReason))

        errorReason match {
          case KnownFactsInvalidVatNumber => InvalidVatNumber
          case ControlListInformationVatNumberNotFound => VatNumberNotFound
          case _ => KnownFactsAndControlListFailure
        }
    }

  private def checkControlListEligibility(vatNumber: String,
                                          controlListInformation: ControlListInformation
                                         )(implicit hc: HeaderCarrier,
                                           request: Request[_]): EitherT[Future, EligibilityFailure, Eligible] =
    EitherT.fromEither[Future](controlListInformation.validate(eligibilityConfig)) leftMap {
      eligibilityState =>
        auditService.audit(ControlListAuditModel.fromEligibilityState(vatNumber, eligibilityState))

        IneligibleVatNumber(MigratableDates.empty)
    }

  private def checkDirectDebitMigrationRestrictions(vatNumber: String,
                                                    controlListInformation: ControlListInformation,
                                                    migratableStatus: ControlListInformation.Eligible
                                                   )(implicit hc: HeaderCarrier,
                                                     request: Request[_]): EitherT[Future, EligibilityFailure, DirectDebitMigrationCheckService.Eligible.type] = {
    val ControlListInformation(parameters, stagger, _) = controlListInformation

    migratableStatus match {
      case Migratable if (parameters contains DirectDebit) && stagger != NonStandardTaxPeriod =>
        EitherT.fromEither[Future](directDebitMigrationCheckService.checkMigrationDate(stagger)) leftMap {
          migratableDates =>
            auditService.audit(ControlListAuditModel.directDebitMigrationRestriction(vatNumber))

            IneligibleVatNumber(migratableDates)
        }
      case _ => EitherT.rightT(DirectDebitMigrationCheckService.Eligible)

    }
  }
}

object ControlListEligibilityService {

  type Eligibility = Either[EligibilityFailure, EligibilitySuccess]

  case class EligibilitySuccess(vatKnownFacts: VatKnownFacts,
                                isMigratable: Boolean,
                                isOverseas: Boolean,
                                isDirectDebit: Boolean)

  sealed trait EligibilityFailure

  case object InvalidVatNumber extends EligibilityFailure

  case class IneligibleVatNumber(migratableDates: MigratableDates) extends EligibilityFailure

  case object VatNumberNotFound extends EligibilityFailure

  case object KnownFactsAndControlListFailure extends EligibilityFailure

}
