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

import uk.gov.hmrc.vatsignup.models.controllist._

sealed trait EligibilityConfiguration

sealed trait EligibleConfig extends EligibilityConfiguration

case object MigratableParameter extends EligibleConfig

case object NonMigratableParameter extends EligibleConfig

case object IneligibleParameter extends EligibilityConfiguration


case class EligibilityConfig(config: Map[ControlListParameter, EligibilityConfiguration]) {
  lazy val ineligibleParameters: Set[ControlListParameter] = config.filter { case (_, value) => value == IneligibleParameter }.keySet
  lazy val nonMigratableParameters: Set[ControlListParameter] = config.filter { case (_, value) => value == NonMigratableParameter }.keySet
}

object EligibilityConfig {

  def apply(belowVatThresholdConfig: EligibilityConfiguration,
            annualStaggerConfig: EligibilityConfiguration,
            missingReturnsConfig: EligibilityConfiguration,
            centralAssessmentsConfig: EligibilityConfiguration,
            criminalInvestigationInhibitsConfig: EligibilityConfiguration,
            compliancePenaltiesOrSurchargesConfig: EligibilityConfiguration,
            insolvencyConfig: EligibilityConfiguration,
            deRegOrDeathConfig: EligibilityConfiguration,
            debtMigrationConfig: EligibilityConfiguration,
            directDebitConfig: EligibilityConfiguration,
            euSalesOrPurchasesConfig: EligibilityConfiguration,
            largeBusinessConfig: EligibilityConfiguration,
            missingTraderConfig: EligibilityConfiguration,
            monthlyStaggerConfig: EligibilityConfiguration,
            nonStandardTaxPeriodConfig: EligibilityConfiguration,
            overseasTraderConfig: EligibilityConfiguration,
            poaTraderConfig: EligibilityConfiguration,
            stagger1Config: EligibilityConfiguration,
            stagger2Config: EligibilityConfiguration,
            stagger3Config: EligibilityConfiguration,
            companyConfig: EligibilityConfiguration,
            divisionConfig: EligibilityConfiguration,
            groupConfig: EligibilityConfiguration,
            partnershipConfig: EligibilityConfiguration,
            publicCorporationConfig: EligibilityConfiguration,
            soleTraderConfig: EligibilityConfiguration,
            localAuthorityConfig: EligibilityConfiguration,
            nonProfitConfig: EligibilityConfiguration,
            dificTraderConfig: EligibilityConfiguration,
            anythingUnderAppealConfig: EligibilityConfiguration,
            repaymentTraderConfig: EligibilityConfiguration,
            mossTraderConfig: EligibilityConfiguration
           ): EligibilityConfig =
    new EligibilityConfig(
      Map(
        BelowVatThreshold -> belowVatThresholdConfig,
        AnnualStagger -> annualStaggerConfig,
        MissingReturns -> missingReturnsConfig,
        CentralAssessments -> centralAssessmentsConfig,
        CriminalInvestigationInhibits -> criminalInvestigationInhibitsConfig,
        CompliancePenaltiesOrSurcharges -> compliancePenaltiesOrSurchargesConfig,
        Insolvency -> insolvencyConfig,
        DeRegOrDeath -> deRegOrDeathConfig,
        DebtMigration -> debtMigrationConfig,
        DirectDebit -> directDebitConfig,
        EuSalesOrPurchases -> euSalesOrPurchasesConfig,
        LargeBusiness -> largeBusinessConfig,
        MissingTrader -> missingTraderConfig,
        MonthlyStagger -> monthlyStaggerConfig,
        NonStandardTaxPeriod -> nonStandardTaxPeriodConfig,
        OverseasTrader -> overseasTraderConfig,
        PoaTrader -> poaTraderConfig,
        Stagger1 -> stagger1Config,
        Stagger2 -> stagger2Config,
        Stagger3 -> stagger3Config,
        Company -> companyConfig,
        Division -> divisionConfig,
        Group -> groupConfig,
        Partnership -> partnershipConfig,
        PublicCorporation -> publicCorporationConfig,
        SoleTrader -> soleTraderConfig,
        LocalAuthority -> localAuthorityConfig,
        NonProfitMakingBody -> nonProfitConfig,
        DificTrader -> dificTraderConfig,
        AnythingUnderAppeal -> anythingUnderAppealConfig,
        RepaymentTrader -> repaymentTraderConfig,
        MossTrader -> mossTraderConfig
      )
    )

}
