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

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.libs.json.{JsObject, JsValue, Json}
import uk.gov.hmrc.vatsignup.helpers.IntegrationTestConstants._

object PartnershipKnownFactsStub extends WireMockMethods {
  def stubGetPartnershipKnownFacts(sautr: String)(status: Int, body: Option[JsValue] = None): StubMapping =
    when(
      method = GET,
      uri = s"/income-tax-self-assessment/known-facts/utr/$sautr\\?returnType=P", //Escaping the ? to allow the regex url matching to work
      headers = Map(
        "Authorization" -> "Bearer dev",
        "Environment" -> "dev"
      )
    ) thenReturn(
      status = status,
      body = body
    )

  val fullPartnershipKnownFactsBody: JsObject = Json.obj(
    "postCode" -> testPostCode,
    "correspondenceDetails" -> Json.obj(
      "corresPostCode" -> testCorrespondencePostCode
    ),
    "basePostCode" -> testBasePostCode,
    "commsPostCode" -> testCommsPostCode,
    "traderPostCode" -> testTraderPostCode
  )
}
