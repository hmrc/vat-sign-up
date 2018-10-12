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
import uk.gov.hmrc.vatsignup.config.EligibilityConfig
import uk.gov.hmrc.vatsignup.models.{DateRange, MigratableDates}
import uk.gov.hmrc.vatsignup.models.controllist.Stagger
import uk.gov.hmrc.vatsignup.services.DirectDebitMigrationCheckService._
import uk.gov.hmrc.vatsignup.utils.CurrentDateProvider

@Singleton
class DirectDebitMigrationCheckService @Inject()(eligibilityConfig: EligibilityConfig,
                                                 currentDateProvider: CurrentDateProvider) {

  def checkMigrationDate(stagger: Stagger): DirectDebitMigrationEligibility = {
    val sortedDateRanges = eligibilityConfig.staggerParameters.getOrElse(stagger, Set.empty)
      .toList.sortBy(_.startDate.toEpochDay)

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
        Right(Eligible)
    }

  }
}

object DirectDebitMigrationCheckService {
  type DirectDebitMigrationEligibility = Either[MigratableDates, Eligible.type]

  case object Eligible

}
