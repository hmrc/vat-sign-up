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

package uk.gov.hmrc.vatsignup.models

import cats.data.NonEmptyList
import cats.data.Validated.Invalid
import org.scalatest.enablers.Containing
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.vatsignup.config.EligibilityConfig
import uk.gov.hmrc.vatsignup.models.ControlListInformation._
import uk.gov.hmrc.vatsignup.utils.controllist.ControlListIneligibilityMessages._

class ControlListInformationSpec extends UnitSpec {

  val allFalseEligibilityConfig = EligibilityConfig(
    false, false, false, false, false, false, false, false,
    false, false, false, false, false, false, false, false,
    false, false, false, false, false, false, false, false,
    false, false, false, false, false, false, false, false
  )

  val allTrueEligibilityConfig = EligibilityConfig(
    true, true, true, true, true, true, true, true,
    true, true, true, true, true, true, true, true,
    true, true, true, true, true, true, true, true,
    true, true, true, true, true, true, true, true
  )

  private val testControlList = ControlListInformation(
    belowVatThreshold = false,
    missingReturns = false,
    centralAssessments = false,
    criminalInvestigationInhibits = false,
    compliancePenaltiesOrSurcharges = false,
    insolvency = false,
    deRegOrDeath = false,
    debtMigration = false,
    directDebit = false,
    euSalesOrPurchases = false,
    largeBusiness = false,
    missingTrader = false,
    staggerType = Stagger1,
    nonStandardTaxPeriod = false,
    overseasTrader = false,
    poaTrader = false,
    entityType = Company,
    dificTrader = false,
    anythingUnderAppeal = false,
    repaymentTrader = false,
    mossTrader = false
  )

  private val testAllTrueControlList = ControlListInformation(
    belowVatThreshold = true,
    missingReturns = true,
    centralAssessments = true,
    criminalInvestigationInhibits = true,
    compliancePenaltiesOrSurcharges = true,
    insolvency = true,
    deRegOrDeath = true,
    debtMigration = true,
    directDebit = true,
    euSalesOrPurchases = true,
    largeBusiness = true,
    missingTrader = true,
    staggerType = Stagger1,
    nonStandardTaxPeriod = true,
    overseasTrader = true,
    poaTrader = true,
    entityType = Company,
    dificTrader = true,
    anythingUnderAppeal = true,
    repaymentTrader = true,
    mossTrader = true
  )

  implicit val t = new Containing[ValidatedType] {
    override def contains(container: ValidatedType, element: Any): Boolean = (container, element) match {
      case (Invalid(err: NonEmptyList[_]), Invalid(expected: NonEmptyList[_])) =>
        expected.forall(exp => err.exists(x => x == exp))
      case (Invalid(err: NonEmptyList[_]), msg: String) =>
        err.exists(x => x == msg)
      case _ => false
    }

    override def containsOneOf(container: ValidatedType, elements: Seq[Any]): Boolean = ???

    override def containsNoneOf(container: ValidatedType, elements: Seq[Any]): Boolean = ???
  }

  private def specificFlagTest(controlListWithFlagSetToFalse: ControlListInformation, expectedValidationErrorMessage: String) = {
    val invalid = ineligible(expectedValidationErrorMessage)
    "config permits the flag then it is eligible regardless" in {
      controlListWithFlagSetToFalse.validate(allTrueEligibilityConfig) should not contain invalid
      testAllTrueControlList.validate(allTrueEligibilityConfig) should not contain invalid
    }
    "config is set to does not permit then it is eligible only if it the control list has it set to false" in {
      controlListWithFlagSetToFalse.validate(allFalseEligibilityConfig) should not contain invalid
      testAllTrueControlList.validate(allFalseEligibilityConfig) should contain(invalid)
    }
  }

