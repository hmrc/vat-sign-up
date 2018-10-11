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

package uk.gov.hmrc.vatsignup.service

import java.time.LocalDate

import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.vatsignup.config.mocks.MockEligibilityConfig
import uk.gov.hmrc.vatsignup.models.controllist.Stagger1
import uk.gov.hmrc.vatsignup.models.{DateRange, MigratableDates}
import uk.gov.hmrc.vatsignup.services.DirectDebitMigrationCheckService
import uk.gov.hmrc.vatsignup.utils.controllist.mocks.MockCurrentDateProvider

class DirectDebitMigrationCheckServiceSpec extends UnitSpec
  with MockCurrentDateProvider with MockEligibilityConfig {
  case object TestDirectDebitMigrationCheckService extends DirectDebitMigrationCheckService(
    mockEligibilityConfig,
    mockCurrentDateProvider
  )

  "checkMigrationDate" when {
    "the current date and stagger are not conflicting with the restricted dates" should {
      "return Eligible" in {
        val startDate = LocalDate.of(2018, 1, 1)
        val endDate = LocalDate.of(2018, 2, 1)

        val nextStartDate =  LocalDate.of(2018, 3, 1)
        val nextEndDate = LocalDate.of(2018, 4, 1)

        val currentDate = endDate plusDays 1

        mockStaggerParameters(Map(Stagger1 -> Set(
          DateRange(startDate, endDate),
          DateRange(nextStartDate, nextEndDate)
        )))

        mockCurrentDate(currentDate)

        val res = TestDirectDebitMigrationCheckService.checkMigrationDate(Stagger1)

        res shouldBe Right(DirectDebitMigrationCheckService.Eligible)
      }
    }
    "the current date and stagger fall within a restricted date range" when {
      "there is a date for the next restricted period available" should {
        "return MigratableDates with two dates provided" in {
          val startDate = LocalDate.of(2018, 1, 1)
          val endDate = LocalDate.of(2018, 2, 1)

          val nextStartDate =  LocalDate.of(2018, 3, 1)
          val nextEndDate = LocalDate.of(2018, 4, 1)

          val currentDate = LocalDate.of(2018, 1, 1)

          mockStaggerParameters(Map(Stagger1 -> Set(
            DateRange(startDate, endDate),
            DateRange(nextStartDate, nextEndDate)
          )))

          mockCurrentDate(currentDate)

          val res = TestDirectDebitMigrationCheckService.checkMigrationDate(Stagger1)

          res shouldBe Left(MigratableDates(
            Some(endDate plusDays 1),
            Some(nextStartDate minusDays 1)
          ))
        }
      }

      "there is only the eligibility date" should {
        "return MigratableDates with one date provided" in {
          val startDate = LocalDate.of(2018, 1, 1)
          val endDate = LocalDate.of(2018, 2, 1)

          val nextStartDate =  LocalDate.of(2018, 3, 1)
          val nextEndDate = LocalDate.of(2018, 4, 1)

          val currentDate = LocalDate.of(2018, 1, 1)

          mockStaggerParameters(Map(Stagger1 -> Set(
            DateRange(startDate, endDate)
          )))

          mockCurrentDate(currentDate)

          val res = TestDirectDebitMigrationCheckService.checkMigrationDate(Stagger1)

          res shouldBe Left(
            MigratableDates(
              Some(endDate plusDays 1),
              None
            ))
        }
      }
    }
  }
}
