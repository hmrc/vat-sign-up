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
import play.api.http.Status._
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.vatsignup.helpers.IntegrationTestConstants._

object KnownFactsAndControlListInformationStub extends WireMockMethods {

  private def stubGetKnownFactsAndControlListInformation(vatNumber: String)(status: Int, body: Option[JsValue]): StubMapping =
    when(method = GET, uri = s"/vat/known-facts/control-list/$vatNumber",
      headers = Map(
        "Authorization" -> "Bearer dev",
        "Environment" -> "dev"
      )
    ).thenReturn(status = status, body = body)

  def stubGetKnownFactsAndControlListInformation34(vatNumber: String, businessPostcode: String, vatRegistrationDate: String): Unit = {
    val body = Json.obj(
      "postcode" -> businessPostcode,
      "dateOfReg" -> vatRegistrationDate,
      "controlListInformation" -> ControlList34.eligible
    )
    stubGetKnownFactsAndControlListInformation(vatNumber)(OK, Some(body))
  }

  def stubGetKnownFactsAndControlListInformation35(vatNumber: String, businessPostcode: String, vatRegistrationDate: String): Unit = {
    val body = Json.obj(
      "postcode" -> businessPostcode,
      "dateOfReg" -> vatRegistrationDate,
      "controlListInformation" -> ControlList35.eligible
    )
    stubGetKnownFactsAndControlListInformation(vatNumber)(OK, Some(body))
  }

  def stubGetKnownFactsAndControlListInformationMultipleEntities34(vatNumber: String, businessPostcode: String, vatRegistrationDate: String): Unit = {
    val body = Json.obj(
      "postcode" -> businessPostcode,
      "dateOfReg" -> vatRegistrationDate,
      "controlListInformation" -> ControlList35.multipleEntities
    )
    stubGetKnownFactsAndControlListInformation(vatNumber)(OK, Some(body))
  }

  def stubIneligibleControlListInformation(vatNumber: String): Unit = {
    val body = Json.obj(
      "postcode" -> testPostCode,
      "dateOfReg" -> testDateOfRegistration,
      "controlListInformation" -> ControlList35.ineligible
    )
    stubGetKnownFactsAndControlListInformation(vatNumber)(OK, Some(body))
  }

  def stubEligibleControlListInformation(vatNumber: String): Unit = {
    val body = Json.obj(
      "postcode" -> testPostCode,
      "dateOfReg" -> testDateOfRegistration,
      "controlListInformation" -> ControlList35.eligible
    )
    stubGetKnownFactsAndControlListInformation(vatNumber)(OK, Some(body))
  }

  def stubDirectDebitControlListInformation(vatNumber: String): Unit = {
    val body = Json.obj(
      "postcode" -> testPostCode,
      "dateOfReg" -> testDateOfRegistration,
      "controlListInformation" -> ControlList35.directDebit
    )
    stubGetKnownFactsAndControlListInformation(vatNumber)(OK, Some(body))
  }

  def stubOverseasControlListInformation(vatNumber: String): Unit = {
    val body = Json.obj(
      "postcode" -> testPostCode,
      "dateOfReg" -> testDateOfRegistration,
      "controlListInformation" -> ControlList35.overseas
    )
    stubGetKnownFactsAndControlListInformation(vatNumber)(OK, Some(body))
  }

  def stubDeregisteredControlListInformation(vatNumber: String): Unit = {
    val body = Json.obj(
      "postcode" -> testPostCode,
      "dateOfReg" -> testDateOfRegistration,
      "controlListInformation" -> ControlList35.deregistered
    )
    stubGetKnownFactsAndControlListInformation(vatNumber)(OK, Some(body))
  }

  def stubOverseasFourKFControlListInformation(vatNumber: String): Unit = {
    val body = Json.obj(
      "postcode" -> "",
      "dateOfReg" -> testDateOfRegistration,
      "lastReturnMonthPeriod" -> testLastReturnMonthPeriod,
      "lastNetDue" -> testLastNetDue.toDouble,
      "controlListInformation" -> ControlList35.overseas
    )
    stubGetKnownFactsAndControlListInformation(vatNumber)(OK, Some(body))
  }

  def stubSuccessGetKnownFactsAndControlListInformation(vatNumber: String): StubMapping =
    stubGetKnownFactsAndControlListInformation(vatNumber)(OK, Some(successResponseBody))

  def stubFailureControlListVatNumberNotFound(vatNumber: String): StubMapping =
    stubGetKnownFactsAndControlListInformation(vatNumber)(NOT_FOUND, None)

  def stubFailureKnownFactsInvalidVatNumber(vatNumber: String): StubMapping =
    stubGetKnownFactsAndControlListInformation(vatNumber)(BAD_REQUEST, None)

  def stubTimeoutRetry(vatNumber: String): StubMapping = {
    stubFor(get(urlEqualTo(s"/vat/known-facts/control-list/$vatNumber"))
      .inScenario("timeout")
      .whenScenarioStateIs(Scenario.STARTED)
      .willReturn(
        aResponse().withStatus(GATEWAY_TIMEOUT).withFixedDelay(2100)
      )
      .willSetStateTo("pass")
    )

    stubFor(get(urlEqualTo(s"/vat/known-facts/control-list/$vatNumber"))
      .inScenario("timeout")
      .whenScenarioStateIs("pass")
      .willReturn(
        aResponse().withStatus(OK).withBody(successResponseBody.toString())
      )
    )
  }

  def stubInternalServerErrorRetry(vatNumber: String): StubMapping = {
    stubFor(get(urlEqualTo(s"/vat/known-facts/control-list/$vatNumber"))
      .inScenario("error")
      .whenScenarioStateIs(Scenario.STARTED)
      .willReturn(
        aResponse().withStatus(INTERNAL_SERVER_ERROR)
      )
      .willSetStateTo("pass")
    )

    stubFor(get(urlEqualTo(s"/vat/known-facts/control-list/$vatNumber"))
      .inScenario("error")
      .whenScenarioStateIs("pass")
      .willReturn(
        aResponse().withStatus(OK).withBody(successResponseBody.toString())
      )
    )
  }

  def stubSuccessNotFiled(vatNumber: String): Unit =
    stubGetKnownFactsAndControlListInformation(vatNumber)(OK, Some(Json.obj(
      "postcode" -> testPostCode,
      "dateOfReg" -> testDateOfRegistration,
      "lastReturnMonthPeriod" -> "N/A",
      "lastNetDue" -> 0,
      "controlListInformation" -> ControlList35.eligible
    )))

  private def successResponseBody =
    Json.obj(
      "postcode" -> testPostCode,
      "dateOfReg" -> testDateOfRegistration,
      "lastReturnMonthPeriod" -> testLastReturnMonthPeriod,
      "lastNetDue" -> testLastNetDue.toDouble,
      "controlListInformation" -> ControlList35.eligible
    )

}
