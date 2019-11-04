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

import play.api.http.Status._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.vatsignup.helpers.ComponentSpecBase
import uk.gov.hmrc.vatsignup.helpers.IntegrationTestConstants._
import uk.gov.hmrc.vatsignup.helpers.servicemocks.SignUpStub._
import uk.gov.hmrc.vatsignup.models.{CustomerSignUpResponseFailure, CustomerSignUpResponseSuccess}

class MigratedCustomerSignUpConnectorISpec extends ComponentSpecBase {

  lazy val connector: MigratedCustomerSignUpConnector = app.injector.instanceOf[MigratedCustomerSignUpConnector]

  implicit val hc = HeaderCarrier()

  "customerSignUp" when {
    "the sign up is successful" should {
      "Return CustomerSignUpResponseSuccess" in {
        stubMigratedSignUp(
          testSafeId,
          testVatNumber,
          isMigratable = true
        )(OK)

        val res = await(connector.signUp(testSafeId, testVatNumber, isMigratable = true))

        res shouldBe Right(CustomerSignUpResponseSuccess)
      }
    }
    "the sign up is not successful" should {
      "return CustomerSignUpResponseFailure" in {
        stubMigratedSignUp(
          testSafeId,
          testVatNumber,
          isMigratable = true
        )(BAD_REQUEST)

        val res = await(connector.signUp(testSafeId, testVatNumber,  isMigratable = true))

        res shouldBe Left(CustomerSignUpResponseFailure(BAD_REQUEST, ""))
      }
    }
  }

}
