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

import javax.inject.Inject
import play.api.mvc.Request
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.vatsignup.config.EligibilityConfig
import uk.gov.hmrc.vatsignup.connectors.KnownFactsAndControlListInformationConnector
import uk.gov.hmrc.vatsignup.httpparsers.KnownFactsAndControlListInformationHttpParser._
import uk.gov.hmrc.vatsignup.models.MigratableDates
import uk.gov.hmrc.vatsignup.models.controllist.{ControlListInformation, DirectDebit, NonStandardTaxPeriod}
import uk.gov.hmrc.vatsignup.models.controllist.ControlListInformation._
import uk.gov.hmrc.vatsignup.models.monitoring.ControlListAuditing.ControlListAuditModel
import uk.gov.hmrc.vatsignup.services.ControlListEligibilityService._
import uk.gov.hmrc.vatsignup.services.monitoring.AuditService

import scala.concurrent.{ExecutionContext, Future}

class ControlListEligibilityService @Inject()(knownFactsAndControlListInformationConnector: KnownFactsAndControlListInformationConnector,
                                              eligibilityConfig: EligibilityConfig,
                                              directDebitMigrationCheckService: DirectDebitMigrationCheckService,
                                              auditService: AuditService)(implicit ec: ExecutionContext) {
  def getEligibilityStatus(vatNumber: String
                          )(implicit hc: HeaderCarrier, request: Request[_]): Future[Eligibility] = {
    knownFactsAndControlListInformationConnector.getKnownFactsAndControlListInformation(vatNumber) map {
      case Right(KnownFactsAndControlListInformation(businessPostcode, vatRegistrationDate, controlList@ControlListInformation(parameters, stagger, _))) =>
        controlList.validate(eligibilityConfig) match {
          case Right(Migratable) if (parameters contains DirectDebit) && stagger != NonStandardTaxPeriod =>
            directDebitMigrationCheckService.checkMigrationDate(stagger) match {
              case Right(DirectDebitMigrationCheckService.Eligible) =>
                auditService.audit(ControlListAuditModel.fromEligibilityState(vatNumber, Migratable))

                Right(EligibilitySuccess(
                  businessPostcode = businessPostcode,
                  vatRegistrationDate = vatRegistrationDate,
                  isMigratable = true
                ))
              case Left(migratableDates) =>
                auditService.audit(ControlListAuditModel.directDebitMigrationRestriction(vatNumber))

                Left(IneligibleVatNumber(migratableDates))
            }
          case Right(eligibilityState) =>
            auditService.audit(ControlListAuditModel.fromEligibilityState(vatNumber, eligibilityState))

            Right(EligibilitySuccess(
              businessPostcode = businessPostcode,
              vatRegistrationDate = vatRegistrationDate,
              isMigratable = eligibilityState == Migratable
            ))
          case Left(eligibilityState) =>
            auditService.audit(ControlListAuditModel.fromEligibilityState(vatNumber, eligibilityState))

            Left(IneligibleVatNumber(MigratableDates.empty))
        }
      case Left(errorReason) =>
        auditService.audit(ControlListAuditModel.fromFailure(vatNumber, errorReason))

        errorReason match {
          case KnownFactsInvalidVatNumber => Left(InvalidVatNumber)
          case ControlListInformationVatNumberNotFound => Left(VatNumberNotFound)
          case _ => Left(KnownFactsAndControlListFailure)
        }
    }
  }
}

object ControlListEligibilityService {

  type Eligibility = Either[EligibilityFailure, EligibilitySuccess]

  case class EligibilitySuccess(businessPostcode: String, vatRegistrationDate: String, isMigratable: Boolean)

  sealed trait EligibilityFailure

  case object InvalidVatNumber extends EligibilityFailure

  case class IneligibleVatNumber(migratableDates: MigratableDates) extends EligibilityFailure

  case object VatNumberNotFound extends EligibilityFailure

  case object KnownFactsAndControlListFailure extends EligibilityFailure

}
