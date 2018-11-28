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
import play.api.mvc.Request
import play.api.test.FakeRequest
import uk.gov.hmrc.http.{ForbiddenException, HeaderCarrier}
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.vatsignup.connectors.mocks._
import uk.gov.hmrc.vatsignup.helpers.TestConstants._
import uk.gov.hmrc.vatsignup.httpparsers.AllocateEnrolmentResponseHttpParser.EnrolSuccess
import uk.gov.hmrc.vatsignup.httpparsers.KnownFactsHttpParser.{InvalidKnownFacts, KnownFacts}
import uk.gov.hmrc.vatsignup.httpparsers.UpsertEnrolmentResponseHttpParser.{UpsertEnrolmentFailure, UpsertEnrolmentSuccess}
import uk.gov.hmrc.vatsignup.httpparsers.{AllocateEnrolmentResponseHttpParser, KnownFactsHttpParser}
import uk.gov.hmrc.vatsignup.models.monitoring.ClaimSubscriptionAuditing.ClaimSubscriptionAuditModel
import uk.gov.hmrc.vatsignup.service.mocks.monitoring.MockAuditService
import uk.gov.hmrc.vatsignup.services.ClaimSubscriptionService
import uk.gov.hmrc.vatsignup.services.ClaimSubscriptionService._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ClaimSubscriptionServiceSpec extends UnitSpec
  with MockKnownFactsConnector with MockAuthConnector with MockTaxEnrolmentsConnector with MockAuditService {

  object TestClaimSubscriptionService extends ClaimSubscriptionService(
    mockAuthConnector,
    mockKnownFactsConnector,
    mockTaxEnrolmentsConnector,
    mockAuditService
  )

  implicit val hc: HeaderCarrier = HeaderCarrier()
  implicit val request: Request[_] = FakeRequest()

  "claimSubscription" when {
    "the known facts connector is successful" when {
      "auth returns a valid ggw credential and group ID" when {
        "tax enrolment to upsert the enrolment is successful" when {
          "tax enrolment to allocate enrolment returns a success" should {
            "return SubscriptionClaimed" in {
              mockGetKnownFacts(testVatNumber)(Future.successful(Right(KnownFacts(testPostCode, testDateOfRegistration))))
              mockAuthRetrieveCredentialAndGroupId(testCredentials, Some(testGroupId))
              mockUpsertEnrolment(testVatNumber, testPostCode, testDateOfRegistration.toTaxEnrolmentsFormat)(
                Future.successful(Right(UpsertEnrolmentSuccess))
              )
              mockAllocateEnrolment(
                testGroupId,
                testCredentialId,
                testVatNumber,
                testPostCode,
                testDateOfRegistration.toTaxEnrolmentsFormat
              )(Future.successful(Right(EnrolSuccess)))

              val res = await(TestClaimSubscriptionService.claimSubscription(testVatNumber, None, None, isFromBta =
                false))

              res shouldBe Right(SubscriptionClaimed)
              verifyAudit(ClaimSubscriptionAuditModel(
                testVatNumber,
                testPostCode,
                testDateOfRegistration.toTaxEnrolmentsFormat,
                isFromBta = false,
                isSuccess = true
              ))
            }
          }
          "tax enrolment to allocate enrolment returns a failure" should {
            "return TaxEnrolmentsFailure" in {
              val allocateEnrolmentFailureMessage = "allocateEnrolmentFailure"

              mockGetKnownFacts(testVatNumber)(Future.successful(Right(KnownFacts(testPostCode, testDateOfRegistration))))
              mockAuthRetrieveCredentialAndGroupId(testCredentials, Some(testGroupId))
              mockUpsertEnrolment(testVatNumber, testPostCode, testDateOfRegistration.toTaxEnrolmentsFormat)(
                Future.successful(Right(UpsertEnrolmentSuccess))
              )
              mockAllocateEnrolment(
                testGroupId,
                testCredentialId,
                testVatNumber,
                testPostCode,
                testDateOfRegistration.toTaxEnrolmentsFormat
              )(Future.successful(Left(AllocateEnrolmentResponseHttpParser.UnexpectedEnrolFailure(allocateEnrolmentFailureMessage))))

              val res = await(TestClaimSubscriptionService.claimSubscription(testVatNumber, None, None, isFromBta = true))

              res shouldBe Left(EnrolFailure)
              verifyAudit(ClaimSubscriptionAuditModel(
                testVatNumber,
                testPostCode,
                testDateOfRegistration.toTaxEnrolmentsFormat,
                isFromBta = true,
                isSuccess = false,
                allocateEnrolmentFailureMessage = Some(allocateEnrolmentFailureMessage)
              ))
            }
          }
        }
        "tax enrolment to upsert the enrolment fails" when {
          "tax enrolment to allocate enrolment returns a success" should {
            "return SubscriptionClaimed" in {
              val upsertEnrolmentErrorMessage = "upsertEnrolErr"

              mockGetKnownFacts(testVatNumber)(Future.successful(Right(KnownFacts(testPostCode, testDateOfRegistration))))
              mockAuthRetrieveCredentialAndGroupId(testCredentials, Some(testGroupId))
              mockUpsertEnrolment(testVatNumber, testPostCode, testDateOfRegistration.toTaxEnrolmentsFormat)(
                Future.successful(Left(UpsertEnrolmentFailure(status = Status.BAD_REQUEST, message = upsertEnrolmentErrorMessage)))
              )
              mockAllocateEnrolment(
                testGroupId,
                testCredentialId,
                testVatNumber,
                testPostCode,
                testDateOfRegistration.toTaxEnrolmentsFormat
              )(Future.successful(Right(EnrolSuccess)))

              val res = await(TestClaimSubscriptionService.claimSubscription(testVatNumber, None, None, isFromBta = false))

              res shouldBe Right(SubscriptionClaimed)

              verifyAudit(ClaimSubscriptionAuditModel(
                testVatNumber,
                testPostCode,
                testDateOfRegistration.toTaxEnrolmentsFormat,
                isFromBta = false,
                isSuccess = true
              ))
            }
          }
          "tax enrolment to allocate enrolment a failure" should {
            "return TaxEnrolmentsFailure" in {
              val allocateEnrolmentErrorMessage = "allocateEnrolErr"
              val upsertEnrolmentErrorMessage = "upsertEnrolErr"

              mockGetKnownFacts(testVatNumber)(Future.successful(Right(KnownFacts(testPostCode, testDateOfRegistration))))
              mockAuthRetrieveCredentialAndGroupId(testCredentials, Some(testGroupId))
              mockUpsertEnrolment(testVatNumber, testPostCode, testDateOfRegistration.toTaxEnrolmentsFormat)(
                Future.successful(Left(UpsertEnrolmentFailure(status = Status.BAD_REQUEST, message = upsertEnrolmentErrorMessage)))
              )
              mockAllocateEnrolment(
                testGroupId,
                testCredentialId,
                testVatNumber,
                testPostCode,
                testDateOfRegistration.toTaxEnrolmentsFormat
              )(Future.successful(Left(AllocateEnrolmentResponseHttpParser.UnexpectedEnrolFailure(allocateEnrolmentErrorMessage))))

              val res = await(TestClaimSubscriptionService.claimSubscription(testVatNumber, None, None, isFromBta = true))

              res shouldBe Left(EnrolFailure)
              verifyAudit(ClaimSubscriptionAuditModel(
                testVatNumber,
                testPostCode,
                testDateOfRegistration.toTaxEnrolmentsFormat,
                isFromBta = true,
                isSuccess = false,
                allocateEnrolmentFailureMessage = Some(allocateEnrolmentErrorMessage),
                upsertEnrolmentFailureMessage = Some(upsertEnrolmentErrorMessage))
              )
            }
          }
        }

        "the supplied known facts do not match what is held on ETMP" should {
          "return KnownFactsMismatch" in {
            mockGetKnownFacts(testVatNumber)(Future.successful(Right(KnownFacts(testPostCode, testDateOfRegistration))))
            mockAuthRetrieveCredentialAndGroupId(testCredentials, Some(testGroupId))

            val nonMatchingPostcode = "ZZ2 2ZZ"

            val res = await(TestClaimSubscriptionService.claimSubscription(
              vatNumber = testVatNumber,
              businessPostcode = Some(nonMatchingPostcode),
              vatRegistrationDate = Some(testDateOfRegistration),
              isFromBta = true)
            )

            res shouldBe Left(KnownFactsMismatch)
          }
        }
      }
      "auth does not return a valid credential" should {
        "return InvalidCredential" in {
          mockGetKnownFacts(testVatNumber)(Future.successful(Right(KnownFacts(testPostCode, testDateOfRegistration))))
          mockAuthRetrieveCredentialAndGroupId(testCredentials, None)

          val res = TestClaimSubscriptionService.claimSubscription(testVatNumber, None, None, isFromBta = false)

          intercept[ForbiddenException](await(res))
        }
      }
    }
    "the known facts connector returns invalid VAT number" should {
      "return InvalidVatNumber" in {
        mockGetKnownFacts(testVatNumber)(Future.successful(Left(KnownFactsHttpParser.InvalidVatNumber)))

        val res = await(TestClaimSubscriptionService.claimSubscription(testVatNumber, None, None, isFromBta = false))

        res shouldBe Left(InvalidVatNumber)
      }
    }
    "the known facts connector returns VAT number not found" should {
      "return InvalidVatNumber" in {
        mockGetKnownFacts(testVatNumber)(Future.successful(Left(KnownFactsHttpParser.VatNumberNotFound)))

        val res = await(TestClaimSubscriptionService.claimSubscription(testVatNumber, None, None, isFromBta = false))

        res shouldBe Left(VatNumberNotFound)
      }
    }
    "the known facts connector fails" should {
      "return KnownFactsFailure" in {
        mockGetKnownFacts(testVatNumber)(Future.successful(Left(InvalidKnownFacts(
          status = Status.BAD_REQUEST,
          body = ""
        ))))

        val res = await(TestClaimSubscriptionService.claimSubscription(testVatNumber, None, None, isFromBta = false))

        res shouldBe Left(KnownFactsFailure)
      }
    }
  }

  "claimSubscriptionWithEnrolment" when {
    "the known facts connector is successful" when {
      "auth returns a valid ggw credential and group ID" when {
        "tax enrolment to upsert the enrolment is successful" when {
          "tax enrolment to allocate enrolment returns a success" should {
            "return SubscriptionClaimed" in {
              mockGetKnownFacts(testVatNumber)(Future.successful(Right(KnownFacts(testPostCode, testDateOfRegistration))))
              mockAuthRetrieveCredentialAndGroupId(testCredentials, Some(testGroupId))
              mockUpsertEnrolment(testVatNumber, testPostCode, testDateOfRegistration.toTaxEnrolmentsFormat)(
                Future.successful(Right(UpsertEnrolmentSuccess))
              )
              mockAllocateEnrolment(
                testGroupId,
                testCredentialId,
                testVatNumber,
                testPostCode,
                testDateOfRegistration.toTaxEnrolmentsFormat
              )(Future.successful(Right(EnrolSuccess)))

              val res = await(TestClaimSubscriptionService.claimSubscriptionWithEnrolment(
                testVatNumber,
                isFromBta = false
              ))

              res shouldBe Right(SubscriptionClaimed)
              verifyAudit(ClaimSubscriptionAuditModel(
                testVatNumber,
                testPostCode,
                testDateOfRegistration.toTaxEnrolmentsFormat,
                isFromBta = false,
                isSuccess = true
              ))
            }
          }
        }
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