  "validate" should {
    "return valid if all eligibility rules passed according to its config" in {
      val testConfig = allFalseEligibilityConfig.copy(permitStagger1 = true, permitCompany = true)
      testControlList.validate(testConfig) shouldBe eligible
    }
    "return valid if all eligibility criteria are permitted in the config" in {
      testAllTrueControlList.validate(allTrueEligibilityConfig) shouldBe eligible
    }

    "validate belowVatThreshold correctly according to the eligibility config" when {
      val testEligibleCase = testAllTrueControlList.copy(belowVatThreshold = false)
      val expectedValidationErrorMessage = belowVatThresholdMessage

      specificFlagTest(testEligibleCase, expectedValidationErrorMessage)
    }
    "validate missingReturns correctly according to the eligibility config" when {
      val testEligibleCase = testAllTrueControlList.copy(missingReturns = false)
      val expectedValidationErrorMessage = missingReturnsMessage

      specificFlagTest(testEligibleCase, expectedValidationErrorMessage)
    }
    "validate centralAssessments correctly according to the eligibility config" when {
      val testEligibleCase = testAllTrueControlList.copy(centralAssessments = false)
      val expectedValidationErrorMessage = centralAssessmentsMessage

      specificFlagTest(testEligibleCase, expectedValidationErrorMessage)
    }
    "validate criminalInvestigationInhibits correctly according to the eligibility config" when {
      val testEligibleCase = testAllTrueControlList.copy(criminalInvestigationInhibits = false)
      val expectedValidationErrorMessage = criminalInvestigationInhibitsMessage

      specificFlagTest(testEligibleCase, expectedValidationErrorMessage)
    }
    "validate compliancePenaltiesOrSurcharges correctly according to the eligibility config" when {
      val testEligibleCase = testAllTrueControlList.copy(compliancePenaltiesOrSurcharges = false)
      val expectedValidationErrorMessage = compliancePenaltiesOrSurchargesMessage

      specificFlagTest(testEligibleCase, expectedValidationErrorMessage)
    }
    "validate insolvency correctly according to the eligibility config" when {
      val testEligibleCase = testAllTrueControlList.copy(insolvency = false)
      val expectedValidationErrorMessage = insolvencyMessage

      specificFlagTest(testEligibleCase, expectedValidationErrorMessage)
    }
    "validate deRegOrDeath correctly according to the eligibility config" when {
      val testEligibleCase = testAllTrueControlList.copy(deRegOrDeath = false)
      val expectedValidationErrorMessage = deRegOrDeathMessage

      specificFlagTest(testEligibleCase, expectedValidationErrorMessage)
    }
    "validate debtMigration correctly according to the eligibility config" when {
      val testEligibleCase = testAllTrueControlList.copy(debtMigration = false)
      val expectedValidationErrorMessage = debtMigrationMessage

      specificFlagTest(testEligibleCase, expectedValidationErrorMessage)
    }
    "validate directDebit correctly according to the eligibility config" when {
      val testEligibleCase = testAllTrueControlList.copy(directDebit = false)
      val expectedValidationErrorMessage = directDebitMessage

      specificFlagTest(testEligibleCase, expectedValidationErrorMessage)
    }
    "validate euSalesOrPurchases correctly according to the eligibility config" when {
      val testEligibleCase = testAllTrueControlList.copy(euSalesOrPurchases = false)
      val expectedValidationErrorMessage = euSalesOrPurchasesMessage

      specificFlagTest(testEligibleCase, expectedValidationErrorMessage)
    }
    "validate largeBusiness correctly according to the eligibility config" when {
      val testEligibleCase = testAllTrueControlList.copy(largeBusiness = false)
      val expectedValidationErrorMessage = largeBusinessMessage

      specificFlagTest(testEligibleCase, expectedValidationErrorMessage)
    }
    "validate missingTrader correctly according to the eligibility config" when {
      val testEligibleCase = testAllTrueControlList.copy(missingTrader = false)
      val expectedValidationErrorMessage = missingTraderMessage

      specificFlagTest(testEligibleCase, expectedValidationErrorMessage)
    }
    "validate nonStandardTaxPeriod correctly according to the eligibility config" when {
      val testEligibleCase = testAllTrueControlList.copy(nonStandardTaxPeriod = false)
      val expectedValidationErrorMessage = nonStandardTaxPeriodMessage

      specificFlagTest(testEligibleCase, expectedValidationErrorMessage)
    }
    "validate overseasTrader correctly according to the eligibility config" when {
      val testEligibleCase = testAllTrueControlList.copy(overseasTrader = false)
      val expectedValidationErrorMessage = overseasTraderMessage

      specificFlagTest(testEligibleCase, expectedValidationErrorMessage)
    }
    "validate poaTrader correctly according to the eligibility config" when {
      val testEligibleCase = testAllTrueControlList.copy(poaTrader = false)
      val expectedValidationErrorMessage = poaTraderMessage

      specificFlagTest(testEligibleCase, expectedValidationErrorMessage)
    }
    "validate dificTrader correctly according to the eligibility config" when {
      val testEligibleCase = testAllTrueControlList.copy(dificTrader = false)
      val expectedValidationErrorMessage = dificTraderMessage

      specificFlagTest(testEligibleCase, expectedValidationErrorMessage)
    }
    "validate anythingUnderAppeal correctly according to the eligibility config" when {
      val testEligibleCase = testAllTrueControlList.copy(anythingUnderAppeal = false)
      val expectedValidationErrorMessage = anythingUnderAppealMessage

      specificFlagTest(testEligibleCase, expectedValidationErrorMessage)
    }
    "validate repaymentTrader correctly according to the eligibility config" when {
      val testEligibleCase = testAllTrueControlList.copy(repaymentTrader = false)
      val expectedValidationErrorMessage = repaymentTraderMessage

      specificFlagTest(testEligibleCase, expectedValidationErrorMessage)
    }
    "validate mossTrader correctly according to the eligibility config" when {
      val testEligibleCase = testAllTrueControlList.copy(mossTrader = false)
      val expectedValidationErrorMessage = mossTraderMessage

      specificFlagTest(testEligibleCase, expectedValidationErrorMessage)
    }

    "validate annualStagger correctly according to the eligibility config" when {
      val testEligibleCase = testAllTrueControlList.copy(staggerType = AnnualStagger)
      val expectedValidationErrorMessage = invalidStaggerTypeMessage(AnnualStagger)

      val invalid = ineligible(expectedValidationErrorMessage)
      "config permits the flag then it is eligible regardless" in {
        testEligibleCase.validate(allTrueEligibilityConfig) should not contain invalid
        testAllTrueControlList.validate(allTrueEligibilityConfig) should not contain invalid
      }
      "config is set to does not permit then it is eligible only if it the control list does not have it set to AnnualStagger" in {
        testAllTrueControlList.copy(staggerType = MonthlyStagger).validate(allFalseEligibilityConfig) should not contain invalid
        testAllTrueControlList.copy(staggerType = Stagger1).validate(allFalseEligibilityConfig) should not contain invalid
        testAllTrueControlList.copy(staggerType = Stagger2).validate(allFalseEligibilityConfig) should not contain invalid
        testAllTrueControlList.copy(staggerType = Stagger3).validate(allFalseEligibilityConfig) should not contain invalid
      }
    }
    "validate monthlyStagger correctly according to the eligibility config" when {
      val testEligibleCase = testAllTrueControlList.copy(staggerType = MonthlyStagger)
      val expectedValidationErrorMessage = invalidStaggerTypeMessage(MonthlyStagger)

      val invalid = ineligible(expectedValidationErrorMessage)
      "config permits the flag then it is eligible regardless" in {
        testEligibleCase.validate(allTrueEligibilityConfig) should not contain invalid
        testAllTrueControlList.validate(allTrueEligibilityConfig) should not contain invalid
      }
      "config is set to does not permit then it is eligible only if it the control list does not have it set to MonthlyStagger" in {
        testAllTrueControlList.copy(staggerType = AnnualStagger).validate(allFalseEligibilityConfig) should not contain invalid
        testAllTrueControlList.copy(staggerType = Stagger1).validate(allFalseEligibilityConfig) should not contain invalid
        testAllTrueControlList.copy(staggerType = Stagger2).validate(allFalseEligibilityConfig) should not contain invalid
        testAllTrueControlList.copy(staggerType = Stagger3).validate(allFalseEligibilityConfig) should not contain invalid
      }
    }
    "validate stagger 1 correctly according to the eligibility config" when {
      val testEligibleCase = testAllTrueControlList.copy(staggerType = Stagger1)
      val expectedValidationErrorMessage = invalidStaggerTypeMessage(Stagger1)

      val invalid = ineligible(expectedValidationErrorMessage)
      "config permits the flag then it is eligible regardless" in {
        testEligibleCase.validate(allTrueEligibilityConfig) should not contain invalid
        testAllTrueControlList.validate(allTrueEligibilityConfig) should not contain invalid
      }
      "config is set to does not permit then it is eligible only if it the control list does not have it set to Stagger1" in {
        testAllTrueControlList.copy(staggerType = AnnualStagger).validate(allFalseEligibilityConfig) should not contain invalid
        testAllTrueControlList.copy(staggerType = MonthlyStagger).validate(allFalseEligibilityConfig) should not contain invalid
        testAllTrueControlList.copy(staggerType = Stagger2).validate(allFalseEligibilityConfig) should not contain invalid
        testAllTrueControlList.copy(staggerType = Stagger3).validate(allFalseEligibilityConfig) should not contain invalid
      }
    }
    "validate stagger 2 correctly according to the eligibility config" when {
      val testEligibleCase = testAllTrueControlList.copy(staggerType = Stagger2)
      val expectedValidationErrorMessage = invalidStaggerTypeMessage(Stagger2)

      val invalid = ineligible(expectedValidationErrorMessage)
      "config permits the flag then it is eligible regardless" in {
        testEligibleCase.validate(allTrueEligibilityConfig) should not contain invalid
        testAllTrueControlList.validate(allTrueEligibilityConfig) should not contain invalid
      }
      "config is set to does not permit then it is eligible only if it the control list does not have it set to Stagger2" in {
        testAllTrueControlList.copy(staggerType = AnnualStagger).validate(allFalseEligibilityConfig) should not contain invalid
        testAllTrueControlList.copy(staggerType = MonthlyStagger).validate(allFalseEligibilityConfig) should not contain invalid
        testAllTrueControlList.copy(staggerType = Stagger1).validate(allFalseEligibilityConfig) should not contain invalid
        testAllTrueControlList.copy(staggerType = Stagger3).validate(allFalseEligibilityConfig) should not contain invalid
      }
    }
    "validate stagger 3 correctly according to the eligibility config" when {
      val testEligibleCase = testAllTrueControlList.copy(staggerType = Stagger3)
      val expectedValidationErrorMessage = invalidStaggerTypeMessage(Stagger3)

      val invalid = ineligible(expectedValidationErrorMessage)
      "config permits the flag then it is eligible regardless" in {
        testEligibleCase.validate(allTrueEligibilityConfig) should not contain invalid
        testAllTrueControlList.validate(allTrueEligibilityConfig) should not contain invalid
      }
      "config is set to does not permit then it is eligible only if it the control list does not have it set to Stagger3" in {
        testAllTrueControlList.copy(staggerType = AnnualStagger).validate(allFalseEligibilityConfig) should not contain invalid
        testAllTrueControlList.copy(staggerType = MonthlyStagger).validate(allFalseEligibilityConfig) should not contain invalid
        testAllTrueControlList.copy(staggerType = Stagger1).validate(allFalseEligibilityConfig) should not contain invalid
        testAllTrueControlList.copy(staggerType = Stagger2).validate(allFalseEligibilityConfig) should not contain invalid
      }
    }

    "validate Company correctly according to the eligibility config" when {
      val testEligibleCase = testAllTrueControlList.copy(entityType = Company)
      val expectedValidationErrorMessage = invalidEntityTypeMessage(Company)

      val invalid = ineligible(expectedValidationErrorMessage)
      "config permits the flag then it is eligible regardless" in {
        testEligibleCase.validate(allTrueEligibilityConfig) should not contain invalid
        testAllTrueControlList.validate(allTrueEligibilityConfig) should not contain invalid
      }
      "config is set to does not permit then it is eligible only if it the control list does not have it set to Company" in {
        testAllTrueControlList.copy(entityType = Division).validate(allFalseEligibilityConfig) should not contain invalid
        testAllTrueControlList.copy(entityType = Group).validate(allFalseEligibilityConfig) should not contain invalid
        testAllTrueControlList.copy(entityType = Partnership).validate(allFalseEligibilityConfig) should not contain invalid
        testAllTrueControlList.copy(entityType = PublicCorporation).validate(allFalseEligibilityConfig) should not contain invalid
        testAllTrueControlList.copy(entityType = SoleTrader).validate(allFalseEligibilityConfig) should not contain invalid
        testAllTrueControlList.copy(entityType = LocalAuthority).validate(allFalseEligibilityConfig) should not contain invalid
        testAllTrueControlList.copy(entityType = NonProfitMakingBody).validate(allFalseEligibilityConfig) should not contain invalid
      }
    }
    "validate Division correctly according to the eligibility config" when {
      val testEligibleCase = testAllTrueControlList.copy(entityType = Division)
      val expectedValidationErrorMessage = invalidEntityTypeMessage(Division)

      val invalid = ineligible(expectedValidationErrorMessage)
      "config permits the flag then it is eligible regardless" in {
        testEligibleCase.validate(allTrueEligibilityConfig) should not contain invalid
        testAllTrueControlList.validate(allTrueEligibilityConfig) should not contain invalid
      }
      "config is set to does not permit then it is eligible only if it the control list does not have it set to Division" in {
        testAllTrueControlList.copy(entityType = Company).validate(allFalseEligibilityConfig) should not contain invalid
        testAllTrueControlList.copy(entityType = Group).validate(allFalseEligibilityConfig) should not contain invalid
        testAllTrueControlList.copy(entityType = Partnership).validate(allFalseEligibilityConfig) should not contain invalid
        testAllTrueControlList.copy(entityType = PublicCorporation).validate(allFalseEligibilityConfig) should not contain invalid
        testAllTrueControlList.copy(entityType = SoleTrader).validate(allFalseEligibilityConfig) should not contain invalid
        testAllTrueControlList.copy(entityType = LocalAuthority).validate(allFalseEligibilityConfig) should not contain invalid
        testAllTrueControlList.copy(entityType = NonProfitMakingBody).validate(allFalseEligibilityConfig) should not contain invalid
      }
    }
    "validate Group correctly according to the eligibility config" when {
      val testEligibleCase = testAllTrueControlList.copy(entityType = Group)
      val expectedValidationErrorMessage = invalidEntityTypeMessage(Group)

      val invalid = ineligible(expectedValidationErrorMessage)
      "config permits the flag then it is eligible regardless" in {
        testEligibleCase.validate(allTrueEligibilityConfig) should not contain invalid
        testAllTrueControlList.validate(allTrueEligibilityConfig) should not contain invalid
      }
      "config is set to does not permit then it is eligible only if it the control list does not have it set to Group" in {
        testAllTrueControlList.copy(entityType = Company).validate(allFalseEligibilityConfig) should not contain invalid
        testAllTrueControlList.copy(entityType = Division).validate(allFalseEligibilityConfig) should not contain invalid
        testAllTrueControlList.copy(entityType = Partnership).validate(allFalseEligibilityConfig) should not contain invalid
        testAllTrueControlList.copy(entityType = PublicCorporation).validate(allFalseEligibilityConfig) should not contain invalid
        testAllTrueControlList.copy(entityType = SoleTrader).validate(allFalseEligibilityConfig) should not contain invalid
        testAllTrueControlList.copy(entityType = LocalAuthority).validate(allFalseEligibilityConfig) should not contain invalid
        testAllTrueControlList.copy(entityType = NonProfitMakingBody).validate(allFalseEligibilityConfig) should not contain invalid
      }
    }
    "validate Partnership correctly according to the eligibility config" when {
      val testEligibleCase = testAllTrueControlList.copy(entityType = Partnership)
      val expectedValidationErrorMessage = invalidEntityTypeMessage(Partnership)

      val invalid = ineligible(expectedValidationErrorMessage)
      "config permits the flag then it is eligible regardless" in {
        testEligibleCase.validate(allTrueEligibilityConfig) should not contain invalid
        testAllTrueControlList.validate(allTrueEligibilityConfig) should not contain invalid
      }
      "config is set to does not permit then it is eligible only if it the control list does not have it set to Partnership" in {
        testAllTrueControlList.copy(entityType = Company).validate(allFalseEligibilityConfig) should not contain invalid
        testAllTrueControlList.copy(entityType = Division).validate(allFalseEligibilityConfig) should not contain invalid
        testAllTrueControlList.copy(entityType = Group).validate(allFalseEligibilityConfig) should not contain invalid
        testAllTrueControlList.copy(entityType = PublicCorporation).validate(allFalseEligibilityConfig) should not contain invalid
        testAllTrueControlList.copy(entityType = SoleTrader).validate(allFalseEligibilityConfig) should not contain invalid
        testAllTrueControlList.copy(entityType = LocalAuthority).validate(allFalseEligibilityConfig) should not contain invalid
        testAllTrueControlList.copy(entityType = NonProfitMakingBody).validate(allFalseEligibilityConfig) should not contain invalid
      }
    }
    "validate PublicCorporation correctly according to the eligibility config" when {
      val testEligibleCase = testAllTrueControlList.copy(entityType = PublicCorporation)
      val expectedValidationErrorMessage = invalidEntityTypeMessage(PublicCorporation)

      val invalid = ineligible(expectedValidationErrorMessage)
      "config permits the flag then it is eligible regardless" in {
        testEligibleCase.validate(allTrueEligibilityConfig) should not contain invalid
        testAllTrueControlList.validate(allTrueEligibilityConfig) should not contain invalid
      }
      "config is set to does not permit then it is eligible only if it the control list does not have it set to PublicCorporation" in {
        testAllTrueControlList.copy(entityType = Company).validate(allFalseEligibilityConfig) should not contain invalid
        testAllTrueControlList.copy(entityType = Division).validate(allFalseEligibilityConfig) should not contain invalid
        testAllTrueControlList.copy(entityType = Group).validate(allFalseEligibilityConfig) should not contain invalid
        testAllTrueControlList.copy(entityType = Partnership).validate(allFalseEligibilityConfig) should not contain invalid
        testAllTrueControlList.copy(entityType = SoleTrader).validate(allFalseEligibilityConfig) should not contain invalid
        testAllTrueControlList.copy(entityType = LocalAuthority).validate(allFalseEligibilityConfig) should not contain invalid
        testAllTrueControlList.copy(entityType = NonProfitMakingBody).validate(allFalseEligibilityConfig) should not contain invalid
      }
    }
    "validate SoleTrader correctly according to the eligibility config" when {
      val testEligibleCase = testAllTrueControlList.copy(entityType = SoleTrader)
      val expectedValidationErrorMessage = invalidEntityTypeMessage(SoleTrader)

      val invalid = ineligible(expectedValidationErrorMessage)
      "config permits the flag then it is eligible regardless" in {
        testEligibleCase.validate(allTrueEligibilityConfig) should not contain invalid
        testAllTrueControlList.validate(allTrueEligibilityConfig) should not contain invalid
      }
      "config is set to does not permit then it is eligible only if it the control list does not have it set to SoleTrader" in {
        testAllTrueControlList.copy(entityType = Company).validate(allFalseEligibilityConfig) should not contain invalid
        testAllTrueControlList.copy(entityType = Division).validate(allFalseEligibilityConfig) should not contain invalid
        testAllTrueControlList.copy(entityType = Group).validate(allFalseEligibilityConfig) should not contain invalid
        testAllTrueControlList.copy(entityType = Partnership).validate(allFalseEligibilityConfig) should not contain invalid
        testAllTrueControlList.copy(entityType = PublicCorporation).validate(allFalseEligibilityConfig) should not contain invalid
        testAllTrueControlList.copy(entityType = LocalAuthority).validate(allFalseEligibilityConfig) should not contain invalid
        testAllTrueControlList.copy(entityType = NonProfitMakingBody).validate(allFalseEligibilityConfig) should not contain invalid
      }
    }
    "validate LocalAuthority correctly according to the eligibility config" when {
      val testEligibleCase = testAllTrueControlList.copy(entityType = LocalAuthority)
      val expectedValidationErrorMessage = invalidEntityTypeMessage(LocalAuthority)

      val invalid = ineligible(expectedValidationErrorMessage)
      "config permits the flag then it is eligible regardless" in {
        testEligibleCase.validate(allTrueEligibilityConfig) should not contain invalid
        testAllTrueControlList.validate(allTrueEligibilityConfig) should not contain invalid
      }
      "config is set to does not permit then it is eligible only if it the control list does not have it set to LocalAuthority" in {
        testAllTrueControlList.copy(entityType = Company).validate(allFalseEligibilityConfig) should not contain invalid
        testAllTrueControlList.copy(entityType = Division).validate(allFalseEligibilityConfig) should not contain invalid
        testAllTrueControlList.copy(entityType = Group).validate(allFalseEligibilityConfig) should not contain invalid
        testAllTrueControlList.copy(entityType = Partnership).validate(allFalseEligibilityConfig) should not contain invalid
        testAllTrueControlList.copy(entityType = PublicCorporation).validate(allFalseEligibilityConfig) should not contain invalid
        testAllTrueControlList.copy(entityType = SoleTrader).validate(allFalseEligibilityConfig) should not contain invalid
        testAllTrueControlList.copy(entityType = NonProfitMakingBody).validate(allFalseEligibilityConfig) should not contain invalid
      }
    }
    "validate NonProfitMakingBody correctly according to the eligibility config" when {
      val testEligibleCase = testAllTrueControlList.copy(entityType = NonProfitMakingBody)
      val expectedValidationErrorMessage = invalidEntityTypeMessage(NonProfitMakingBody)

      val invalid = ineligible(expectedValidationErrorMessage)
      "config permits the flag then it is eligible regardless" in {
        testEligibleCase.validate(allTrueEligibilityConfig) should not contain invalid
        testAllTrueControlList.validate(allTrueEligibilityConfig) should not contain invalid
      }
      "config is set to does not permit then it is eligible only if it the control list does not have it set to NonProfitMakingBody" in {
        testAllTrueControlList.copy(entityType = Company).validate(allFalseEligibilityConfig) should not contain invalid
        testAllTrueControlList.copy(entityType = Division).validate(allFalseEligibilityConfig) should not contain invalid
        testAllTrueControlList.copy(entityType = Group).validate(allFalseEligibilityConfig) should not contain invalid
        testAllTrueControlList.copy(entityType = Partnership).validate(allFalseEligibilityConfig) should not contain invalid
        testAllTrueControlList.copy(entityType = PublicCorporation).validate(allFalseEligibilityConfig) should not contain invalid
        testAllTrueControlList.copy(entityType = SoleTrader).validate(allFalseEligibilityConfig) should not contain invalid
        testAllTrueControlList.copy(entityType = LocalAuthority).validate(allFalseEligibilityConfig) should not contain invalid
      }
    }
  }

}
