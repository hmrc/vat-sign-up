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

package uk.gov.hmrc.vatsignup.helpers.servicemocks

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.{Scenario, StubMapping}
import play.api.test.Helpers._
import play.api.libs.json.{JsObject, Json}
import uk.gov.hmrc.vatsignup.models._
import uk.gov.hmrc.vatsignup.utils.JsonUtils._

object RegistrationStub extends WireMockMethods {
  private val registerUri = "/cross-regime/register/VATC"

  private def registerCompanyJsonBody(vatNumber: String, companyNumber: String): JsObject =
    Json.obj(
      "company" -> Json.obj(
        "vrn" -> vatNumber,
        "crn" -> companyNumber
      )
    )

  private def registerIndividualJsonBody(vatNumber: String, nino: String): JsObject =
    Json.obj(
      "soleTrader" -> Json.obj(
        "vrn" -> vatNumber,
        "nino" -> nino
      )
    )

  private def registerGeneralPartnershipJsonBody(vatNumber: String, sautr: Option[String]): JsObject =
    Json.obj(
      "ordinaryPartnership" -> (
        Json.obj("vrn" -> vatNumber)
          + ("sautr" -> sautr)
        )
    )

  private def registerLimitedPartnershipJsonBody(vatNumber: String, sautr: Option[String], companyNumber: String): JsObject =
    Json.obj(
      "limitedPartnership" -> (
        Json.obj(
          "vrn" -> vatNumber,
          "crn" -> companyNumber
        )
          + ("sautr" -> sautr)
        )
    )

  private def registerLimitedLiabilityPartnershipJsonBody(vatNumber: String, sautr: Option[String], companyNumber: String): JsObject =
    Json.obj(
      "limitedLiabilityPartnership" -> (
        Json.obj(
          "vrn" -> vatNumber,
          "crn" -> companyNumber
        )
          + ("sautr" -> sautr)
        )
    )

  private def registerScottishLimitedPartnershipJsonBody(vatNumber: String, sautr: Option[String], companyNumber: String): JsObject =
    Json.obj(
      "scottishLimitedPartnership" -> (
        Json.obj(
          "vrn" -> vatNumber,
          "crn" -> companyNumber
        )
          + ("sautr" -> sautr)
        )
    )

  private def registerVatGroupJsonBody(vatNumber: String): JsObject =
    Json.obj(
      "vatGroup" -> Json.obj(
        "vrn" -> vatNumber
      )
    )

  private def registerDivisionJsonBody(vatNumber: String): JsObject =
    Json.obj(
      "division" -> Json.obj(
        "vrn" -> vatNumber
      )
    )

  private def registerUnincorporatedAssociationJsonBody(vatNumber: String): JsObject =
    Json.obj(
      "unincorporatedAssociation" -> Json.obj(
        "vrn" -> vatNumber
      )
    )

  private def registerTrustJsonBody(vatNumber: String): JsObject =
    Json.obj(
      "trust" -> Json.obj(
        "vrn" -> vatNumber
      )
    )

  private def registerCharityJsonBody(vatNumber: String): JsObject =
    Json.obj(
      "charitableIncorporatedOrganisation" -> Json.obj(
        "vrn" -> vatNumber
      )
    )

  private def registerRegisteredSocietyJsonBody(vatNumber: String, companyNumber: String): JsObject =
    Json.obj(
      "registeredSociety" -> Json.obj(
        "vrn" -> vatNumber,
        "crn" -> companyNumber
      )
    )

  private def registerGovernmentOrganisationJsonBody(vatNumber: String): JsObject =
    Json.obj(
      "publicBody" -> Json.obj(
        "vrn" -> vatNumber
      )
    )

  private def registerOverseasJsonBody(vatNumber: String): JsObject =
    Json.obj(
      "nonUKCompanyNoUKEstablishment" -> Json.obj(
        "vrn" -> vatNumber
      )
    )

