/*
 * Copyright 2021 HM Revenue & Customs
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

package uk.gov.hmrc.vatsignup.helpers

import java.time.{LocalDate, Month}
import java.util.UUID

import uk.gov.hmrc.auth.core.Enrolment
import uk.gov.hmrc.auth.core.retrieve.Credentials
import uk.gov.hmrc.vatsignup.config.Constants.Des._
import uk.gov.hmrc.vatsignup.config.Constants.TaxEnrolments._
import uk.gov.hmrc.vatsignup.config.Constants._
import uk.gov.hmrc.vatsignup.models.SignUpRequest.EmailAddress
import uk.gov.hmrc.vatsignup.models._
import uk.gov.hmrc.vatsignup.models.controllist.ControlListIndices._
import uk.gov.hmrc.vatsignup.models.controllist._
import uk.gov.hmrc.vatsignup.services.AgentClientRelationshipService.{LegacyRelationship, MtdVatRelationship}
import uk.gov.hmrc.vatsignup.services.ClaimSubscriptionService.GGProviderId

object  TestConstants {
  val testVatNumber: String = UUID.randomUUID().toString
  val testNino: String = UUID.randomUUID().toString
  val testCompanyNumber: String = UUID.randomUUID().toString
  val testCtReference: String = UUID.randomUUID().toString
  val testUtr: String = UUID.randomUUID().toString
  val testEmail: String = UUID.randomUUID().toString
  val testPasscode: String = UUID.randomUUID().toString
  val testAgentReferenceNumber: String = UUID.randomUUID().toString
  val testSafeId: String = UUID.randomUUID().toString
  val testToken: String = UUID.randomUUID().toString
  val testJourneyLink = s"/mdtp/journey/journeyId/${UUID.randomUUID().toString}"
  val testLimitedCompany = LimitedCompany(testCompanyNumber)
  val testGeneralPartnership = GeneralPartnership(Some(testUtr))
  val testNoUtrGeneralPartnership = GeneralPartnership(None)
  val testLimitedPartnership = LimitedPartnership(Some(testUtr), testCompanyNumber)
  val testLimitedLiabilityPartnership = LimitedLiabilityPartnership(Some(testUtr), testCompanyNumber)
  val testScottishLimitedPartnership = ScottishLimitedPartnership(Some(testUtr), testCompanyNumber)
  val testSoleTrader = uk.gov.hmrc.vatsignup.models.SoleTrader(testNino)
  val testSignUpEmail = EmailAddress(testEmail, isVerified = true)
  val testRegisteredSociety = RegisteredSociety(testCompanyNumber)
  val testNonUkNoEstablishmentCompanyNumber = "FC123321"
  val testContactPreference = Digital
  val testMtdVatRelationship = MtdVatRelationship
  val testLegacyRelationship = LegacyRelationship


  val testCredentialId: String = UUID.randomUUID().toString
  val testCredentialId2: String = UUID.randomUUID().toString
  val testCredentialId3: String = UUID.randomUUID().toString
  val testCredentials: Credentials = Credentials(testCredentialId, GGProviderId)
  val testGroupId: String = UUID.randomUUID().toString
  val testUserIdSet: Set[String] = Set(testGroupId, testGroupId, testGroupId)

  val testPostCode = "ZZ11 1ZZ"
  val testDateOfRegistration = "2017-01-01"
  val testRecentDateOfRegistration: String = LocalDate.now().toString
  val testLastReturnMonthPeriod: String = "MAR"
  val testLastNetDue: String = "10000.02"

  val testAgentEnrolment: Enrolment = Enrolment(AgentEnrolmentKey).withIdentifier(AgentReferenceNumberKey, testAgentReferenceNumber)
  val testPrincipalEnrolment: Enrolment = Enrolment(VatDecEnrolmentKey).withIdentifier(VatReferenceKey, testVatNumber)
  val testPrincipalMtdEnrolment: Enrolment = Enrolment(MtdEnrolmentKey).withIdentifier(VrnKey, testVatNumber)
  val testPrincipalVatEnrolment: Enrolment = Enrolment(VatMtdEnrolmentKey).withIdentifier(VatReferenceKey, testVatNumber)


  def testPartnershipEnrolment(sautr: String): Enrolment =
    Enrolment(PartnershipIrsaEnrolmentKey).withIdentifier(PartnershipIrsaReferenceNumberKey, sautr)

  val testPartnershipEnrolment: Enrolment = testPartnershipEnrolment(testUtr)

  val testErrorMsg = "this is an error"

  val testCustomerDetails = CustomerDetails(Some("testFirstName"),
    Some("testLastName"),
    Some("testOrganisationName"),
    Some("testTradingName"))

  val testControlListInformation = ControlListInformation(
    controlList = Set(Stagger1, Company),
    stagger = Stagger1
  )

  val testFourKnownFacts = VatKnownFacts(
    businessPostcode = Some(testPostCode),
    vatRegistrationDate = testDateOfRegistration,
    lastReturnMonthPeriod = Some(Month.MARCH),
    lastNetDue = Some(testLastNetDue)
  )

  val testTwoKnownFacts = VatKnownFacts(
    businessPostcode = Some(testPostCode),
    vatRegistrationDate = testDateOfRegistration,
    lastReturnMonthPeriod = None,
    lastNetDue = None
  )

  val testKnownFactsAndControlListInformation = KnownFactsAndControlListInformation(
    VatKnownFacts(
      Some(testPostCode),
      testDateOfRegistration,
      Some(Month.MARCH),
      Some(testLastNetDue)
    ),
    testControlListInformation
  )

  object ControlList34 {
    val allFalse: String = "1" * CONTROL_INFORMATION34_STRING_LENGTH
    val valid: String = setupTestDataCore(allFalse)(STAGGER_1 -> '0', COMPANY -> '0')
    val businessEntityConflict: String = setupTestData(COMPANY -> '0', SOLE_TRADER -> '0')
    val staggerConflict: String = setupTestData(ANNUAL_STAGGER -> '0', STAGGER_1 -> '0')

    def setupTestData(amendments: (Int, Character)*): String = setupTestDataCore(valid)(amendments: _*)

    private def setupTestDataCore(startString: String)(amendments: (Int, Character)*): String = {
      require(amendments.forall { case (index, _) => index >= 0 && index < CONTROL_INFORMATION34_STRING_LENGTH })
      require(amendments.forall { case (_, newValue) => newValue == '0' || newValue == '1' })

      amendments.foldLeft[String](startString) {
        case (pre: String, (index: Int, value: Character)) =>
          pre.substring(0, index) + value + pre.substring(index + 1, pre.length)
      }
    }
  }

  object ControlList35 {
    val allFalse: String = "1" * CONTROL_INFORMATION35_STRING_LENGTH
    val valid: String = setupTestDataCore(allFalse)(STAGGER_1 -> '0', COMPANY -> '0')
    val businessEntityConflict: String = setupTestData(COMPANY -> '0', SOLE_TRADER -> '0')
    val staggerConflict: String = setupTestData(ANNUAL_STAGGER -> '0', STAGGER_1 -> '0')

    def setupTestData(amendments: (Int, Character)*): String = setupTestDataCore(valid)(amendments: _*)

    private def setupTestDataCore(startString: String)(amendments: (Int, Character)*): String = {
      require(amendments.forall { case (index, _) => index >= 0 && index < CONTROL_INFORMATION35_STRING_LENGTH })
      require(amendments.forall { case (_, newValue) => newValue == '0' || newValue == '1' })

      amendments.foldLeft[String](startString) {
        case (pre: String, (index: Int, value: Character)) =>
          pre.substring(0, index) + value + pre.substring(index + 1, pre.length)
      }
    }
  }

  val testDDConfig: Map[Stagger, Set[DateRange]] = Map(
    Stagger1 -> Set(
      DateRange(LocalDate.of(2018, 10, 18), LocalDate.of(2018, 11, 13)),
      DateRange(LocalDate.of(2019, 1, 18), LocalDate.of(2019, 2, 13))
    ),
    Stagger2 -> Set(
      DateRange(LocalDate.of(2018, 11, 17), LocalDate.of(2018, 12, 13)),
      DateRange(LocalDate.of(2019, 2, 15), LocalDate.of(2019, 3, 13))
    ),
    Stagger3 -> Set(
      DateRange(LocalDate.of(2018, 12, 13), LocalDate.of(2019, 1, 13)),
      DateRange(LocalDate.of(2019, 3, 16), LocalDate.of(2019, 4, 11))
    )
  )

  val testFilingDateConfig: Map[Stagger, Set[DateRange]] = Map(
    Stagger1 -> Set(
      DateRange(LocalDate.of(2018, 10, 18), LocalDate.of(2018, 11, 13)),
      DateRange(LocalDate.of(2019, 1, 18), LocalDate.of(2019, 2, 13))
    ),
    Stagger2 -> Set(
      DateRange(LocalDate.of(2018, 11, 17), LocalDate.of(2018, 12, 13)),
      DateRange(LocalDate.of(2019, 2, 15), LocalDate.of(2019, 3, 13))
    ),
    Stagger3 -> Set(
      DateRange(LocalDate.of(2018, 12, 13), LocalDate.of(2019, 1, 13)),
      DateRange(LocalDate.of(2019, 3, 16), LocalDate.of(2019, 4, 11))
    ),
    MonthlyStagger -> Set(
      DateRange(LocalDate.of(2018, 10, 18), LocalDate.of(2018, 11, 13)),
      DateRange(LocalDate.of(2019, 1, 18), LocalDate.of(2019, 2, 13))
    )
  )

  val testMigratableDate: LocalDate = LocalDate.now()
  val testMigratableDates: MigratableDates = MigratableDates(Some(testMigratableDate), Some(testMigratableDate))

  val testCorrespondencePostCode: String = UUID.randomUUID().toString
  val testBasePostCode: String = UUID.randomUUID().toString
  val testCommsPostCode: String = UUID.randomUUID().toString
  val testTraderPostCode: String = UUID.randomUUID().toString

  val testVatCustomerDetails = VatCustomerDetails(
    KnownFacts(Some(testPostCode), testDateOfRegistration),
    isOverseas = false
  )
}
