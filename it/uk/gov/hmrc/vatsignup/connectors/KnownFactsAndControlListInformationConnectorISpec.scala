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

import com.github.tomakehurst.wiremock.client.WireMock._
import org.scalatest.EitherValues
import play.api.test.Helpers._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.vatsignup.config.featureswitch.FeatureSwitching
import uk.gov.hmrc.vatsignup.helpers.ComponentSpecBase
import uk.gov.hmrc.vatsignup.helpers.IntegrationTestConstants._
import uk.gov.hmrc.vatsignup.helpers.servicemocks.KnownFactsAndControlListInformationStub
import uk.gov.hmrc.vatsignup.httpparsers.KnownFactsAndControlListInformationHttpParser._
import uk.gov.hmrc.vatsignup.models.{KnownFactsAndControlListInformation, VatKnownFacts}

import java.time.Month

class KnownFactsAndControlListInformationConnectorISpec extends ComponentSpecBase with EitherValues
  with FeatureSwitching {

  private lazy val KnownFactsAndControlListInformationConnector: KnownFactsAndControlListInformationConnector =
    app.injector.instanceOf[KnownFactsAndControlListInformationConnector]

  private implicit val headerCarrier: HeaderCarrier = HeaderCarrier()

  "getKnownFactsAndControlListInformation" when {
    "DES returns a successful response" should {
      "return the known facts and control list information" in {
        KnownFactsAndControlListInformationStub.stubSuccessGetKnownFactsAndControlListInformation(testVatNumber)

        val res = await(
          KnownFactsAndControlListInformationConnector.getKnownFactsAndControlListInformation(testVatNumber)
        )
        res.right.value shouldBe KnownFactsAndControlListInformation(
          VatKnownFacts(
            businessPostcode = Some(testPostCode),
            vatRegistrationDate = testDateOfRegistration,
            lastReturnMonthPeriod = Some(Month.MARCH),
            lastNetDue = Some(testLastNetDue)
          ),
          controlListInformation = eligibleModel
        )
      }
    }

    "DES returns a BAD_REQUEST" should {
      "return a KnownFactsInvalidVatNumber" in {
        KnownFactsAndControlListInformationStub.stubFailureKnownFactsInvalidVatNumber(testVatNumber)

        val res = await(
          KnownFactsAndControlListInformationConnector.getKnownFactsAndControlListInformation(testVatNumber)
        )
        res.left.value shouldBe KnownFactsInvalidVatNumber
      }
    }

    "DES returns a NOT_FOUND" should {
      "return a ControlListInformationVatNumberNotFound" in {
        KnownFactsAndControlListInformationStub.stubFailureControlListVatNumberNotFound(testVatNumber)

        val res = await(
          KnownFactsAndControlListInformationConnector.getKnownFactsAndControlListInformation(testVatNumber)
        )
        res.left.value shouldBe ControlListInformationVatNumberNotFound
      }
    }

    "DES times out and then succeeds on the retry" should {
      "return the known facts and control list information" in {
        KnownFactsAndControlListInformationStub.stubTimeoutRetry(testVatNumber)

        val res = await(
          KnownFactsAndControlListInformationConnector.getKnownFactsAndControlListInformation(testVatNumber)
        )

        res.right.value shouldBe KnownFactsAndControlListInformation(
          VatKnownFacts(
            businessPostcode = Some(testPostCode),
            vatRegistrationDate = testDateOfRegistration,
            lastReturnMonthPeriod = Some(Month.MARCH),
            lastNetDue = Some(testLastNetDue)
          ),
          controlListInformation = eligibleModel
        )

        verify(2, getRequestedFor(urlEqualTo(s"/vat/known-facts/control-list/$testVatNumber")))
      }
    }

    "DES returns an INTERNAL_SERVER_ERROR and then succeeds on the retry" should {
      "return the known facts and control list information" in {
        KnownFactsAndControlListInformationStub.stubInternalServerErrorRetry(testVatNumber)

        val res = await(
          KnownFactsAndControlListInformationConnector.getKnownFactsAndControlListInformation(testVatNumber)
        )

        res.right.value shouldBe KnownFactsAndControlListInformation(
          VatKnownFacts(
            businessPostcode = Some(testPostCode),
            vatRegistrationDate = testDateOfRegistration,
            lastReturnMonthPeriod = Some(Month.MARCH),
            lastNetDue = Some(testLastNetDue)
          ),
          controlListInformation = eligibleModel
        )

        verify(2, getRequestedFor(urlEqualTo(s"/vat/known-facts/control-list/$testVatNumber")))
      }
    }
  }

}
