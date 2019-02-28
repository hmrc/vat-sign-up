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

package uk.gov.hmrc.vatsignup.helpers.servicemocks

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.http.Status.{BAD_REQUEST, NOT_FOUND, OK}
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

  def stubGetKnownFactsAndControlListInformation32(vatNumber: String, businessPostcode: String, vatRegistrationDate: String): Unit = {
    val body = Json.obj(
      "postcode" -> businessPostcode,
      "dateOfReg" -> vatRegistrationDate,
      "controlListInformation" -> ControlList32.eligible
    )
    stubGetKnownFactsAndControlListInformation(vatNumber)(OK, Some(body))
  }

  def stubGetKnownFactsAndControlListInformation33(vatNumber: String, businessPostcode: String, vatRegistrationDate: String): Unit = {
    val body = Json.obj(
      "postcode" -> businessPostcode,
      "dateOfReg" -> vatRegistrationDate,
      "controlListInformation" -> ControlList33.eligible
    )
    stubGetKnownFactsAndControlListInformation(vatNumber)(OK, Some(body))
  }

  def stubIneligibleControlListInformation(vatNumber: String): Unit = {
    val body = Json.obj(
      "postcode" -> testPostCode,
      "dateOfReg" -> testDateOfRegistration,
      "controlListInformation" -> ControlList33.ineligible
    )
    stubGetKnownFactsAndControlListInformation(vatNumber)(OK, Some(body))
  }

  def stubDirectDebitControlListInformation(vatNumber: String): Unit = {
    val body = Json.obj(
      "postcode" -> testPostCode,
      "dateOfReg" -> testDateOfRegistration,
      "controlListInformation" -> ControlList33.directDebit
    )
    stubGetKnownFactsAndControlListInformation(vatNumber)(OK, Some(body))
  }

  def stubOverseasControlListInformation(vatNumber: String): Unit = {
    val body = Json.obj(
      "postcode" -> testPostCode,
      "dateOfReg" -> testDateOfRegistration,
      "controlListInformation" -> ControlList33.overseas
    )
    stubGetKnownFactsAndControlListInformation(vatNumber)(OK, Some(body))
  }

  def stubSuccessGetKnownFactsAndControlListInformation(vatNumber: String): StubMapping =
    stubGetKnownFactsAndControlListInformation(vatNumber)(OK, Some(successResponseBody))

  def stubFailureControlListVatNumberNotFound(vatNumber: String): StubMapping =
    stubGetKnownFactsAndControlListInformation(vatNumber)(NOT_FOUND, None)

  def stubFailureKnownFactsInvalidVatNumber(vatNumber: String): StubMapping =
    stubGetKnownFactsAndControlListInformation(vatNumber)(BAD_REQUEST, None)

  private def successResponseBody =
    Json.obj(
      "postcode" -> testPostCode,
      "dateOfReg" -> testDateOfRegistration,
      "lastReturnMonthPeriod" -> testLastReturnMonthPeriod,
      "lastNetDue" -> testLastNetDue.toDouble,
      "controlListInformation" -> ControlList33.eligible
    )

}
