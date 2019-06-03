/*
 * Copyright 2019 HM Revenue & Customs
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
import play.api.libs.json.Json
import play.api.{Configuration, Environment}
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.vatsignup.config.featureswitch.{FeatureSwitch, FeatureSwitching}
import uk.gov.hmrc.vatsignup.models.DateRange
import uk.gov.hmrc.vatsignup.models.controllist._

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

  lazy val enrolmentStoreProxyUrl: String = baseUrl("enrolment-store-proxy")

  def registerWithMultipleIdentifiersUrl: String = s"$desUrl/cross-regime/register/VATC"

  lazy val authenticatorUrl: String = baseUrl("authenticator")

  lazy val emailVerificationUrl: String = baseUrl("email-verification")

  lazy val emailVerifiedUrl= s"$emailVerificationUrl/email-verification/verified-email-check"

  lazy val verifyEmailUrl = s"$emailVerificationUrl/email-verification/verification-requests"

  lazy val frontendBaseUrl: String = loadConfig("microservice.services.vat-sign-up-frontend.url")

  lazy val principalVerifyEmailContinueUrl = s"$frontendBaseUrl/vat-through-software/sign-up/email-verified"

  lazy val delegatedVerifyEmailContinueUrl = s"$frontendBaseUrl/vat-through-software/sign-up/client/client-email-verified"

  lazy val agentVerifyEmailContinueUrl = s"$frontendBaseUrl/vat-through-software/sign-up/client/email-verified"

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

  def vatSubscriptionKnownFacts(vatNumber: String): String = s"$vatSubscriptionUrl/vat-subscription/$vatNumber/known-facts"

  def getCtReferenceUrl(companyNumber: String): String = s"$desUrl/corporation-tax/identifiers/crn/$companyNumber"

  def upsertEnrolmentUrl(enrolmentKey: String): String = s"$taxEnrolmentsUrl/enrolments/$enrolmentKey"

  def allocateEnrolmentUrl(groupId: String, enrolmentKey: String): String = s"$taxEnrolmentsUrl/groups/$groupId/enrolments/$enrolmentKey"

  def assignEnrolmentUrl(userId: String, enrolmentKey: String): String = s"$taxEnrolmentsUrl/users/$userId/enrolments/$enrolmentKey"

  def getPartnershipKnownFactsUrl(sautr: String): String =
    s"$desUrl/income-tax-self-assessment/known-facts/utr/$sautr"

  def getAllocatedEnrolmentUrl(enrolmentKey: String): String =
    s"$enrolmentStoreProxyUrl/enrolment-store-proxy/enrolment-store/enrolments/$enrolmentKey/groups"

  def queryUsersUrl(vatNumber: String): String =
    s"$enrolmentStoreProxyUrl/enrolment-store-proxy/enrolment-store/enrolments/HMCE-VATDEC-ORG~VATRegNo~$vatNumber/users"

  def usersGroupsSearchUrl: String = baseUrl("users-groups-search")

  def getUsersForGroupUrl(groupId: String): String = s"$usersGroupsSearchUrl/users-groups-search/groups/$groupId/users"

  override def isEnabled(featureSwitch: FeatureSwitch): Boolean = super.isEnabled(featureSwitch)

  def isDisabled(featureSwitch: FeatureSwitch): Boolean = !isEnabled(featureSwitch)

  private def loadConfigFromEnvFirst(key: String): Option[String] = {
    sys.props.get(key) match {
      case r@Some(result) if result.nonEmpty => r
      case _ => runModeConfiguration.getString(key)
    }
  }

  def loadIsEligibleConfig(param: ControlListParameter): Boolean =
    loadConfigFromEnvFirst(s"control-list.${param.configKey}.eligible") match {
      case Some(bool) => bool.toBoolean
      case _ => throw new Exception(s"Missing eligibility configuration key: ${param.configKey}")
    }

  def loadIsMigratableConfig(param: ControlListParameter): Boolean =
    loadConfigFromEnvFirst(s"control-list.${param.configKey}.migratable") match {
      case Some(bool) => bool.toBoolean
      case _ => throw new Exception(s"Missing migratability configuration key: ${param.configKey}")
    }

  private def loadDatesConfig(configKey: String): Map[Stagger, Set[DateRange]] =
    loadConfigFromEnvFirst(configKey) match {
      case Some(jsonConfig) =>
        val config = Json.parse(jsonConfig.replaceAll("[\n\r]", ""))
        val stagger1Dates = (config \ "Stagger1").validate[Set[DateRange]].asOpt
        val stagger2Dates = (config \ "Stagger2").validate[Set[DateRange]].asOpt
        val stagger3Dates = (config \ "Stagger3").validate[Set[DateRange]].asOpt
        val monthlyStaggerDates = (config \ "MonthlyStagger").validate[Set[DateRange]].asOpt
        Map(
          Stagger1 -> stagger1Dates,
          Stagger2 -> stagger2Dates,
          Stagger3 -> stagger3Dates,
          MonthlyStagger -> monthlyStaggerDates
        ).collect { case (key, Some(value)) => (key, value) }
      case _ => throw new Exception(s"Missing migratability configuration key: $configKey")
    }

  def loadFilingDateConfig: Map[Stagger, Set[DateRange]] = loadDatesConfig("filing-config")

  def loadDirectDebitConfig: Map[Stagger, Set[DateRange]] = loadDatesConfig("dd-config")

}
