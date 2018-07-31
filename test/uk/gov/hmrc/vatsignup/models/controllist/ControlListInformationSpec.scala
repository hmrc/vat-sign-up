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

package uk.gov.hmrc.vatsignup.models.controllist

import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.vatsignup.config.{EligibilityConfig, IneligibleParameter, MigratableParameter, NonMigratableParameter}


class ControlListInformationSpec extends UnitSpec {


  // n.b. invalid scenario since multiple stagger and business entities are present
  // however we don't care about that here as that'll fail at the parser level
  val testControlList = ControlListInformation(
    BelowVatThreshold, MissingReturns, CentralAssessments, CriminalInvestigationInhibits,
    CompliancePenaltiesOrSurcharges, Insolvency, DeRegOrDeath, DebtMigration,
    DirectDebit, EuSalesOrPurchases, LargeBusiness, MissingTrader,
    NonStandardTaxPeriod, OverseasTrader, PoaTrader, DificTrader,
    AnythingUnderAppeal, RepaymentTrader, MossTrader,
    AnnualStagger, MonthlyStagger, Stagger1, Stagger2, Stagger3,
    Company, Division, Group, Partnership, PublicCorporation,
    SoleTrader, LocalAuthority, NonProfitMakingBody
  )

  val allMigratableConfig =
    EligibilityConfig(
      MigratableParameter, MigratableParameter, MigratableParameter, MigratableParameter,
      MigratableParameter, MigratableParameter, MigratableParameter, MigratableParameter,
      MigratableParameter, MigratableParameter, MigratableParameter, MigratableParameter,
      MigratableParameter, MigratableParameter, MigratableParameter, MigratableParameter,
      MigratableParameter, MigratableParameter, MigratableParameter, MigratableParameter,
      MigratableParameter, MigratableParameter, MigratableParameter, MigratableParameter,
      MigratableParameter, MigratableParameter, MigratableParameter, MigratableParameter,
      MigratableParameter, MigratableParameter, MigratableParameter, MigratableParameter
    )

  ".validate" should {
    "return Right(Migratable) if every parameter is migratable" in {
      val res = testControlList.validate(allMigratableConfig)
      res shouldBe Right(Migratable)
    }
    "return Right(NonMigratable) with all the members in the reason field if every parameter is nonmigratable" in {
      val nonMigratableConfig = EligibilityConfig(allMigratableConfig.config.map { case (k, v) => (k, NonMigratableParameter) })
      val res = testControlList.validate(nonMigratableConfig)
      res shouldBe Right(NonMigratable(testControlList.controlList.toSeq))
    }
    "return Left(Ineligible) with all the members in the reason field if every parameter is ineligible" in {
      val ineligibleConfig = EligibilityConfig(allMigratableConfig.config.map { case (k, v) => (k, IneligibleParameter) })
      val res = testControlList.validate(ineligibleConfig)
      res shouldBe Left(Ineligible(testControlList.controlList.toSeq))
    }
  }

}