  private def registerOverseasWithUkEstablishmentJsonBody(vatNumber: String, companyNumber: String): JsObject =
    Json.obj(
      "nonUKCompanyWithUKEstablishment" -> Json.obj(
        "vrn" -> vatNumber,
        "crn" -> companyNumber
      )
    )

  private def registerResponseBody(safeId: String): JsObject =
    Json.obj(
      "identification" -> Json.arr(
        Json.obj(
          "idType" -> "SAFEID",
          "idValue" -> safeId
        )
      )
    )

  private val desHeaders = Map(
    "Authorization" -> "Bearer dev",
    "Content-Type" -> "application/json",
    "Environment" -> "dev"
  )

  def stubRegisterBusinessEntity(vatNumber: String, businessEntity: BusinessEntity)(safeId: String): Unit = {
    when(
      method = POST,
      uri = registerUri,
      headers = desHeaders,
      body = businessEntity match {
        case SoleTrader(nino) =>
          registerIndividualJsonBody(vatNumber, nino)
        case LimitedCompany(companyNumber) =>
          registerCompanyJsonBody(vatNumber, companyNumber)
        case GeneralPartnership(sautr) =>
          registerGeneralPartnershipJsonBody(vatNumber, sautr)
        case LimitedPartnership(sautr, companyNumber) =>
          registerLimitedPartnershipJsonBody(vatNumber, sautr, companyNumber)
        case LimitedLiabilityPartnership(sautr, companyNumber) =>
          registerLimitedLiabilityPartnershipJsonBody(vatNumber, sautr, companyNumber)
        case ScottishLimitedPartnership(sautr, companyNumber) =>
          registerScottishLimitedPartnershipJsonBody(vatNumber, sautr, companyNumber)
        case VatGroup =>
          registerVatGroupJsonBody(vatNumber)
        case AdministrativeDivision =>
          registerDivisionJsonBody(vatNumber)
        case UnincorporatedAssociation =>
          registerUnincorporatedAssociationJsonBody(vatNumber)
        case Trust =>
          registerTrustJsonBody(vatNumber)
        case Charity =>
          registerCharityJsonBody(vatNumber)
        case RegisteredSociety(companyNumber) =>
          registerRegisteredSocietyJsonBody(vatNumber, companyNumber)
        case GovernmentOrganisation =>
          registerGovernmentOrganisationJsonBody(vatNumber)
        case Overseas =>
          registerOverseasJsonBody(vatNumber)
        case OverseasWithUkEstablishment(companyNumber) =>
          registerOverseasWithUkEstablishmentJsonBody(vatNumber, companyNumber)
      }
    ) thenReturn(OK, registerResponseBody(safeId))
  }

  def stubTimeoutRetry(safeId: String): StubMapping = {
    val delayInMilliseconds = 2100
    stubFor(post(urlEqualTo(registerUri))
      .inScenario("timeout")
      .whenScenarioStateIs(Scenario.STARTED)
      .willReturn(
        aResponse().withStatus(GATEWAY_TIMEOUT).withFixedDelay(delayInMilliseconds)
      )
      .willSetStateTo("pass")
    )

    stubFor(post(urlEqualTo(registerUri))
      .inScenario("timeout")
      .whenScenarioStateIs("pass")
      .willReturn(
        aResponse().withStatus(OK).withBody(registerResponseBody(safeId).toString())
      )
    )
  }

  def stubInternalServerErrorRetry(safeId: String): StubMapping = {
    stubFor(post(urlEqualTo(registerUri))
      .inScenario("error")
      .whenScenarioStateIs(Scenario.STARTED)
      .willReturn(
        aResponse().withStatus(INTERNAL_SERVER_ERROR)
      )
      .willSetStateTo("pass")
    )

    stubFor(post(urlEqualTo(registerUri))
      .inScenario("error")
      .whenScenarioStateIs("pass")
      .willReturn(
        aResponse().withStatus(OK).withBody(registerResponseBody(safeId).toString())
      )
    )
  }

}
