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

object KnownFactsStub extends WireMockMethods {

  private def stubGetKnownFacts(vatNumber: String)(status: Int, body: Option[JsValue]): StubMapping =
    when(method = GET, uri = s"/vat-subscription/$vatNumber/known-facts").thenReturn(
      status = status,
      body = body
    )

  def stubSuccessGetKnownFacts(vatNumber: String): StubMapping =
    stubGetKnownFacts(vatNumber)(OK, Some(successResponseBody))

  def stubFailureVatNumberNotFound(vatNumber: String): StubMapping =
    stubGetKnownFacts(vatNumber)(NOT_FOUND, None)

  def stubFailureInvalidVatNumber(vatNumber: String): StubMapping =
    stubGetKnownFacts(vatNumber)(BAD_REQUEST, None)

  private def successResponseBody =
    Json.obj(
      "postcode" -> testPostCode,
      "dateOfReg" -> testDateOfRegistration
    )
}
