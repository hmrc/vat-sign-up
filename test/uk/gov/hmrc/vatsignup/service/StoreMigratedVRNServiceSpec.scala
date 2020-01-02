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

import play.api.test.FakeRequest
import reactivemongo.api.commands.UpdateWriteResult
import uk.gov.hmrc.auth.core.{Enrolment, Enrolments}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.HeaderCarrierConverter
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.vatsignup.config.Constants.Des.VrnKey
import uk.gov.hmrc.vatsignup.config.Constants.TaxEnrolments.MtdEnrolmentKey
import uk.gov.hmrc.vatsignup.config.Constants.{VatDecEnrolmentKey, VatReferenceKey}
import uk.gov.hmrc.vatsignup.helpers.TestConstants._
import uk.gov.hmrc.vatsignup.models.VatKnownFacts
import uk.gov.hmrc.vatsignup.repositories.mocks.MockSubscriptionRequestRepository
import uk.gov.hmrc.vatsignup.service.mocks.{MockAgentClientRelationshipService, MockMigratedKnownFactsMatchingService}
import uk.gov.hmrc.vatsignup.services.AgentClientRelationshipService.{RelationshipCheckError, RelationshipCheckNotFound, RelationshipCheckSuccess}
import uk.gov.hmrc.vatsignup.services.StoreMigratedVRNService._
import uk.gov.hmrc.vatsignup.services._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class StoreMigratedVRNServiceSpec
  extends UnitSpec with MockSubscriptionRequestRepository with MockMigratedKnownFactsMatchingService with MockAgentClientRelationshipService {

  object TestStoreMigratedVRNService extends StoreMigratedVRNService(
    mockSubscriptionRequestRepository,
    mockMigratedKnownFactsMatchingService,
    mockAgentClientRelationshipService
  )

  implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromHeadersAndSession(FakeRequest().headers)
  implicit val request = FakeRequest("POST", "testUrl")

  val successfulWriteResult: UpdateWriteResult = UpdateWriteResult(ok = true, 0, 0, Seq(), Seq(), None, None, None)
  val failedWriteResult: UpdateWriteResult = UpdateWriteResult(ok = false, 0, 0, Seq(), Seq(), None, None, None)

  val testPrincipalDecEnrolment = Enrolment(VatDecEnrolmentKey).withIdentifier(VatReferenceKey, testVatNumber)
  val testPrincipalMtdEnrolment = Enrolment(MtdEnrolmentKey).withIdentifier(VrnKey, testVatNumber)

  val testEnrolmentLegacy = Enrolments(Set(testPrincipalDecEnrolment))
  val testEnrolmentMtd = Enrolments(Set(testPrincipalMtdEnrolment))
  val testEnrolmentLegacyAndMtd = Enrolments(Set(testPrincipalMtdEnrolment, testPrincipalDecEnrolment))
  val testEnrolmentAgent = Enrolments(Set(testAgentEnrolment))
  val testEnrolmentEmpty = Enrolments(Set.empty)

  val test2KnownFacts = VatKnownFacts(Some(testPostCode), testDateOfRegistration, None, None)

  "storeVatNumber" when {
    "the user has an agent enrolment" should {
      "store the vat number" when {
        "the agent client relationship check passes" in {
          mockUpsertVatNumber(testVatNumber, isMigratable = true, isDirectDebit = false)(Future.successful(successfulWriteResult))
          mockCheckAgentClientRelationship(
            vatNumber = testVatNumber,
            agentReferenceNumber = testAgentReferenceNumber
          )(Future.successful(Right(RelationshipCheckSuccess)))

          val res = await(TestStoreMigratedVRNService.storeVatNumber(testVatNumber, testEnrolmentAgent))

          res shouldBe Right(StoreMigratedVRNSuccess)
        }
      }
      "not store the vat number" when {
        "the agent client relationship check returns not found" in {
          mockCheckAgentClientRelationship(
            vatNumber = testVatNumber,
            agentReferenceNumber = testAgentReferenceNumber
          )(Future.successful(Left(RelationshipCheckNotFound)))

          val res = await(TestStoreMigratedVRNService.storeVatNumber(testVatNumber, testEnrolmentAgent))

          res shouldBe Left(AgentClientRelationshipNotFound)
        }
        "the agent client relationship check fails" in {
          mockCheckAgentClientRelationship(
            vatNumber = testVatNumber,
            agentReferenceNumber = testAgentReferenceNumber
          )(Future.successful(Left(RelationshipCheckError)))

          val res = await(TestStoreMigratedVRNService.storeVatNumber(testVatNumber, testEnrolmentAgent))

          res shouldBe Left(AgentClientRelationshipFailure)
        }
      }
    }
    "the user has a vat enrolment" should {
      "store the vat number" when {
        "both enrolment VRNs match the request VRN and the upsert is successful" in {
          mockUpsertVatNumber(testVatNumber, isMigratable = true, isDirectDebit = false)(Future.successful(successfulWriteResult))

          val res = await(TestStoreMigratedVRNService.storeVatNumber(testVatNumber, testEnrolmentLegacyAndMtd))
          res shouldBe Right(StoreMigratedVRNSuccess)
        }
        "legacy enrolment VRN matches the request VRN and the upsert is successful" in {
          mockUpsertVatNumber(testVatNumber, isMigratable = true, isDirectDebit = false)(Future.successful(successfulWriteResult))

          val res = await(TestStoreMigratedVRNService.storeVatNumber(testVatNumber, testEnrolmentLegacy))
          res shouldBe Right(StoreMigratedVRNSuccess)
        }
        "mtd enrolment VRN matches the request VRN and the upsert is successful" in {
          mockUpsertVatNumber(testVatNumber, isMigratable = true, isDirectDebit = false)(Future.successful(successfulWriteResult))

          val res = await(TestStoreMigratedVRNService.storeVatNumber(testVatNumber, testEnrolmentMtd))
          res shouldBe Right(StoreMigratedVRNSuccess)
        }
      }
      "not store the vat number" when {
        "mtd enrolment VRN match the request VRN but the upsert fails" should {
          "return Left(UpsertMigratedVRNFailure)" in {
            mockUpsertVatNumber(testVatNumber, isMigratable = true, isDirectDebit = false)(Future.successful(failedWriteResult))

            val res = await(TestStoreMigratedVRNService.storeVatNumber(testVatNumber, testEnrolmentMtd))
            res shouldBe Left(UpsertMigratedVRNFailure)
          }
        }
        "legacy VRN mismatches mtd VRN" should {
          "return Left(VatNumberDoesNotMatch)" in {
            val testPrincipalMtdEnrolment: Enrolment = Enrolment(MtdEnrolmentKey).withIdentifier(VrnKey, testVatNumber + "1")
            val vatEnrolments = Enrolments(Set(testPrincipalMtdEnrolment, testPrincipalDecEnrolment))
            mockUpsertVatNumber(testVatNumber, isMigratable = true, isDirectDebit = false)(Future.successful(successfulWriteResult))

            val res = await(TestStoreMigratedVRNService.storeVatNumber(testVatNumber, vatEnrolments))
            res shouldBe Left(VatNumberDoesNotMatch)
          }
        }
        "there is no enrolment VRN matches provided VRN" should {
          "return Left(NoVatEnrolment)" in {
            val vatEnrolments = Enrolments(Set.empty)
            mockUpsertVatNumber(testVatNumber, isMigratable = true, isDirectDebit = false)(Future.successful(successfulWriteResult))

            val res = await(TestStoreMigratedVRNService.storeVatNumber(testVatNumber, vatEnrolments))
            res shouldBe Left(NoVatEnrolment)
          }
        }
        "provided VRN does not match the enrolment VRN" should {
          "return Left(VatNumberDoesNotMatch)" in {
            val vatEnrolments = Enrolments(Set(testPrincipalDecEnrolment))
            mockUpsertVatNumber(testVatNumber, isMigratable = true, isDirectDebit = false)(Future.successful(successfulWriteResult))

            val res = await(TestStoreMigratedVRNService.storeVatNumber("123456782", vatEnrolments))
            res shouldBe Left(VatNumberDoesNotMatch)
          }
        }
      }
    }
    "the user has entered known facts" when {
      "the post code and vat registration date match the entered known facts" should {
        "return StoreMigratedVRNSuccess" in {
          mockCheckKnownFactsMatch(testVatNumber, test2KnownFacts)(Future.successful(true))
          mockUpsertVatNumber(testVatNumber, isMigratable = true, isDirectDebit = false)(Future.successful(successfulWriteResult))

          val res = await(TestStoreMigratedVRNService.storeVatNumber(testVatNumber, testEnrolmentEmpty, Some(test2KnownFacts)))
          res shouldBe Right(StoreMigratedVRNSuccess)
        }
      }
      "the post code doesn't match the entered known facts" should {
        "return KnownFactsMismatch" in {
          val mismatchedKnownFacts = VatKnownFacts(Some("1234"), testDateOfRegistration, None, None)
          mockCheckKnownFactsMatch(testVatNumber, mismatchedKnownFacts)(Future.successful(false))
          mockUpsertVatNumber(testVatNumber, isMigratable = true, isDirectDebit = false)(Future.successful(successfulWriteResult))

          val res = await(TestStoreMigratedVRNService.storeVatNumber(testVatNumber, testEnrolmentEmpty, Some(mismatchedKnownFacts)))
          res shouldBe Left(KnownFactsMismatch)
        }
      }
      "the registration date doesn't match the entered known facts" should {
        "return KnownFactsMismatch" in {
          val mismatchedKnownFacts = VatKnownFacts(Some(testPostCode), "1234", None, None)
          mockCheckKnownFactsMatch(testVatNumber, mismatchedKnownFacts)(Future.successful(false))
          mockUpsertVatNumber(testVatNumber, isMigratable = true, isDirectDebit = false)(Future.successful(successfulWriteResult))

          val res = await(TestStoreMigratedVRNService.storeVatNumber(testVatNumber, testEnrolmentEmpty, Some(mismatchedKnownFacts)))
          res shouldBe Left(KnownFactsMismatch)
        }
      }
      "the known facts match, but the upsert fails" should {
        "return KnownFactsMismatch" in {
          mockCheckKnownFactsMatch(testVatNumber, test2KnownFacts)(Future.successful(false))
          mockUpsertVatNumber(testVatNumber, isMigratable = true, isDirectDebit = false)(Future.successful(failedWriteResult))

          val res = await(TestStoreMigratedVRNService.storeVatNumber(testVatNumber, testEnrolmentEmpty, Some(test2KnownFacts)))
          res shouldBe Left(KnownFactsMismatch)
        }
      }
    }
  }
}
