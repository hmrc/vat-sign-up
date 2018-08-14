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

package uk.gov.hmrc.vatsignup.service


import play.api.http.Status
import uk.gov.hmrc.http.{ForbiddenException, HeaderCarrier}
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.vatsignup.connectors.mocks._
import uk.gov.hmrc.vatsignup.helpers.TestConstants._
import uk.gov.hmrc.vatsignup.httpparsers.{AllocateEnrolmentResponseHttpParser, KnownFactsHttpParser}
import uk.gov.hmrc.vatsignup.httpparsers.AllocateEnrolmentResponseHttpParser.EnrolSuccess
import uk.gov.hmrc.vatsignup.httpparsers.KnownFactsHttpParser.{InvalidKnownFacts, KnownFacts}
import uk.gov.hmrc.vatsignup.services.ClaimSubscriptionService
import uk.gov.hmrc.vatsignup.services.ClaimSubscriptionService._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ClaimSubscriptionServiceSpec extends UnitSpec
  with MockKnownFactsConnector with MockAuthConnector with MockTaxEnrolmentsConnector {

  object TestClaimSubscriptionService extends ClaimSubscriptionService(
    mockAuthConnector,
    mockKnownFactsConnector,
    mockTaxEnrolmentsConnector
  )

  implicit val hc: HeaderCarrier = HeaderCarrier()

  "claimSubscription" when {
    "the known facts connector is successful" when {
      "auth returns a valid ggw credential and group ID" when {
        "tax enrolments returns a success" should {
          "return SubscriptionClaimed" in {
            mockGetKnownFacts(testVatNumber)(Future.successful(Right(KnownFacts(testPostCode, testDateOfRegistration))))
            mockAuthRetrieveCredentialAndGroupId(testCredentials, Some(testGroupId))
            mockAllocateEnrolment(
              testGroupId,
              testCredentialId,
              testVatNumber,
              testPostCode,
              testDateOfRegistration.toTaxEnrolmentsFormat
            )(Future.successful(Right(EnrolSuccess)))

            val res = await(TestClaimSubscriptionService.claimSubscription(testVatNumber))

            res shouldBe Right(SubscriptionClaimed)
          }
        }
        "tax enrolments returns a failure" should {
          "return TaxEnrolmentsFailure" in {
            mockGetKnownFacts(testVatNumber)(Future.successful(Right(KnownFacts(testPostCode, testDateOfRegistration))))
            mockAuthRetrieveCredentialAndGroupId(testCredentials, Some(testGroupId))
            mockAllocateEnrolment(
              testGroupId,
              testCredentialId,
              testVatNumber,
              testPostCode,
              testDateOfRegistration.toTaxEnrolmentsFormat
            )(Future.successful(Left(AllocateEnrolmentResponseHttpParser.EnrolFailure(""))))

            val res = await(TestClaimSubscriptionService.claimSubscription(testVatNumber))

            res shouldBe Left(EnrolFailure)
          }
        }
      }
      "auth does not return a valid credential" should {
        "return InvalidCredential" in {
          mockGetKnownFacts(testVatNumber)(Future.successful(Right(KnownFacts(testPostCode, testDateOfRegistration))))
          mockAuthRetrieveCredentialAndGroupId(testCredentials, None)

          val res = TestClaimSubscriptionService.claimSubscription(testVatNumber)

          intercept[ForbiddenException](await(res))
        }
      }
    }
    "the known facts connector returns invalid VAT number" should {
      "return InvalidVatNumber" in {
        mockGetKnownFacts(testVatNumber)(Future.successful(Left(KnownFactsHttpParser.InvalidVatNumber)))

        val res = await(TestClaimSubscriptionService.claimSubscription(testVatNumber))

        res shouldBe Left(InvalidVatNumber)
      }
    }
    "the known facts connector returns VAT number not found" should {
      "return InvalidVatNumber" in {
        mockGetKnownFacts(testVatNumber)(Future.successful(Left(KnownFactsHttpParser.VatNumberNotFound)))

        val res = await(TestClaimSubscriptionService.claimSubscription(testVatNumber))

        res shouldBe Left(VatNumberNotFound)
      }
    }
    "the known facts connector fails" should {
      "return KnownFactsFailure" in {
        mockGetKnownFacts(testVatNumber)(Future.successful(Left(InvalidKnownFacts(
          status = Status.BAD_REQUEST,
          body = ""
        ))))

        val res = await(TestClaimSubscriptionService.claimSubscription(testVatNumber))

        res shouldBe Left(KnownFactsFailure)
      }
    }
  }

  "toTaxEnrolmentFormat" should {
    "convert the date to the correct format" in {
      "2017-01-01".toTaxEnrolmentsFormat shouldBe "01/01/17"
      "1999-01-01".toTaxEnrolmentsFormat shouldBe "01/01/99"
    }
  }
}
