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

package uk.gov.hmrc.vatsignup.connectors

import com.github.tomakehurst.wiremock.client.WireMock.{getRequestedFor, urlEqualTo, verify}
import play.api.test.Helpers._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.vatsignup.helpers.ComponentSpecBase
import uk.gov.hmrc.vatsignup.helpers.IntegrationTestConstants._
import uk.gov.hmrc.vatsignup.helpers.servicemocks.GetCtReferenceStub._

class GetCtReferenceConnectorISpec extends ComponentSpecBase {

  lazy val connector: GetCtReferenceConnector = app.injector.instanceOf[GetCtReferenceConnector]

  private implicit val headerCarrier: HeaderCarrier = HeaderCarrier()

  "getCtReference" should {
    "return the CT reference" in {
      stubGetCtReference(testCompanyNumber)(OK, ctReferenceBody(testCtReference))

      val res = connector.getCtReference(testCompanyNumber)

      await(res) shouldBe Right(testCtReference)
    }

    "return the CT reference on a successful retry after a timeout" in {
      stubTimeoutRetry(testCompanyNumber)(OK, ctReferenceBody(testCtReference))

      val res = await(connector.getCtReference(testCompanyNumber))

      res shouldBe Right(testCtReference)
      verify(2, getRequestedFor(urlEqualTo(s"/corporation-tax/identifiers/crn/$testCompanyNumber")))
    }

    "return the CT reference on a successful retry after a 500 response" in {
      stubFailureRetry(testCompanyNumber)(OK, ctReferenceBody(testCtReference))

      val res = await(connector.getCtReference(testCompanyNumber))

      res shouldBe Right(testCtReference)
      verify(2, getRequestedFor(urlEqualTo(s"/corporation-tax/identifiers/crn/$testCompanyNumber")))
    }
  }

}
