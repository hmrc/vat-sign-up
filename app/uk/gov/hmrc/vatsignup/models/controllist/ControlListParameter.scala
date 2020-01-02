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

package uk.gov.hmrc.vatsignup.models.controllist

import uk.gov.hmrc.vatsignup.models.controllist.ControlListIndices._
import uk.gov.hmrc.vatsignup.models.controllist.ControlListMessages._

sealed trait ControlListParameter {
  val configKey: String
  val ordinal: Int
  val errorMessage: String

  override def toString: String = errorMessage
}

sealed trait Stagger extends ControlListParameter

sealed trait BusinessEntity extends ControlListParameter

object ControlListParameter {

  val getParameterMap: Map[Int, ControlListParameter] =
    Map(
      BELOW_VAT_THRESHOLD -> BelowVatThreshold,
      ANNUAL_STAGGER -> AnnualStagger,
      MISSING_RETURNS -> MissingReturns,
      CENTRAL_ASSESSMENTS -> CentralAssessments,
      CRIMINAL_INVESTIGATION_INHIBITS -> CriminalInvestigationInhibits,
      COMPLIANCE_PENALTIES_OR_SURCHARGES -> CompliancePenaltiesOrSurcharges,
      INSOLVENCY -> Insolvency,
      DEREG_OR_DEATH -> DeRegOrDeath,
      DEBT_MIGRATION -> DebtMigration,
      DIRECT_DEBIT -> DirectDebit,
      EU_SALES_OR_PURCHASES -> EuSalesOrPurchases,
      LARGE_BUSINESS -> LargeBusiness,
      MISSING_TRADER -> MissingTrader,
      MONTHLY_STAGGER -> MonthlyStagger,
      NONE_STANDARD_TAX_PERIOD -> NonStandardTaxPeriod,
      OVERSEAS_TRADER -> OverseasTrader,
      POA_TRADER -> PoaTrader,
      STAGGER_1 -> Stagger1,
      STAGGER_2 -> Stagger2,
      STAGGER_3 -> Stagger3,
      COMPANY -> Company,
      DIVISION -> Division,
      GROUP -> Group,
      PARTNERSHIP -> Partnership,
      PUBLIC_CORPORATION -> PublicCorporation,
      SOLE_TRADER -> SoleTrader,
      LOCAL_AUTHORITY -> LocalAuthority,
      NON_PROFIT -> NonProfitMakingBody,
      DIFIC_TRADER -> DificTrader,
      ANYTHING_UNDER_APPEAL -> AnythingUnderAppeal,
      REPAYMENT_TRADER -> RepaymentTrader,
      MOSS_TRADER -> MossTrader,
      FLAT_RATE -> FlatRate,
      PENDING_OA -> PendingOA
    )
}


case object BelowVatThreshold extends ControlListParameter {
  val configKey: String = "below_vat_threshold"
  val ordinal = BELOW_VAT_THRESHOLD
  val errorMessage: String = belowVatThresholdMessage
}

case object AnnualStagger extends ControlListParameter with Stagger {
  val configKey: String = "annual_stagger"
  val ordinal = ANNUAL_STAGGER
  val errorMessage: String = invalidStaggerTypeMessage("AnnualStagger")
}

case object MissingReturns extends ControlListParameter {
  val configKey: String = "missing_returns"
  val ordinal = MISSING_RETURNS
  val errorMessage: String = missingReturnsMessage
}

case object CentralAssessments extends ControlListParameter {
  val configKey: String = "central_assessments"
  val ordinal = CENTRAL_ASSESSMENTS
  val errorMessage: String = centralAssessmentsMessage
}

case object CriminalInvestigationInhibits extends ControlListParameter {
  val configKey: String = "criminal_investigation_inhibits"
  val ordinal = CRIMINAL_INVESTIGATION_INHIBITS
  val errorMessage: String = criminalInvestigationInhibitsMessage
}

case object CompliancePenaltiesOrSurcharges extends ControlListParameter {
  val configKey: String = "compliance_penalties_or_surcharges"
  val ordinal = COMPLIANCE_PENALTIES_OR_SURCHARGES
  val errorMessage: String = compliancePenaltiesOrSurchargesMessage
}

case object Insolvency extends ControlListParameter {
  val configKey: String = "insolvency"
  val ordinal = INSOLVENCY
  val errorMessage: String = insolvencyMessage
}

case object DeRegOrDeath extends ControlListParameter {
  val configKey: String = "dereg_or_death"
  val ordinal = DEREG_OR_DEATH
  val errorMessage: String = deRegOrDeathMessage
}

case object DebtMigration extends ControlListParameter {
  val configKey: String = "debt_migration"
  val ordinal = DEBT_MIGRATION
  val errorMessage: String = debtMigrationMessage
}

case object DirectDebit extends ControlListParameter {
  val configKey: String = "direct_debit"
  val ordinal = DIRECT_DEBIT
  val errorMessage: String = directDebitMessage
}

