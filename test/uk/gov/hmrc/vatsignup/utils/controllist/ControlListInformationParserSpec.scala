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

package uk.gov.hmrc.vatsignup.utils.controllist

import org.scalatest.EitherValues
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.vatsignup.helpers.TestConstants.{ControlList33, ControlList34}
import uk.gov.hmrc.vatsignup.models.controllist.ControlListIndices._
import uk.gov.hmrc.vatsignup.models.controllist._
import uk.gov.hmrc.vatsignup.utils.controllist.ControlListInformationParser._


class ControlListInformationParserSpec extends UnitSpec with EitherValues {
  "ControlListInformation.tryParse" should {
    "parse into a ControlListInformation object" when {
      "the string is valid" in {
        tryParse(ControlList33.valid).isRight shouldBe true

        tryParse(ControlList34.valid).isRight shouldBe true
      }

      "the string is valid and" should {
        "parse below VAT threshold correctly" in {
          tryParse(ControlList33.setupTestData(BELOW_VAT_THRESHOLD -> '0')).right.value.controlList should contain(BelowVatThreshold)
          tryParse(ControlList33.setupTestData(BELOW_VAT_THRESHOLD -> '1')).right.value.controlList should not contain BelowVatThreshold

          tryParse(ControlList34.setupTestData(BELOW_VAT_THRESHOLD -> '0')).right.value.controlList should contain(BelowVatThreshold)
          tryParse(ControlList34.setupTestData(BELOW_VAT_THRESHOLD -> '1')).right.value.controlList should not contain BelowVatThreshold
        }

        "parse missing returns correctly" in {
          tryParse(ControlList33.setupTestData(MISSING_RETURNS -> '0')).right.value.controlList should contain(MissingReturns)
          tryParse(ControlList33.setupTestData(MISSING_RETURNS -> '1')).right.value.controlList should not contain MissingReturns

          tryParse(ControlList34.setupTestData(MISSING_RETURNS -> '0')).right.value.controlList should contain(MissingReturns)
          tryParse(ControlList34.setupTestData(MISSING_RETURNS -> '1')).right.value.controlList should not contain MissingReturns
        }

        "parse central assessments correctly" in {
          tryParse(ControlList33.setupTestData(CENTRAL_ASSESSMENTS -> '0')).right.value.controlList should contain(CentralAssessments)
          tryParse(ControlList33.setupTestData(CENTRAL_ASSESSMENTS -> '1')).right.value.controlList should not contain CentralAssessments

          tryParse(ControlList34.setupTestData(CENTRAL_ASSESSMENTS -> '0')).right.value.controlList should contain(CentralAssessments)
          tryParse(ControlList34.setupTestData(CENTRAL_ASSESSMENTS -> '1')).right.value.controlList should not contain CentralAssessments
        }

        "parse criminal investigation inhibits correctly" in {
          tryParse(ControlList33.setupTestData(CRIMINAL_INVESTIGATION_INHIBITS -> '0')).right.value.controlList should contain(CriminalInvestigationInhibits)
          tryParse(ControlList33.setupTestData(CRIMINAL_INVESTIGATION_INHIBITS -> '1')).right.value.controlList should not contain CriminalInvestigationInhibits

          tryParse(ControlList34.setupTestData(CRIMINAL_INVESTIGATION_INHIBITS -> '0')).right.value.controlList should contain(CriminalInvestigationInhibits)
          tryParse(ControlList34.setupTestData(CRIMINAL_INVESTIGATION_INHIBITS -> '1')).right.value.controlList should not contain CriminalInvestigationInhibits
        }

        "parse compliance penalities or surcharges correctly" in {
          tryParse(ControlList33.setupTestData(COMPLIANCE_PENALTIES_OR_SURCHARGES -> '0')).right.value.controlList should contain(CompliancePenaltiesOrSurcharges)
          tryParse(ControlList33.setupTestData(COMPLIANCE_PENALTIES_OR_SURCHARGES -> '1')).right.value.controlList should not contain CompliancePenaltiesOrSurcharges

          tryParse(ControlList34.setupTestData(COMPLIANCE_PENALTIES_OR_SURCHARGES -> '0')).right.value.controlList should contain(CompliancePenaltiesOrSurcharges)
          tryParse(ControlList34.setupTestData(COMPLIANCE_PENALTIES_OR_SURCHARGES -> '1')).right.value.controlList should not contain CompliancePenaltiesOrSurcharges
        }

        "parse insolvency correctly" in {
          tryParse(ControlList33.setupTestData(INSOLVENCY -> '0')).right.value.controlList should contain(Insolvency)
          tryParse(ControlList33.setupTestData(INSOLVENCY -> '1')).right.value.controlList should not contain Insolvency

          tryParse(ControlList34.setupTestData(INSOLVENCY -> '0')).right.value.controlList should contain(Insolvency)
          tryParse(ControlList34.setupTestData(INSOLVENCY -> '1')).right.value.controlList should not contain Insolvency
        }

        "parse de-reg/death correctly" in {
          tryParse(ControlList33.setupTestData(DEREG_OR_DEATH -> '0')).right.value.controlList should contain(DeRegOrDeath)
          tryParse(ControlList33.setupTestData(DEREG_OR_DEATH -> '1')).right.value.controlList should not contain DeRegOrDeath

          tryParse(ControlList34.setupTestData(DEREG_OR_DEATH -> '0')).right.value.controlList should contain(DeRegOrDeath)
          tryParse(ControlList34.setupTestData(DEREG_OR_DEATH -> '1')).right.value.controlList should not contain DeRegOrDeath
        }

        "parse debt migration correctly" in {
          tryParse(ControlList33.setupTestData(DEBT_MIGRATION -> '0')).right.value.controlList should contain(DebtMigration)
          tryParse(ControlList33.setupTestData(DEBT_MIGRATION -> '1')).right.value.controlList should not contain DebtMigration

          tryParse(ControlList34.setupTestData(DEBT_MIGRATION -> '0')).right.value.controlList should contain(DebtMigration)
          tryParse(ControlList34.setupTestData(DEBT_MIGRATION -> '1')).right.value.controlList should not contain DebtMigration
        }

        "parse direct debit correctly" in {
          tryParse(ControlList33.setupTestData(DIRECT_DEBIT -> '0')).right.value.controlList should contain(DirectDebit)
          tryParse(ControlList33.setupTestData(DIRECT_DEBIT -> '1')).right.value.controlList should not contain DirectDebit

          tryParse(ControlList34.setupTestData(DIRECT_DEBIT -> '0')).right.value.controlList should contain(DirectDebit)
          tryParse(ControlList34.setupTestData(DIRECT_DEBIT -> '1')).right.value.controlList should not contain DirectDebit
        }

        "parse large business correctly" in {
          tryParse(ControlList33.setupTestData(LARGE_BUSINESS -> '0')).right.value.controlList should contain(LargeBusiness)
          tryParse(ControlList33.setupTestData(LARGE_BUSINESS -> '1')).right.value.controlList should not contain LargeBusiness

          tryParse(ControlList34.setupTestData(LARGE_BUSINESS -> '0')).right.value.controlList should contain(LargeBusiness)
          tryParse(ControlList34.setupTestData(LARGE_BUSINESS -> '1')).right.value.controlList should not contain LargeBusiness
        }

        "parse missing trader correctly" in {
          tryParse(ControlList33.setupTestData(MISSING_TRADER -> '0')).right.value.controlList should contain(MissingTrader)
          tryParse(ControlList33.setupTestData(MISSING_TRADER -> '1')).right.value.controlList should not contain MissingTrader

          tryParse(ControlList34.setupTestData(MISSING_TRADER -> '0')).right.value.controlList should contain(MissingTrader)
          tryParse(ControlList34.setupTestData(MISSING_TRADER -> '1')).right.value.controlList should not contain MissingTrader
        }

        "parse EU sales/purchases  correctly" in {
          tryParse(ControlList33.setupTestData(EU_SALES_OR_PURCHASES -> '0')).right.value.controlList should contain(EuSalesOrPurchases)
          tryParse(ControlList33.setupTestData(EU_SALES_OR_PURCHASES -> '1')).right.value.controlList should not contain EuSalesOrPurchases

          tryParse(ControlList34.setupTestData(EU_SALES_OR_PURCHASES -> '0')).right.value.controlList should contain(EuSalesOrPurchases)
          tryParse(ControlList34.setupTestData(EU_SALES_OR_PURCHASES -> '1')).right.value.controlList should not contain EuSalesOrPurchases
        }

        "parse Stagger without none standard tax period  correctly" in {
          tryParse(ControlList33.setupTestData(STAGGER_1 -> '1', ANNUAL_STAGGER -> '0')).right.value.controlList should contain(AnnualStagger)
          tryParse(ControlList33.setupTestData(STAGGER_1 -> '1', MONTHLY_STAGGER -> '0')).right.value.controlList should contain(MonthlyStagger)
          tryParse(ControlList33.setupTestData(STAGGER_1 -> '0')).right.value.controlList should contain(Stagger1)
          tryParse(ControlList33.setupTestData(STAGGER_1 -> '1', STAGGER_2 -> '0')).right.value.controlList should contain(Stagger2)
          tryParse(ControlList33.setupTestData(STAGGER_1 -> '1', STAGGER_3 -> '0')).right.value.controlList should contain(Stagger3)

          tryParse(ControlList34.setupTestData(STAGGER_1 -> '1', ANNUAL_STAGGER -> '0')).right.value.controlList should contain(AnnualStagger)
          tryParse(ControlList34.setupTestData(STAGGER_1 -> '1', MONTHLY_STAGGER -> '0')).right.value.controlList should contain(MonthlyStagger)
          tryParse(ControlList34.setupTestData(STAGGER_1 -> '0')).right.value.controlList should contain(Stagger1)
          tryParse(ControlList34.setupTestData(STAGGER_1 -> '1', STAGGER_2 -> '0')).right.value.controlList should contain(Stagger2)
          tryParse(ControlList34.setupTestData(STAGGER_1 -> '1', STAGGER_3 -> '0')).right.value.controlList should contain(Stagger3)
        }

        "parse Stagger with none standard tax period correctly" in {
          tryParse(ControlList33.setupTestData(STAGGER_1 -> '1', NONE_STANDARD_TAX_PERIOD -> '0')).right.value.controlList should contain(NonStandardTaxPeriod)
          tryParse(ControlList33.setupTestData(STAGGER_1 -> '0', NONE_STANDARD_TAX_PERIOD -> '0')).right.value.controlList should contain(Stagger1)
          tryParse(ControlList33.setupTestData(STAGGER_1 -> '1', STAGGER_2 -> '0', NONE_STANDARD_TAX_PERIOD -> '0')).right.value.controlList should contain(Stagger2)
          tryParse(ControlList33.setupTestData(STAGGER_1 -> '1', STAGGER_3 -> '0', NONE_STANDARD_TAX_PERIOD -> '0')).right.value.controlList should contain(Stagger3)
          tryParse(ControlList33.setupTestData(STAGGER_1 -> '1', ANNUAL_STAGGER -> '0', NONE_STANDARD_TAX_PERIOD -> '0')).right.value.controlList should contain(AnnualStagger)
          tryParse(ControlList33.setupTestData(STAGGER_1 -> '1', MONTHLY_STAGGER -> '0', NONE_STANDARD_TAX_PERIOD -> '0')).right.value.controlList should contain(MonthlyStagger)
          tryParse(ControlList33.setupTestData(STAGGER_1 -> '1', NONE_STANDARD_TAX_PERIOD -> '1')) shouldBe Left(StaggerConflict)
          tryParse(ControlList33.setupTestData(STAGGER_1 -> '1', ANNUAL_STAGGER -> '0', MONTHLY_STAGGER -> '0', NONE_STANDARD_TAX_PERIOD -> '0')) shouldBe Left(StaggerConflict)

          tryParse(ControlList34.setupTestData(STAGGER_1 -> '1', NONE_STANDARD_TAX_PERIOD -> '0')).right.value.controlList should contain(NonStandardTaxPeriod)
          tryParse(ControlList34.setupTestData(STAGGER_1 -> '0', NONE_STANDARD_TAX_PERIOD -> '0')).right.value.controlList should contain(Stagger1)
          tryParse(ControlList34.setupTestData(STAGGER_1 -> '1', STAGGER_2 -> '0', NONE_STANDARD_TAX_PERIOD -> '0')).right.value.controlList should contain(Stagger2)
          tryParse(ControlList34.setupTestData(STAGGER_1 -> '1', STAGGER_3 -> '0', NONE_STANDARD_TAX_PERIOD -> '0')).right.value.controlList should contain(Stagger3)
          tryParse(ControlList34.setupTestData(STAGGER_1 -> '1', ANNUAL_STAGGER -> '0', NONE_STANDARD_TAX_PERIOD -> '0')).right.value.controlList should contain(AnnualStagger)
          tryParse(ControlList34.setupTestData(STAGGER_1 -> '1', MONTHLY_STAGGER -> '0', NONE_STANDARD_TAX_PERIOD -> '0')).right.value.controlList should contain(MonthlyStagger)
          tryParse(ControlList34.setupTestData(STAGGER_1 -> '1', NONE_STANDARD_TAX_PERIOD -> '1')) shouldBe Left(StaggerConflict)
          tryParse(ControlList34.setupTestData(STAGGER_1 -> '1', ANNUAL_STAGGER -> '0', MONTHLY_STAGGER -> '0', NONE_STANDARD_TAX_PERIOD -> '0')) shouldBe Left(StaggerConflict)
        }

        "parse over seas trader correctly" in {
          tryParse(ControlList33.setupTestData(OVERSEAS_TRADER -> '0')).right.value.controlList should contain(OverseasTrader)
          tryParse(ControlList33.setupTestData(OVERSEAS_TRADER -> '1')).right.value.controlList should not contain OverseasTrader

          tryParse(ControlList34.setupTestData(OVERSEAS_TRADER -> '0')).right.value.controlList should contain(OverseasTrader)
          tryParse(ControlList34.setupTestData(OVERSEAS_TRADER -> '1')).right.value.controlList should not contain OverseasTrader
        }

        "parse POA trader correctly" in {
          tryParse(ControlList33.setupTestData(POA_TRADER -> '0')).right.value.controlList should contain(PoaTrader)
          tryParse(ControlList33.setupTestData(POA_TRADER -> '1')).right.value.controlList should not contain PoaTrader

          tryParse(ControlList34.setupTestData(POA_TRADER -> '0')).right.value.controlList should contain(PoaTrader)
          tryParse(ControlList34.setupTestData(POA_TRADER -> '1')).right.value.controlList should not contain PoaTrader
        }

        "parse Business Entity correctly" in {
          tryParse(ControlList33.setupTestData(COMPANY -> '0')).right.value.controlList should contain(Company)
          tryParse(ControlList33.setupTestData(COMPANY -> '1', DIVISION -> '0')).right.value.controlList should contain(Division)
          tryParse(ControlList33.setupTestData(COMPANY -> '1', GROUP -> '0')).right.value.controlList should contain(Group)
          tryParse(ControlList33.setupTestData(COMPANY -> '0', GROUP -> '0')).right.value.controlList should contain(Group)
          tryParse(ControlList33.setupTestData(COMPANY -> '1', PARTNERSHIP -> '0')).right.value.controlList should contain(Partnership)
          tryParse(ControlList33.setupTestData(COMPANY -> '1', PUBLIC_CORPORATION -> '0')).right.value.controlList should contain(PublicCorporation)
          tryParse(ControlList33.setupTestData(COMPANY -> '1', SOLE_TRADER -> '0')).right.value.controlList should contain(SoleTrader)
          tryParse(ControlList33.setupTestData(COMPANY -> '1', LOCAL_AUTHORITY -> '0')).right.value.controlList should contain(LocalAuthority)
          tryParse(ControlList33.setupTestData(COMPANY -> '1', NON_PROFIT -> '0')).right.value.controlList should contain(NonProfitMakingBody)

          tryParse(ControlList34.setupTestData(COMPANY -> '0')).right.value.controlList should contain(Company)
          tryParse(ControlList34.setupTestData(COMPANY -> '1', DIVISION -> '0')).right.value.controlList should contain(Division)
          tryParse(ControlList34.setupTestData(COMPANY -> '1', GROUP -> '0')).right.value.controlList should contain(Group)
          tryParse(ControlList34.setupTestData(COMPANY -> '0', GROUP -> '0')).right.value.controlList should contain(Group)
          tryParse(ControlList34.setupTestData(COMPANY -> '1', PARTNERSHIP -> '0')).right.value.controlList should contain(Partnership)
          tryParse(ControlList34.setupTestData(COMPANY -> '1', PUBLIC_CORPORATION -> '0')).right.value.controlList should contain(PublicCorporation)
          tryParse(ControlList34.setupTestData(COMPANY -> '1', SOLE_TRADER -> '0')).right.value.controlList should contain(SoleTrader)
          tryParse(ControlList34.setupTestData(COMPANY -> '1', LOCAL_AUTHORITY -> '0')).right.value.controlList should contain(LocalAuthority)
          tryParse(ControlList34.setupTestData(COMPANY -> '1', NON_PROFIT -> '0')).right.value.controlList should contain(NonProfitMakingBody)
        }

        "parse DIFIC trader correctly" in {
          tryParse(ControlList33.setupTestData(DIFIC_TRADER -> '0')).right.value.controlList should contain(DificTrader)
          tryParse(ControlList33.setupTestData(DIFIC_TRADER -> '1')).right.value.controlList should not contain DificTrader

          tryParse(ControlList34.setupTestData(DIFIC_TRADER -> '0')).right.value.controlList should contain(DificTrader)
          tryParse(ControlList34.setupTestData(DIFIC_TRADER -> '1')).right.value.controlList should not contain DificTrader
        }

        "parse anything under appeal correctly" in {
          tryParse(ControlList33.setupTestData(ANYTHING_UNDER_APPEAL -> '0')).right.value.controlList should contain(AnythingUnderAppeal)
          tryParse(ControlList33.setupTestData(ANYTHING_UNDER_APPEAL -> '1')).right.value.controlList should not contain AnythingUnderAppeal

          tryParse(ControlList34.setupTestData(ANYTHING_UNDER_APPEAL -> '0')).right.value.controlList should contain(AnythingUnderAppeal)
          tryParse(ControlList34.setupTestData(ANYTHING_UNDER_APPEAL -> '1')).right.value.controlList should not contain AnythingUnderAppeal
        }

        "parse repayment trader correctly" in {
          tryParse(ControlList33.setupTestData(REPAYMENT_TRADER -> '0')).right.value.controlList should contain(RepaymentTrader)
          tryParse(ControlList33.setupTestData(REPAYMENT_TRADER -> '1')).right.value.controlList should not contain RepaymentTrader

          tryParse(ControlList34.setupTestData(REPAYMENT_TRADER -> '0')).right.value.controlList should contain(RepaymentTrader)
          tryParse(ControlList34.setupTestData(REPAYMENT_TRADER -> '1')).right.value.controlList should not contain RepaymentTrader
        }

        "parse MOSS trader correctly" in {
          tryParse(ControlList33.setupTestData(MOSS_TRADER -> '0')).right.value.controlList should contain(MossTrader)
          tryParse(ControlList33.setupTestData(MOSS_TRADER -> '1')).right.value.controlList should not contain MossTrader

          tryParse(ControlList34.setupTestData(MOSS_TRADER -> '0')).right.value.controlList should contain(MossTrader)
          tryParse(ControlList34.setupTestData(MOSS_TRADER -> '1')).right.value.controlList should not contain MossTrader
        }

        "parse Flat rate correctly" in {
          tryParse(ControlList33.setupTestData(FLAT_RATE -> '0')).right.value.controlList should contain(FlatRate)
          tryParse(ControlList33.setupTestData(FLAT_RATE -> '1')).right.value.controlList should not contain FlatRate

          tryParse(ControlList34.setupTestData(FLAT_RATE -> '0')).right.value.controlList should contain(FlatRate)
          tryParse(ControlList34.setupTestData(FLAT_RATE -> '1')).right.value.controlList should not contain FlatRate
        }

        "parse pending oa correctly" in {
          tryParse(ControlList34.setupTestData(PENDING_OA -> '0')).right.value.controlList should contain(PendingOA)
          tryParse(ControlList34.setupTestData(PENDING_OA -> '1')).right.value.controlList should not contain PendingOA
        }
      }
    }

    "fail and return InvalidFormat" when {
      "the string is not 33 or 34 characters long" in {
        tryParse(ControlList33.valid.drop(1)) shouldBe Left(InvalidFormat)
      }
      "the string does not represent a binary number" in {
        tryParse(ControlList33.valid.replaceFirst("1", "2")) shouldBe Left(InvalidFormat)

        tryParse(ControlList34.valid.replaceFirst("1", "2")) shouldBe Left(InvalidFormat)
      }
    }
    "fail and return StaggerConflict" when {
      "multiple stagger is defined" in {
        tryParse(ControlList33.staggerConflict) shouldBe Left(StaggerConflict)

        tryParse(ControlList34.staggerConflict) shouldBe Left(StaggerConflict)
      }
    }
  }

}
