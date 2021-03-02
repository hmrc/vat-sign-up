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

import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, get, stubFor, urlEqualTo}
import com.github.tomakehurst.wiremock.stubbing.{Scenario, StubMapping}
import play.api.http.Status._
import play.api.libs.json.{JsObject, JsValue, Json}
import uk.gov.hmrc.vatsignup.httpparsers.GetCtReferenceHttpParser.CtReferenceKey

object GetCtReferenceStub extends WireMockMethods {
  private val desHeaders = Map(
    "Authorization" -> "Bearer dev",
    "Content-Type" -> "application/json",
    "Environment" -> "dev"
  )

  def stubGetCtReference(companyNumber: String)(status: Int, body: JsValue = Json.obj()): StubMapping =
    when(method = GET, uri = s"/corporation-tax/identifiers/crn/$companyNumber")
      .thenReturn(status = status, body = body, headers = desHeaders)

  def stubTimeoutRetry(companyNumber: String)(status: Int, body: JsValue = Json.obj()): StubMapping = {
    stubFor(get(urlEqualTo(s"/corporation-tax/identifiers/crn/$companyNumber"))
      .inScenario("timeout")
      .whenScenarioStateIs(Scenario.STARTED)
      .willReturn(
        aResponse().withStatus(GATEWAY_TIMEOUT).withFixedDelay(2100)
      )
      .willSetStateTo("pass")
    )


    stubFor(get(urlEqualTo(s"/corporation-tax/identifiers/crn/$companyNumber"))
      .inScenario("timeout")
      .whenScenarioStateIs("pass")
      .willReturn(
        aResponse().withStatus(status).withBody(body.toString())
      )

    )
  }

  def stubFailureRetry(companyNumber: String)(status: Int, body: JsValue = Json.obj()): StubMapping = {
    stubFor(get(urlEqualTo(s"/corporation-tax/identifiers/crn/$companyNumber"))
      .inScenario("timeout")
      .whenScenarioStateIs(Scenario.STARTED)
      .willReturn(
        aResponse().withStatus(INTERNAL_SERVER_ERROR)
      )
      .willSetStateTo("pass")
    )


    stubFor(get(urlEqualTo(s"/corporation-tax/identifiers/crn/$companyNumber"))
      .inScenario("timeout")
      .whenScenarioStateIs("pass")
      .willReturn(
        aResponse().withStatus(status).withBody(body.toString())
      )

    )
  }

  def ctReferenceBody(ctReference: String): JsObject = Json.obj(CtReferenceKey -> ctReference)
}
