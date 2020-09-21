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

import org.scalatest.EitherValues
import play.api.test.Helpers._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.vatsignup.helpers.ComponentSpecBase
import uk.gov.hmrc.vatsignup.helpers.IntegrationTestConstants._
import uk.gov.hmrc.vatsignup.helpers.servicemocks.KnownFactsStub
import uk.gov.hmrc.vatsignup.httpparsers.VatCustomerDetailsHttpParser._
import uk.gov.hmrc.vatsignup.models.{KnownFacts, VatCustomerDetails}

class VatCustomerDetailsConnectorISpec extends ComponentSpecBase with EitherValues {

  private lazy val vatCustomerDetailsConnector: VatCustomerDetailsConnector =
    app.injector.instanceOf[VatCustomerDetailsConnector]

  private implicit val headerCarrier: HeaderCarrier = HeaderCarrier()

  "getVatCustomerDetails" when {
    "Vat subscription returns a successful response" should {
      "return the known facts" in {
        KnownFactsStub.stubSuccessGetKnownFacts(testVatNumber)

        val res = await(vatCustomerDetailsConnector.getVatCustomerDetails(testVatNumber))

        res.right.value shouldBe VatCustomerDetails(KnownFacts(testPostCode, testDateOfRegistration), isOverseas = false)
      }
    }

    "Vat subscription returns a successful response for an overseas user" should {
      "return the known facts" in {
        KnownFactsStub.stubSuccessGetKnownFactsOverseas(testVatNumber)

        val res = await(vatCustomerDetailsConnector.getVatCustomerDetails(testVatNumber))

        res.right.value shouldBe VatCustomerDetails(KnownFacts(testPostCode, testDateOfRegistration), isOverseas = true)
      }
    }

    "Vat subscription returns returns a BAD_REQUEST" should {
      "return an InvalidVatNumber" in {
        KnownFactsStub.stubFailureInvalidVatNumber(testVatNumber)

        val res = await(vatCustomerDetailsConnector.getVatCustomerDetails(testVatNumber))

        res.left.value shouldBe InvalidVatNumber
      }
    }

    "Vat subscription returns a NOT_FOUND" should {
      "return a VatNumberNotFound" in {
        KnownFactsStub.stubFailureVatNumberNotFound(testVatNumber)

        val res = await(vatCustomerDetailsConnector.getVatCustomerDetails(testVatNumber))

        res.left.value shouldBe VatNumberNotFound
      }
    }
  }
}