case object EuSalesOrPurchases extends ControlListParameter {
  val configKey: String = "eu_sales_or_purchases"
  val ordinal = EU_SALES_OR_PURCHASES
  val errorMessage: String = euSalesOrPurchasesMessage
}

case object LargeBusiness extends ControlListParameter {
  val configKey: String = "large_business"
  val ordinal = LARGE_BUSINESS
  val errorMessage: String = largeBusinessMessage
}

case object MissingTrader extends ControlListParameter {
  val configKey: String = "missing_trader"
  val ordinal = MISSING_TRADER
  val errorMessage: String = missingTraderMessage
}

case object MonthlyStagger extends ControlListParameter with Stagger {
  val configKey: String = "monthly_stagger"
  val ordinal = MONTHLY_STAGGER
  val errorMessage: String = invalidStaggerTypeMessage("MonthlyStagger")
}

case object NonStandardTaxPeriod extends ControlListParameter with Stagger {
  val configKey: String = "none_standard_tax_period"
  val ordinal = NONE_STANDARD_TAX_PERIOD
  val errorMessage: String = nonStandardTaxPeriodMessage
}

case object OverseasTrader extends ControlListParameter {
  val configKey: String = "overseas_trader"
  val ordinal = OVERSEAS_TRADER
  val errorMessage: String = overseasTraderMessage
}

case object PoaTrader extends ControlListParameter {
  val configKey: String = "poa_trader"
  val ordinal = POA_TRADER
  val errorMessage: String = poaTraderMessage
}

case object Stagger1 extends ControlListParameter with Stagger {
  val configKey: String = "stagger_1"
  val ordinal = STAGGER_1
  val errorMessage: String = invalidStaggerTypeMessage("Stagger1")
}

case object Stagger2 extends ControlListParameter with Stagger {
  val configKey: String = "stagger_2"
  val ordinal = STAGGER_2
  val errorMessage: String = invalidStaggerTypeMessage("Stagger2")
}

case object Stagger3 extends ControlListParameter with Stagger {
  val configKey: String = "stagger_3"
  val ordinal = STAGGER_3
  val errorMessage: String = invalidStaggerTypeMessage("Stagger3")
}

case object Company extends ControlListParameter with BusinessEntity {
  val configKey: String = "company"
  val ordinal = COMPANY
  val errorMessage: String = invalidEntityTypeMessage("Company")
}

case object Division extends ControlListParameter with BusinessEntity {
  val configKey: String = "division"
  val ordinal = DIVISION
  val errorMessage: String = invalidEntityTypeMessage("Division")
}

case object Group extends ControlListParameter with BusinessEntity {
  val configKey: String = "group"
  val ordinal = GROUP
  val errorMessage: String = invalidEntityTypeMessage("Group")
}

case object Partnership extends ControlListParameter with BusinessEntity {
  val configKey: String = "partnership"
  val ordinal = PARTNERSHIP
  val errorMessage: String = invalidEntityTypeMessage("Partnership")
}

case object PublicCorporation extends ControlListParameter with BusinessEntity {
  val configKey: String = "public_corporation"
  val ordinal = PUBLIC_CORPORATION
  val errorMessage: String = invalidEntityTypeMessage("PublicCorporation")
}

case object SoleTrader extends ControlListParameter with BusinessEntity {
  val configKey: String = "sole_trader"
  val ordinal = SOLE_TRADER
  val errorMessage: String = invalidEntityTypeMessage("SoleTrader")
}

case object LocalAuthority extends ControlListParameter with BusinessEntity {
  val configKey: String = "local_authority"
  val ordinal = LOCAL_AUTHORITY
  val errorMessage: String = invalidEntityTypeMessage("LocalAuthority")
}

case object NonProfitMakingBody extends ControlListParameter with BusinessEntity {
  val configKey: String = "non_profit"
  val ordinal = NON_PROFIT
  val errorMessage: String = invalidEntityTypeMessage("NonProfitMakingBody")
}

case object DificTrader extends ControlListParameter {
  val configKey: String = "dific_trader"
  val ordinal = DIFIC_TRADER
  val errorMessage: String = dificTraderMessage
}

case object AnythingUnderAppeal extends ControlListParameter {
  val configKey: String = "anything_under_appeal"
  val ordinal = ANYTHING_UNDER_APPEAL
  val errorMessage: String = anythingUnderAppealMessage
}

case object RepaymentTrader extends ControlListParameter {
  val configKey: String = "repayment_trader"
  val ordinal = REPAYMENT_TRADER
  val errorMessage: String = repaymentTraderMessage
}

case object MossTrader extends ControlListParameter {
  val configKey: String = "oss_trader"
  val ordinal = MOSS_TRADER
  val errorMessage: String = mossTraderMessage
}

case object FlatRate extends ControlListParameter {
  val configKey: String = "flat_rate"
  val ordinal = FLAT_RATE
  val errorMessage: String = flatRateMessage
}

case object PendingOA extends ControlListParameter {
  val configKey: String = "pending_oa"
  val ordinal = PENDING_OA
  val errorMessage: String = pendingOaMessage
}
