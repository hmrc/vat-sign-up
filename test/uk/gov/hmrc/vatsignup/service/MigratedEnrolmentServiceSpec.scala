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

package uk.gov.hmrc.vatsignup.service

import org.scalatest.{Matchers, WordSpec}
import play.api.test.Helpers._
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}
import uk.gov.hmrc.vatsignup.connectors.mocks.MockTaxEnrolmentsConnector
import uk.gov.hmrc.vatsignup.helpers.TestConstants._
import uk.gov.hmrc.vatsignup.httpparsers.TaxEnrolmentsHttpParser.{FailedTaxEnrolment, SuccessfulTaxEnrolment}
import uk.gov.hmrc.vatsignup.services.MigratedEnrolmentService
import uk.gov.hmrc.vatsignup.services.MigratedEnrolmentService._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class MigratedEnrolmentServiceSpec extends WordSpec with Matchers with MockTaxEnrolmentsConnector {

  object TestMigratedEnrolmentService extends MigratedEnrolmentService(mockTaxEnrolmentsConnector)

  implicit val hc = HeaderCarrier()

  "enrolForMtdVat" when {
    "when the enrolment is successful" should {
      "return EnrolmentSuccess" in {
        mockRegisterEnrolment(testVatNumber, testSafeId)(
          Future.successful(Right(SuccessfulTaxEnrolment))
        )

        val res = await(TestMigratedEnrolmentService.enrolForMtd(testVatNumber, testSafeId))

        res shouldBe EnrolmentSuccess
      }
    }
    "when the enrolment fails" should {
      "throw internal server exception" in {
        mockRegisterEnrolment(testVatNumber, testSafeId)(
          Future.successful(Left(FailedTaxEnrolment(BAD_REQUEST)))
        )

        intercept[InternalServerException] {
          await(TestMigratedEnrolmentService.enrolForMtd(testVatNumber, testSafeId))
        }
      }
    }
  }

}
