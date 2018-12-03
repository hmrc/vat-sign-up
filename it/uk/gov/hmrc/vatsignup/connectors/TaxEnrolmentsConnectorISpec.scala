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
import uk.gov.hmrc.vatsignup.helpers.servicemocks.TaxEnrolmentsStub
import uk.gov.hmrc.vatsignup.httpparsers.AllocateEnrolmentResponseHttpParser.{EnrolFailure, EnrolSuccess}
import uk.gov.hmrc.vatsignup.httpparsers.TaxEnrolmentsHttpParser.{FailedTaxEnrolment, SuccessfulTaxEnrolment}
import uk.gov.hmrc.vatsignup.httpparsers.UpsertEnrolmentResponseHttpParser.{UpsertEnrolmentFailure, UpsertEnrolmentSuccess}

class TaxEnrolmentsConnectorISpec extends ComponentSpecBase {

  lazy val connector: TaxEnrolmentsConnector = app.injector.instanceOf[TaxEnrolmentsConnector]

  private implicit val headerCarrier: HeaderCarrier = HeaderCarrier()

  "registerEnrolment" when {
    "Tax Enrolments returns a successful response" should {
      "return a SuccessfulTaxEnrolment" in {
        TaxEnrolmentsStub.stubRegisterEnrolment(testVatNumber, testSafeId)(NO_CONTENT)

        val res = connector.registerEnrolment(testVatNumber, testSafeId)

        await(res) shouldBe Right(SuccessfulTaxEnrolment)
      }
    }

    "Tax Enrolments returns an unsuccessful response" should {
      "return a FailedTaxEnrolment" in {
        TaxEnrolmentsStub.stubRegisterEnrolment(testVatNumber, testSafeId)(BAD_REQUEST)

        val res = connector.registerEnrolment(testVatNumber, testSafeId)

        await(res) shouldBe Left(FailedTaxEnrolment(BAD_REQUEST))
      }
    }
  }

  "upsertEnrolment" when {
    "Tax Enrolments returns a successful response" should {
      "return an EnrolSuccess" in {
        TaxEnrolmentsStub.stubUpsertEnrolment(testVatNumber, testPostCode, testDateOfRegistration)(NO_CONTENT)

        val res = connector.upsertEnrolment(testVatNumber, testPostCode, testDateOfRegistration)

        await(res) shouldBe Right(UpsertEnrolmentSuccess)
      }
    }

    "Tax Enrolments returns an unsuccessful response" should {
      "return an EnrolFailure" in {
        TaxEnrolmentsStub.stubUpsertEnrolment(testVatNumber, testPostCode, testDateOfRegistration)(BAD_REQUEST)

        val res = connector.upsertEnrolment(testVatNumber, testPostCode, testDateOfRegistration)

        await(res) shouldBe Left(UpsertEnrolmentFailure(BAD_REQUEST, ""))
      }
    }
  }

  "allocateEnrolment" when {
    "Tax Enrolments returns a Created" should {
      "return an EnrolSuccess" in {
        TaxEnrolmentsStub.stubAllocateEnrolment(testVatNumber, testGroupId, testCredentialId, testPostCode, testDateOfRegistration)(CREATED)

        val res = connector.allocateEnrolment(testGroupId, testCredentialId, testVatNumber, testPostCode, testDateOfRegistration)

        await(res) shouldBe Right(EnrolSuccess)
      }
    }

    "Tax Enrolments returns a Bad Request" should {
      "return an EnrolFailure" in {
        TaxEnrolmentsStub.stubAllocateEnrolment(testVatNumber, testGroupId, testCredentialId, testPostCode, testDateOfRegistration)(BAD_REQUEST)

        val res = connector.allocateEnrolment(testGroupId, testCredentialId, testVatNumber, testPostCode, testDateOfRegistration)

        await(res) shouldBe Left(EnrolFailure(""))
      }
    }
  }

}