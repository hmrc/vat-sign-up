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
import play.api.http.Status._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.vatsignup.helpers.ComponentSpecBase
import uk.gov.hmrc.vatsignup.helpers.IntegrationTestConstants._
import uk.gov.hmrc.vatsignup.helpers.servicemocks.PartnershipKnownFactsStub._
import uk.gov.hmrc.vatsignup.models.PartnershipKnownFacts

class PartnershipKnownFactsConnectorISpec extends ComponentSpecBase with EitherValues {

  private lazy val KnownFactsConnector: PartnershipKnownFactsConnector =
    app.injector.instanceOf[PartnershipKnownFactsConnector]

  private implicit val headerCarrier: HeaderCarrier = HeaderCarrier()

  "getPartnershipKnownFacts" when {
    "DES returns a successful response" should {
      "return the known facts" in {
        stubGetPartnershipKnownFacts(testUtr)(OK, Some(fullPartnershipKnownFactsBody))

        val res = await(KnownFactsConnector.getPartnershipKnownFacts(testUtr))

        res shouldBe Right(PartnershipKnownFacts(
          postCode = Some(testPostCode),
          correspondencePostCode = Some(testCorrespondencePostCode),
          basePostCode = Some(testBasePostCode),
          commsPostCode = Some(testCommsPostCode),
          traderPostCode = Some(testTraderPostCode)
        ))
      }
    }

  }
}
