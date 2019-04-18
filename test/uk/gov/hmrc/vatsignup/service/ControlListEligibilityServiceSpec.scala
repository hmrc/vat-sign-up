/*
 * Copyright 2019 HM Revenue & Customs
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

import play.api.http.Status._
import play.api.mvc.{AnyContent, Request}
import play.api.test.FakeRequest
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.vatsignup.config.mocks.MockEligibilityConfig
import uk.gov.hmrc.vatsignup.connectors.mocks.MockKnownFactsAndControlListInformationConnector
import uk.gov.hmrc.vatsignup.helpers.TestConstants._
import uk.gov.hmrc.vatsignup.httpparsers.KnownFactsAndControlListInformationHttpParser._
import uk.gov.hmrc.vatsignup.models.controllist._
import uk.gov.hmrc.vatsignup.models.monitoring.ControlListAuditing
import uk.gov.hmrc.vatsignup.models.monitoring.ControlListAuditing.ControlListAuditModel
import uk.gov.hmrc.vatsignup.models.{DateRange, KnownFactsAndControlListInformation, MigratableDates}
import uk.gov.hmrc.vatsignup.service.mocks.MockMigrationCheckService
import uk.gov.hmrc.vatsignup.service.mocks.monitoring.MockAuditService
import uk.gov.hmrc.vatsignup.services.ControlListEligibilityService._
import uk.gov.hmrc.vatsignup.services.{ControlListEligibilityService, MigrationCheckService}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ControlListEligibilityServiceSpec extends UnitSpec
  with MockKnownFactsAndControlListInformationConnector with MockAuditService with MockEligibilityConfig with MockMigrationCheckService {

  object TestControlListEligibilityService extends ControlListEligibilityService(
    mockKnownFactsAndControlListInformationConnector,
    mockEligibilityConfig,
    mockMigrationCheckService,
    mockAuditService
  )

  implicit val request: Request[AnyContent] = FakeRequest()
  implicit val hc: HeaderCarrier = HeaderCarrier()

  "getEligibilityStatus" when {
    "the Control List indicates the user is eligible and migratable" when {
      "the user is an overseas trader" should {
        "return EligibilitySuccess with the known facts, a migration status of true and an overseas status of true" in {
          val overseasControlListInformation = ControlListInformation(
            controlList = Set(Stagger1, Company, OverseasTrader),
            stagger = Stagger1,
            businessEntity = Company
          )

          mockGetKnownFactsAndControlListInformation(testVatNumber)(Future.successful(Right(
            KnownFactsAndControlListInformation(testTwoKnownFacts, overseasControlListInformation)
          )))

          mockCheckMigrationRestrictions(
            vatNumber = testVatNumber,
            stagger = Stagger1,
            isDirectDebit = false,
            isMigratable = true
          )(Right(MigrationCheckService.Eligible))

          val res = await(TestControlListEligibilityService.getEligibilityStatus(testVatNumber))
          res shouldBe Right(EligibilitySuccess(testTwoKnownFacts, isMigratable = true, isOverseas = true, isDirectDebit = false))
          verifyAudit(ControlListAuditModel(testVatNumber, isSuccess = true))
        }
      }
      "the user is not an overseas trader" should {
        "return EligibilitySuccess with the known facts, a migration status of true and an overseas status of true" in {
          mockGetKnownFactsAndControlListInformation(testVatNumber)(
            Future.successful(Right(KnownFactsAndControlListInformation(testTwoKnownFacts, testControlListInformation)))
          )

          mockCheckMigrationRestrictions(
            vatNumber = testVatNumber,
            stagger = Stagger1,
            isDirectDebit = false,
            isMigratable = true
          )(Right(MigrationCheckService.Eligible))

          val res = await(TestControlListEligibilityService.getEligibilityStatus(testVatNumber))

          res shouldBe Right(EligibilitySuccess(testTwoKnownFacts, isMigratable = true, isOverseas = false, isDirectDebit = false))
          verifyAudit(ControlListAuditModel(testVatNumber, isSuccess = true))
        }
      }
    }
    "the Control List indicates the user is eligible but non migratable" should {
      "return EligibilitySuccess with the known facts and a migration status of false" in {
        mockNonMigratableParameters(Set(Stagger1))
        mockGetKnownFactsAndControlListInformation(testVatNumber)(Future.successful(Right(
          KnownFactsAndControlListInformation(testTwoKnownFacts, testControlListInformation)
        )))

        mockCheckMigrationRestrictions(
          vatNumber = testVatNumber,
          stagger = Stagger1,
          isDirectDebit = false,
          isMigratable = false
        )(Right(MigrationCheckService.Eligible))

        val res = await(TestControlListEligibilityService.getEligibilityStatus(testVatNumber))
        res shouldBe Right(EligibilitySuccess(testTwoKnownFacts, isMigratable = false, isOverseas = false, isDirectDebit = false))
        verifyAudit(ControlListAuditModel(testVatNumber, isSuccess = true, nonMigratableReasons = Seq(Stagger1.errorMessage)))
      }
    }
    "the control list indicates the user is ineligible with no migratable dates" should {
      "return IneligibleVatNumber" in {
        mockIneligibleParameters(Set(Stagger1))
        mockGetKnownFactsAndControlListInformation(testVatNumber)(Future.successful(Right(
          KnownFactsAndControlListInformation(testTwoKnownFacts, testControlListInformation)
        )))

        mockCheckMigrationRestrictions(
          vatNumber = testVatNumber,
          stagger = Stagger1,
          isDirectDebit = false,
          isMigratable = false
        )(Left(MigratableDates.empty))

        val res = await(TestControlListEligibilityService.getEligibilityStatus(testVatNumber))
        res shouldBe Left(IneligibleVatNumber(MigratableDates.empty))
        verifyAudit(ControlListAuditModel(testVatNumber, isSuccess = false, failureReasons = Seq(Stagger1.errorMessage)))
      }
    }
    "the control list indicates the user ineligible due to direct debit migration restrictions" should {
      "return IneligibleVatNumber" in {
        val testDateRange = DateRange(testMigratableDate, testMigratableDate)

        mockIneligibleParameters(Set.empty)
        mockDirectDebitStaggerParameters(Map(Stagger1 -> Set(testDateRange)))

        val testControlListInformation = ControlListInformation(Set(DirectDebit, Stagger1, Company), Stagger1, Company)

        mockGetKnownFactsAndControlListInformation(testVatNumber)(
          Future.successful(Right(KnownFactsAndControlListInformation(testTwoKnownFacts, testControlListInformation)))
        )

        mockCheckMigrationRestrictions(
          vatNumber = testVatNumber,
          stagger = Stagger1,
          isDirectDebit = true,
          isMigratable = true
        )(Left(testMigratableDates))

        val res = await(TestControlListEligibilityService.getEligibilityStatus(testVatNumber))
        res shouldBe Left(IneligibleVatNumber(testMigratableDates))
      }
    }
    "the control list indicates the user ineligible due to filing date migration restrictions" should {
      "return IneligibleVatNumber" in {
        val testDateRange = DateRange(testMigratableDate, testMigratableDate)

        mockIneligibleParameters(Set.empty)
        mockDirectDebitStaggerParameters(Map(Stagger1 -> Set.empty))
        mockFilingDateStaggerParameters(Map(Stagger1 -> Set(testDateRange)))

        val testControlListInformation = ControlListInformation(Set(DirectDebit, Stagger1, Company), Stagger1, Company)

        mockGetKnownFactsAndControlListInformation(testVatNumber)(
          Future.successful(Right(KnownFactsAndControlListInformation(testTwoKnownFacts, testControlListInformation)))
        )

        mockCheckMigrationRestrictions(
          vatNumber = testVatNumber,
          stagger = Stagger1,
          isDirectDebit = true,
          isMigratable = true
        )(Left(testMigratableDates))

        val res = await(TestControlListEligibilityService.getEligibilityStatus(testVatNumber))
        res shouldBe Left(IneligibleVatNumber(testMigratableDates))
      }
    }
    "the control list indicates the user eligible as the current date is not within any timeframe restrictions" should {
      "return EligibilitySuccess" in {
        val testDateRange = DateRange(testMigratableDate, testMigratableDate)

        mockIneligibleParameters(Set.empty)
        mockDirectDebitStaggerParameters(Map(Stagger1 -> Set(testDateRange)))
        mockFilingDateStaggerParameters(Map(Stagger1 -> Set.empty))

        val testControlListInformation = ControlListInformation(Set(DirectDebit, Stagger1, Company), Stagger1, Company)

        mockGetKnownFactsAndControlListInformation(testVatNumber)(Future.successful(Right(
          KnownFactsAndControlListInformation(testTwoKnownFacts, testControlListInformation)
        )))

        mockCheckMigrationRestrictions(
          vatNumber = testVatNumber,
          stagger = Stagger1,
          isDirectDebit = true,
          isMigratable = true
        )(Right(MigrationCheckService.Eligible))

        val res = await(TestControlListEligibilityService.getEligibilityStatus(testVatNumber))
        res shouldBe Right(EligibilitySuccess(testTwoKnownFacts, isMigratable = true, isOverseas = false, isDirectDebit = true))
        verifyAudit(ControlListAuditModel(testVatNumber, isSuccess = true))
      }
    }
    "the control list returns invalid VAT number" should {
      "return InvalidVatNumber" in {
        mockGetKnownFactsAndControlListInformation(testVatNumber)(
          Future.successful(Left(KnownFactsInvalidVatNumber))
        )

        val res = await(TestControlListEligibilityService.getEligibilityStatus(testVatNumber))
        res shouldBe Left(InvalidVatNumber)
        verifyAudit(ControlListAuditModel(testVatNumber, isSuccess = false, failureReasons = Seq(ControlListAuditing.invalidVatNumber)))
      }
    }
    "the control list returns VAT number not found" should {
      "return InvalidVatNumber" in {
        mockGetKnownFactsAndControlListInformation(testVatNumber)(
          Future.successful(Left(ControlListInformationVatNumberNotFound))
        )

        val res = await(TestControlListEligibilityService.getEligibilityStatus(testVatNumber))
        res shouldBe Left(VatNumberNotFound)
        verifyAudit(ControlListAuditModel(testVatNumber, isSuccess = false, failureReasons = Seq(ControlListAuditing.vatNumberNotFound)))
      }
    }
    "the control list returns any other error" should {
      "return KnownFactsAndControlListFailure" in {
        mockGetKnownFactsAndControlListInformation(testVatNumber)(
          Future.successful(Left(UnexpectedKnownFactsAndControlListInformationFailure(BAD_REQUEST, "")))
        )

        val expectedErr = ControlListAuditing.unexpectedError + s""" {"status":"$BAD_REQUEST","body":""}"""
        val res = await(TestControlListEligibilityService.getEligibilityStatus(testVatNumber))
        res shouldBe Left(KnownFactsAndControlListFailure)
        verifyAudit(ControlListAuditModel(testVatNumber, isSuccess = false, failureReasons = Seq(expectedErr)))
      }
    }
  }
}
