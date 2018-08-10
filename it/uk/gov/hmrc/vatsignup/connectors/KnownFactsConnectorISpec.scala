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

package uk.gov.hmrc.vatsignup.connectors

import org.scalatest.EitherValues
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.vatsignup.helpers.ComponentSpecBase
import uk.gov.hmrc.vatsignup.helpers.IntegrationTestConstants._
import uk.gov.hmrc.vatsignup.helpers.servicemocks.KnownFactsStub
import uk.gov.hmrc.vatsignup.httpparsers.KnownFactsHttpParser._

class KnownFactsConnectorISpec extends ComponentSpecBase with EitherValues {

  private lazy val KnownFactsConnector: KnownFactsConnector =
    app.injector.instanceOf[KnownFactsConnector]

  private implicit val headerCarrier: HeaderCarrier = HeaderCarrier()

  "getKnownFacts" when {
    "Vat subscription returns a successful response" should {
      "return the known facts" in {
        KnownFactsStub.stubSuccessGetKnownFacts(testVatNumber)

        val res = await(KnownFactsConnector.getKnownFacts(testVatNumber))

        res.right.value shouldBe KnownFacts(testPostCode, testDateOfRegistration)
      }
    }

    "Vat subscription returns returns a BAD_REQUEST" should {
      "return an InvalidVatNumber" in {
        KnownFactsStub.stubFailureInvalidVatNumber(testVatNumber)

        val res = await(KnownFactsConnector.getKnownFacts(testVatNumber))

        res.left.value shouldBe InvalidVatNumber
      }
    }

    "Vat subscription returns a NOT_FOUND" should {
      "return a VatNumberNotFound" in {
        KnownFactsStub.stubFailureVatNumberNotFound(testVatNumber)

        val res = await(KnownFactsConnector.getKnownFacts(testVatNumber))

        res.left.value shouldBe VatNumberNotFound
      }
    }
  }
}
