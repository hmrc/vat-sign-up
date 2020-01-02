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

import cats.implicits._
import javax.inject.{Inject, Singleton}
import play.api.mvc.Request
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.vatsignup.config.EligibilityConfig
import uk.gov.hmrc.vatsignup.models.controllist._
import uk.gov.hmrc.vatsignup.models.monitoring.ControlListAuditing.ControlListAuditModel
import uk.gov.hmrc.vatsignup.models.{DateRange, MigratableDates}
import uk.gov.hmrc.vatsignup.services.MigrationCheckService._
import uk.gov.hmrc.vatsignup.services.monitoring.AuditService
import uk.gov.hmrc.vatsignup.utils.CurrentDateProvider

import scala.concurrent.ExecutionContext

@Singleton
class MigrationCheckService @Inject()(eligibilityConfig: EligibilityConfig,
                                      currentDateProvider: CurrentDateProvider,
                                      auditService: AuditService
                                     )(implicit ec: ExecutionContext) {


  private def checkMigrationDate(stagger: Stagger, parameters: Map[Stagger, Set[DateRange]]): MigrationEligibility = {

    val sortedDateRanges = parameters.getOrElse(stagger, Set.empty).toList.sortBy(_.startDate.toEpochDay)

    sortedDateRanges.zipWithIndex find {
      case (dateRange, _) => dateRange contains currentDateProvider.getCurrentDate()
    } match {
      case Some((DateRange(_, endDate), index)) =>
        val nextEligibilityDate = endDate plusDays 1

        sortedDateRanges.lift(index + 1) match {
          case Some(DateRange(nextStartDate, _)) =>
            Left(MigratableDates(Some(nextEligibilityDate), Some(nextStartDate minusDays 1)))
          case None =>
            Left(MigratableDates(Some(nextEligibilityDate), None))
        }
      case None =>
        Right(MigrationCheckService.Eligible)
    }
  }

  private def checkDirectDebitMigrationRestriction(vatNumber: String,
                                                   stagger: Stagger,
                                                   isDirectDebit: Boolean,
                                                   isMigratable: Boolean
                                                  )(implicit hc: HeaderCarrier,
                                                    request: Request[_]): MigrationEligibility = {

    if ((isDirectDebit && isMigratable) && stagger != NonStandardTaxPeriod)
      checkMigrationDate(stagger, eligibilityConfig.directDebitStaggerParameters) match {
        case Left(migratableDates) =>
          auditService.audit(ControlListAuditModel.directDebitMigrationRestriction(vatNumber))

          Left(migratableDates)
        case _ =>
          Right(Eligible)
      }
    else Right(Eligible)
  }

  private def checkFilingDateMigrationRestriction(vatNumber: String,
                                                  stagger: Stagger
                                                 )(implicit hc: HeaderCarrier,
                                                   request: Request[_]): MigrationEligibility = {

    checkMigrationDate(stagger, eligibilityConfig.filingDateStaggerParameters) match {
      case Left(migratableDates) =>
        auditService.audit(ControlListAuditModel.filingDateMigrationRestriction(vatNumber))

        Left(migratableDates)
      case _ =>
        Right(Eligible)
    }
  }

  def checkMigrationRestrictions(vatNumber: String,
                                 stagger: Stagger,
                                 isDirectDebit: Boolean,
                                 isMigratable: Boolean
                                )(implicit hc: HeaderCarrier,
                                  request: Request[_]): MigrationEligibility = {

    for {
      _ <- checkDirectDebitMigrationRestriction(vatNumber, stagger, isDirectDebit, isMigratable)
      _ <- checkFilingDateMigrationRestriction(vatNumber, stagger)
    } yield Eligible
  }

}


object MigrationCheckService {
  type MigrationEligibility = Either[MigratableDates, Eligible.type]

  case object Eligible

}
