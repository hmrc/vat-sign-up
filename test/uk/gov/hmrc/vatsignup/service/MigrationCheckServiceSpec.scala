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

package uk.gov.hmrc.vatsignup.service

import java.time.LocalDate

import play.api.mvc.{AnyContent, Request}
import play.api.test.FakeRequest
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.vatsignup.config.mocks.MockEligibilityConfig
import uk.gov.hmrc.vatsignup.helpers.TestConstants._
import uk.gov.hmrc.vatsignup.models.controllist._
import uk.gov.hmrc.vatsignup.models.monitoring.ControlListAuditing
import uk.gov.hmrc.vatsignup.models.monitoring.ControlListAuditing.ControlListAuditModel
import uk.gov.hmrc.vatsignup.models.{DateRange, MigratableDates}
import uk.gov.hmrc.vatsignup.service.mocks.monitoring.MockAuditService
import uk.gov.hmrc.vatsignup.services.MigrationCheckService
import uk.gov.hmrc.vatsignup.services.MigrationCheckService.Eligible
import uk.gov.hmrc.vatsignup.utils.controllist.mocks.MockCurrentDateProvider

import scala.concurrent.ExecutionContext.Implicits.global


class MigrationCheckServiceSpec extends UnitSpec
  with MockCurrentDateProvider with MockEligibilityConfig with MockAuditService {

  case object TestMigrationCheckService extends MigrationCheckService(
    mockEligibilityConfig,
    mockCurrentDateProvider,
    mockAuditService
  )

  implicit val request: Request[AnyContent] = FakeRequest()
  implicit val hc: HeaderCarrier = HeaderCarrier()

  "checkMigrationRestrictions" when {
    "the current date and stagger are not conflicting with the restricted direct debit dates and filing dates" should {
      "return Eligible" in {
        val startDate = LocalDate.of(2018, 1, 1)
        val endDate = LocalDate.of(2018, 2, 1)

        val nextStartDate = LocalDate.of(2018, 3, 1)
        val nextEndDate = LocalDate.of(2018, 4, 1)

        val currentDate = endDate plusDays 1

        mockDirectDebitStaggerParameters(Map(Stagger1 -> Set(
          DateRange(startDate, endDate),
          DateRange(nextStartDate, nextEndDate)
        )))

        mockFilingDateStaggerParameters(Map(Stagger1 -> Set.empty))
        // TODO will need to change this to reflect the filing dates when available

        mockCurrentDate(currentDate)

        val res = TestMigrationCheckService.checkMigrationRestrictions(
          vatNumber = testVatNumber,
          stagger = Stagger1,
          isDirectDebit = true,
          isMigratable = true
        )

        res shouldBe Right(Eligible)
      }
    }
    "the current date and stagger fall within the direct debit restricted date range" when {
      "the user is migratable and there is a date for the next restricted period available" should {
        "return MigratableDates with two dates provided" in {
          val startDate = LocalDate.of(2018, 1, 1)
          val endDate = LocalDate.of(2018, 2, 1)

          val nextStartDate = LocalDate.of(2018, 3, 1)
          val nextEndDate = LocalDate.of(2018, 4, 1)

          val currentDate = LocalDate.of(2018, 1, 1)

          mockDirectDebitStaggerParameters(Map(Stagger1 -> Set(
            DateRange(startDate, endDate),
            DateRange(nextStartDate, nextEndDate)
          )))

          mockCurrentDate(currentDate)

          val res = TestMigrationCheckService.checkMigrationRestrictions(
            vatNumber = testVatNumber,
            stagger = Stagger1,
            isDirectDebit = true,
            isMigratable = true
          )

          res shouldBe Left(MigratableDates(
            Some(endDate plusDays 1),
            Some(nextStartDate minusDays 1)
          ))

          verifyAudit(ControlListAuditModel(
            testVatNumber,
            isSuccess = false,
            failureReasons = Seq(ControlListAuditing.directDebitMigrationRestrictionMessage)
          ))

        }
      }

      "the user is migratable and there is only the eligibility date" should {
        "return MigratableDates with one date provided" in {
          val startDate = LocalDate.of(2018, 1, 1)
          val endDate = LocalDate.of(2018, 2, 1)

          val nextStartDate = LocalDate.of(2018, 3, 1)
          val nextEndDate = LocalDate.of(2018, 4, 1)

          val currentDate = LocalDate.of(2018, 1, 1)

          mockDirectDebitStaggerParameters(Map(Stagger1 -> Set(
            DateRange(startDate, endDate)
          )))

          mockCurrentDate(currentDate)

          val res = TestMigrationCheckService.checkMigrationRestrictions(
            vatNumber = testVatNumber,
            stagger = Stagger1,
            isDirectDebit = true,
            isMigratable = true
          )

          res shouldBe Left(MigratableDates(
            Some(endDate plusDays 1),
            None
          ))

          verifyAudit(ControlListAuditModel(
            testVatNumber,
            isSuccess = false,
            failureReasons = Seq(ControlListAuditing.directDebitMigrationRestrictionMessage)
          ))
        }
      }
      "the user is non-migratable and within the direct debit restriction period" should {
        "return Eligible" in {
          val startDate = LocalDate.of(2018, 1, 1)
          val endDate = LocalDate.of(2018, 2, 1)

          val nextStartDate = LocalDate.of(2018, 3, 1)
          val nextEndDate = LocalDate.of(2018, 4, 1)

          val currentDate = LocalDate.of(2018, 1, 1)

          mockNonMigratableParameters(Set(DeRegOrDeath))

          mockDirectDebitStaggerParameters(Map(Stagger1 -> Set(
            DateRange(startDate, endDate),
            DateRange(nextStartDate, nextEndDate)
          )))

          mockCurrentDate(currentDate)

          mockFilingDateStaggerParameters(Map(Stagger1 -> Set.empty))
          // TODO will need to change this to reflect the filing dates when available

          val res = TestMigrationCheckService.checkMigrationRestrictions(
            vatNumber = testVatNumber,
            stagger = Stagger1,
            isDirectDebit = true,
            isMigratable = false
          )

          res shouldBe Right(Eligible)
        }
      }
    }

    "the current date and stagger fall within the filing dates" should {
      "return Left(MigratableDates)" in {
        val startDate = LocalDate.of(2018, 1, 1)
        val endDate = LocalDate.of(2018, 2, 1)

        val currentDate = startDate plusDays 2

        mockFilingDateStaggerParameters(Map(Stagger1 -> Set(
          DateRange(startDate, endDate)
        )))
        // TODO will need to change this to reflect the filing dates when available

        mockCurrentDate(currentDate)

        val res = TestMigrationCheckService.checkMigrationRestrictions(
          vatNumber = testVatNumber,
          stagger = Stagger1,
          isDirectDebit = false,
          isMigratable = true
        )

        res shouldBe Left(MigratableDates(
          Some(endDate plusDays 1),
          None
        ))

        verifyAudit(ControlListAuditModel(
          testVatNumber,
          isSuccess = false,
          failureReasons = Seq(ControlListAuditing.filingDateMigrationRestrictionMessage)
        ))
      }
    }

    "the current date and stagger fall within the monthly filing dates" should {
      "return Left(MigratableDates)" in {
        val startDate = LocalDate.of(2018, 1, 1)
        val endDate = LocalDate.of(2018, 2, 1)

        val currentDate = startDate plusDays 2

        mockFilingDateStaggerParameters(Map(MonthlyStagger -> Set(
          DateRange(startDate, endDate)
        )))

        mockCurrentDate(currentDate)

        val res = TestMigrationCheckService.checkMigrationRestrictions(
          vatNumber = testVatNumber,
          stagger = MonthlyStagger,
          isDirectDebit = false,
          isMigratable = true
        )

        res shouldBe Left(MigratableDates(
          Some(endDate plusDays 1),
          None
        ))

        verifyAudit(ControlListAuditModel(
          testVatNumber,
          isSuccess = false,
          failureReasons = Seq(ControlListAuditing.filingDateMigrationRestrictionMessage)
        ))
      }
    }

    "the current date and stagger fall within the monthly direct debit dates" should {
      "return Left(MigratableDates)" in {
        val startDate = LocalDate.of(2018, 1, 1)
        val endDate = LocalDate.of(2018, 2, 1)

        val currentDate = startDate plusDays 2

        mockDirectDebitStaggerParameters(Map(MonthlyStagger -> Set(
          DateRange(startDate, endDate)
        )))

        mockCurrentDate(currentDate)

        val res = TestMigrationCheckService.checkMigrationRestrictions(
          vatNumber = testVatNumber,
          stagger = MonthlyStagger,
          isDirectDebit = true,
          isMigratable = true
        )

        res shouldBe Left(MigratableDates(
          Some(endDate plusDays 1),
          None
        ))

        verifyAudit(ControlListAuditModel(
          testVatNumber,
          isSuccess = false,
          failureReasons = Seq(ControlListAuditing.directDebitMigrationRestrictionMessage)
        ))
      }
    }
  }
}
