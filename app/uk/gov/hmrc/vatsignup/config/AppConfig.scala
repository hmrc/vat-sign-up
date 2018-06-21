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

import play.api.Mode.Mode
import play.api.{Configuration, Environment}
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.vatsignup.config.featureswitch.{FeatureSwitch, FeatureSwitching}

@Singleton
class AppConfig @Inject()(val runModeConfiguration: Configuration, environment: Environment) extends ServicesConfig with FeatureSwitching {
  override protected def mode: Mode = environment.mode

  private def loadConfig(key: String) = runModeConfiguration.getString(key).getOrElse(throw new Exception(s"Missing configuration key: $key"))

  lazy val agentClientRelationshipUrl: String =
    loadConfig(
      if (isEnabled(featureswitch.StubDESFeature)) "microservice.services.agent-client-relationships.stub-url"
      else "microservice.services.agent-client-relationships.url"
    ) + "/agent-client-relationships"

  lazy val taxEnrolmentsUrl: String = baseUrl("tax-enrolments") + "/tax-enrolments"

  def desUrl: String =
    loadConfig(
      if (isEnabled(featureswitch.StubDESFeature)) "microservice.services.des.stub-url"
      else "microservice.services.des.url"
    )

  lazy val desAuthorisationToken: String = s"Bearer ${loadConfig("microservice.services.des.authorisation-token")}"
  lazy val desEnvironmentHeader: (String, String) =
    "Environment" -> loadConfig("microservice.services.des.environment")

  def registerWithMultipleIdentifiersUrl: String = s"$desUrl/cross-regime/register/VATC"

  lazy val authenticatorUrl: String = baseUrl("authenticator")

  lazy val emailVerificationUrl: String = baseUrl("email-verification")

  def getEmailVerifiedUrl(email: String): String = s"$emailVerificationUrl/email-verification/verified-email-addresses/$email"

  lazy val verifyEmailUrl = s"$emailVerificationUrl/email-verification/verification-requests"

  lazy val frontendBaseUrl: String = loadConfig("microservice.services.vat-sign-up-frontend.url")

  lazy val principalVerifyEmailContinueUrl = s"$frontendBaseUrl/vat-through-software/sign-up/email-verified"

  lazy val delegatedVerifyEmailContinueUrl = s"$frontendBaseUrl/vat-through-software/sign-up/client/email-verified"

  lazy val agentVerifyEmailContinueUrl = s"$frontendBaseUrl/vat-through-software/sign-up/client/you-have-verified-your-email"

  lazy val identityVerificationFrontendUrl: String = baseUrl("identity-verification-frontend")

  lazy val timeToLiveSeconds: Long = loadConfig("mongodb.timeToLiveSeconds").toLong

  lazy val emailTimeToLiveSeconds: Long = loadConfig("mongodb.email.emailTimeToLiveSeconds").toLong

  def vatSubscriptionUrl: String =
    if (isEnabled(featureswitch.StubDESFeature)) desUrl
    else baseUrl("vat-subscription")

  lazy val baseUrl: String = baseUrl("base")

  lazy val emailUrl: String = baseUrl("email")

  lazy val sendEmailUrl: String = s"$emailUrl/hmrc/email"

  def mandationStatusUrl(vatNumber: String): String = s"$vatSubscriptionUrl/vat-subscription/$vatNumber/mandation-status"

  override def isEnabled(featureSwitch: FeatureSwitch): Boolean = super.isEnabled(featureSwitch)

  private def loadEligibilityConfig(key: String): Boolean =
    runModeConfiguration.getBoolean(s"control-list.eligible.$key").getOrElse(throw new Exception(s"Missing eligibility configuration key: $key"))

  def eligibilityConfig: EligibilityConfig = EligibilityConfig(
    permitBelowVatThreshold = loadEligibilityConfig("below_vat_threshold"),
    permitAnnualStagger = loadEligibilityConfig("annual_stagger"),
    permitMissingReturns = loadEligibilityConfig("missing_returns"),
    permitCentralAssessments = loadEligibilityConfig("central_assessments"),
    permitCriminalInvestigationInhibits = loadEligibilityConfig("criminal_investigation_inhibits"),
    permitCompliancePenaltiesOrSurcharges = loadEligibilityConfig("compliance_penalties_or_surcharges"),
    permitInsolvency = loadEligibilityConfig("insolvency"),
    permitDeRegOrDeath = loadEligibilityConfig("dereg_or_death"),
    permitDebtMigration = loadEligibilityConfig("debt_migration"),
    permitDirectDebit = loadEligibilityConfig("direct_debit"),
    permitEuSalesOrPurchases = loadEligibilityConfig("eu_sales_or_purchases"),
    permitLargeBusiness = loadEligibilityConfig("large_business"),
    permitMissingTrader = loadEligibilityConfig("missing_trader"),
    permitMonthlyStagger = loadEligibilityConfig("monthly_stagger"),
    permitNonStandardTaxPeriod = loadEligibilityConfig("none_standard_tax_period"),
    permitOverseasTrader = loadEligibilityConfig("overseas_trader"),
    permitPoaTrader = loadEligibilityConfig("poa_trader"),
    permitStagger1 = loadEligibilityConfig("stagger_1"),
    permitStagger2 = loadEligibilityConfig("stagger_2"),
    permitStagger3 = loadEligibilityConfig("stagger_3"),
    permitCompany = loadEligibilityConfig("company"),
    permitDivision = loadEligibilityConfig("division"),
    permitGroup = loadEligibilityConfig("group"),
    permitPartnership = loadEligibilityConfig("partnership"),
    permitPublicCorporation = loadEligibilityConfig("public_corporation"),
    permitSoleTrader = loadEligibilityConfig("sole_trader"),
    permitLocalAuthority = loadEligibilityConfig("local_authority"),
    permitNonProfit = loadEligibilityConfig("non_profit"),
    permitDificTrader = loadEligibilityConfig("dific_trader"),
    permitAnythingUnderAppeal = loadEligibilityConfig("anything_under_appeal"),
    permitRepaymentTrader = loadEligibilityConfig("repayment_trader"),
    permitMossTrader = loadEligibilityConfig("oss_trader")
  )

}
