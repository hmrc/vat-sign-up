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

package uk.gov.hmrc.vatsignup.config

import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.vatsignup.models.controllist._
import EligibilityConfig._


@Singleton
class EligibilityConfig @Inject()(appConfig: AppConfig) {
  import appConfig.loadEligibilityConfig

  private lazy val configMap = Map(
    BelowVatThreshold -> loadEligibilityConfig("below_vat_threshold"),
    AnnualStagger -> loadEligibilityConfig("annual_stagger"),
    MissingReturns -> loadEligibilityConfig("missing_returns"),
    CentralAssessments -> loadEligibilityConfig("central_assessments"),
    CriminalInvestigationInhibits -> loadEligibilityConfig("criminal_investigation_inhibits"),
    CompliancePenaltiesOrSurcharges -> loadEligibilityConfig("compliance_penalties_or_surcharges"),
    Insolvency -> loadEligibilityConfig("insolvency"),
    DeRegOrDeath -> loadEligibilityConfig("dereg_or_death"),
    DebtMigration -> loadEligibilityConfig("debt_migration"),
    DirectDebit -> loadEligibilityConfig("direct_debit"),
    EuSalesOrPurchases -> loadEligibilityConfig("eu_sales_or_purchases"),
    LargeBusiness -> loadEligibilityConfig("large_business"),
    MissingTrader -> loadEligibilityConfig("missing_trader"),
    MonthlyStagger -> loadEligibilityConfig("monthly_stagger"),
    NonStandardTaxPeriod -> loadEligibilityConfig("none_standard_tax_period"),
    OverseasTrader -> loadEligibilityConfig("overseas_trader"),
    PoaTrader -> loadEligibilityConfig("poa_trader"),
    Stagger1 -> loadEligibilityConfig("stagger_1"),
    Stagger2 -> loadEligibilityConfig("stagger_2"),
    Stagger3 -> loadEligibilityConfig("stagger_3"),
    Company -> loadEligibilityConfig("company"),
    Division -> loadEligibilityConfig("division"),
    Group -> loadEligibilityConfig("group"),
    Partnership -> loadEligibilityConfig("partnership"),
    PublicCorporation -> loadEligibilityConfig("public_corporation"),
    SoleTrader -> loadEligibilityConfig("sole_trader"),
    LocalAuthority -> loadEligibilityConfig("local_authority"),
    NonProfitMakingBody -> loadEligibilityConfig("non_profit"),
    DificTrader -> loadEligibilityConfig("dific_trader"),
    AnythingUnderAppeal -> loadEligibilityConfig("anything_under_appeal"),
    RepaymentTrader -> loadEligibilityConfig("repayment_trader"),
    MossTrader -> loadEligibilityConfig("oss_trader")
  )

  lazy val ineligibleParameters: Set[ControlListParameter] = (configMap collect {
    case (attribute, migrationStatus) if migrationStatus == IneligibleParameter => attribute
  }).toSet
  lazy val nonMigratableParameters: Set[ControlListParameter] = (configMap collect {
    case (attribute, migrationStatus) if migrationStatus == NonMigratableParameter => attribute
  }).toSet

}


object EligibilityConfig {
  sealed trait EligibilityConfiguration

  sealed trait EligibleConfig extends EligibilityConfiguration

  case object MigratableParameter extends EligibleConfig

  case object NonMigratableParameter extends EligibleConfig

  case object IneligibleParameter extends EligibilityConfiguration
}
