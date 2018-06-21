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

package uk.gov.hmrc.vatsignup.models

import cats._
import cats.data.Validated._
import cats.data.{NonEmptyList, Validated}
import cats.implicits._
import uk.gov.hmrc.vatsignup.config.EligibilityConfig
import uk.gov.hmrc.vatsignup.models.ControlListInformation.{Group, _}
import uk.gov.hmrc.vatsignup.utils.controllist.ControlListIneligibilityMessages._

case class ControlListInformation(belowVatThreshold: Boolean,
                                  missingReturns: Boolean,
                                  centralAssessments: Boolean,
                                  criminalInvestigationInhibits: Boolean,
                                  compliancePenaltiesOrSurcharges: Boolean,
                                  insolvency: Boolean,
                                  deRegOrDeath: Boolean,
                                  debtMigration: Boolean,
                                  directDebit: Boolean,
                                  euSalesOrPurchases: Boolean,
                                  largeBusiness: Boolean,
                                  missingTrader: Boolean,
                                  staggerType: Stagger,
                                  nonStandardTaxPeriod: Boolean,
                                  overseasTrader: Boolean,
                                  poaTrader: Boolean,
                                  entityType: BusinessEntity,
                                  dificTrader: Boolean,
                                  anythingUnderAppeal: Boolean,
                                  repaymentTrader: Boolean,
                                  mossTrader: Boolean
                                 ) {
  // scalastyle:off cyclomatic.complexity
  def validate(eligibilityConfig: EligibilityConfig): ValidatedType = {
    Monoid.combineAll(List(
      if (!eligibilityConfig.permitBelowVatThreshold && belowVatThreshold) ineligible(belowVatThresholdMessage) else eligible,
      if (!eligibilityConfig.permitMissingReturns && missingReturns) ineligible(missingReturnsMessage) else eligible,
      if (!eligibilityConfig.permitCentralAssessments && centralAssessments) ineligible(centralAssessmentsMessage) else eligible,
      if (!eligibilityConfig.permitCriminalInvestigationInhibits && criminalInvestigationInhibits) ineligible(criminalInvestigationInhibitsMessage) else eligible,
      if (!eligibilityConfig.permitCompliancePenaltiesOrSurcharges && compliancePenaltiesOrSurcharges) ineligible(compliancePenaltiesOrSurchargesMessage) else eligible,
      if (!eligibilityConfig.permitInsolvency && insolvency) ineligible(insolvencyMessage) else eligible,
      if (!eligibilityConfig.permitDeRegOrDeath && deRegOrDeath) ineligible(deRegOrDeathMessage) else eligible,
      if (!eligibilityConfig.permitDebtMigration && debtMigration) ineligible(debtMigrationMessage) else eligible,
      if (!eligibilityConfig.permitDirectDebit && directDebit) ineligible(directDebitMessage) else eligible,
      if (!eligibilityConfig.permitEuSalesOrPurchases && euSalesOrPurchases) ineligible(euSalesOrPurchasesMessage) else eligible,
      if (!eligibilityConfig.permitLargeBusiness && largeBusiness) ineligible(largeBusinessMessage) else eligible,
      if (!eligibilityConfig.permitMissingTrader && missingTrader) ineligible(missingTraderMessage) else eligible,
      staggerType match {
        case AnnualStagger if eligibilityConfig.permitAnnualStagger => eligible
        case MonthlyStagger if eligibilityConfig.permitMonthlyStagger => eligible
        case Stagger1 if eligibilityConfig.permitStagger1 => eligible
        case Stagger2 if eligibilityConfig.permitStagger2 => eligible
        case Stagger3 if eligibilityConfig.permitStagger3 => eligible
        case _ => ineligible(invalidStaggerTypeMessage(staggerType))
      },
      if (!eligibilityConfig.permitNonStandardTaxPeriod && nonStandardTaxPeriod) ineligible(nonStandardTaxPeriodMessage) else eligible,
      if (!eligibilityConfig.permitOverseasTrader && overseasTrader) ineligible(overseasTraderMessage) else eligible,
      if (!eligibilityConfig.permitPoaTrader && poaTrader) ineligible(poaTraderMessage) else eligible,
      entityType match {
        case Company if eligibilityConfig.permitCompany => eligible
        case Division if eligibilityConfig.permitDivision => eligible
        case Group if eligibilityConfig.permitGroup => eligible
        case Partnership if eligibilityConfig.permitPartnership => eligible
        case PublicCorporation if eligibilityConfig.permitPublicCorporation => eligible
        case SoleTrader if eligibilityConfig.permitSoleTrader => eligible
        case LocalAuthority if eligibilityConfig.permitLocalAuthority => eligible
        case NonProfitMakingBody if eligibilityConfig.permitNonProfit => eligible
        case _ => ineligible(invalidEntityTypeMessage(entityType))
      },
      if (!eligibilityConfig.permitDificTrader && dificTrader) ineligible(dificTraderMessage) else eligible,
      if (!eligibilityConfig.permitAnythingUnderAppeal && anythingUnderAppeal) ineligible(anythingUnderAppealMessage) else eligible,
      if (!eligibilityConfig.permitRepaymentTrader && repaymentTrader) ineligible(repaymentTraderMessage) else eligible,
      if (!eligibilityConfig.permitMossTrader && mossTrader) ineligible(mossTraderMessage) else eligible
    ))
  }

  // scalastyle:on cyclomatic.complexity

}

object ControlListInformation {
  type ValidatedType = Validated[NonEmptyList[String], Unit]

  val eligible: ValidatedType = Validated.Valid(())

  def ineligible(errorMessage: String, additionalErrorMessages: String*): ValidatedType =
    Invalid(NonEmptyList.apply(errorMessage, additionalErrorMessages.toList))

  sealed trait Stagger

  case object AnnualStagger extends Stagger

  case object MonthlyStagger extends Stagger

  case object Stagger1 extends Stagger

  case object Stagger2 extends Stagger

  case object Stagger3 extends Stagger


  sealed trait BusinessEntity

  case object Company extends BusinessEntity

  case object Division extends BusinessEntity

  case object Group extends BusinessEntity

  case object Partnership extends BusinessEntity

  case object PublicCorporation extends BusinessEntity

  case object SoleTrader extends BusinessEntity

  case object LocalAuthority extends BusinessEntity

  case object NonProfitMakingBody extends BusinessEntity

}




