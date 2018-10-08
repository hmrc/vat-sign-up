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

package uk.gov.hmrc.vatsignup.utils.controllist

import org.scalatest.EitherValues
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.vatsignup.helpers.TestConstants.ControlList._
import uk.gov.hmrc.vatsignup.models.controllist.ControlListIndices._
import uk.gov.hmrc.vatsignup.models.controllist._
import uk.gov.hmrc.vatsignup.utils.controllist.ControlListInformationParser._


class ControlListInformationParserSpec extends UnitSpec with EitherValues {
  "ControlListInformation.tryParse" should {
    "parse into a ControlListInformation object" when {
      "the string is valid" in {
        tryParse(valid).isRight shouldBe true
      }

      "the string is valid and" should {
        "parse below VAT threshold correctly" in {
          tryParse(setupTestData(BELOW_VAT_THRESHOLD -> '0')).right.value.controlList should contain(BelowVatThreshold)
          tryParse(setupTestData(BELOW_VAT_THRESHOLD -> '1')).right.value.controlList should not contain BelowVatThreshold
        }

        "parse missing returns correctly" in {
          tryParse(setupTestData(MISSING_RETURNS -> '0')).right.value.controlList should contain(MissingReturns)
          tryParse(setupTestData(MISSING_RETURNS -> '1')).right.value.controlList should not contain MissingReturns
        }

        "parse central assessments correctly" in {
          tryParse(setupTestData(CENTRAL_ASSESSMENTS -> '0')).right.value.controlList should contain(CentralAssessments)
          tryParse(setupTestData(CENTRAL_ASSESSMENTS -> '1')).right.value.controlList should not contain CentralAssessments
        }

        "parse criminal investigation inhibits correctly" in {
          tryParse(setupTestData(CRIMINAL_INVESTIGATION_INHIBITS -> '0')).right.value.controlList should contain(CriminalInvestigationInhibits)
          tryParse(setupTestData(CRIMINAL_INVESTIGATION_INHIBITS -> '1')).right.value.controlList should not contain CriminalInvestigationInhibits
        }

        "parse compliance penalities or surcharges correctly" in {
          tryParse(setupTestData(COMPLIANCE_PENALTIES_OR_SURCHARGES -> '0')).right.value.controlList should contain(CompliancePenaltiesOrSurcharges)
          tryParse(setupTestData(COMPLIANCE_PENALTIES_OR_SURCHARGES -> '1')).right.value.controlList should not contain CompliancePenaltiesOrSurcharges
        }

        "parse insolvency correctly" in {
          tryParse(setupTestData(INSOLVENCY -> '0')).right.value.controlList should contain(Insolvency)
          tryParse(setupTestData(INSOLVENCY -> '1')).right.value.controlList should not contain Insolvency
        }

        "parse de-reg/death correctly" in {
          tryParse(setupTestData(DEREG_OR_DEATH -> '0')).right.value.controlList should contain(DeRegOrDeath)
          tryParse(setupTestData(DEREG_OR_DEATH -> '1')).right.value.controlList should not contain DeRegOrDeath
        }

        "parse debt migration correctly" in {
          tryParse(setupTestData(DEBT_MIGRATION -> '0')).right.value.controlList should contain(DebtMigration)
          tryParse(setupTestData(DEBT_MIGRATION -> '1')).right.value.controlList should not contain DebtMigration
        }

        "parse direct debit correctly" in {
          tryParse(setupTestData(DIRECT_DEBIT -> '0')).right.value.controlList should contain(DirectDebit)
          tryParse(setupTestData(DIRECT_DEBIT -> '1')).right.value.controlList should not contain DirectDebit
        }

        "parse large business correctly" in {
          tryParse(setupTestData(LARGE_BUSINESS -> '0')).right.value.controlList should contain(LargeBusiness)
          tryParse(setupTestData(LARGE_BUSINESS -> '1')).right.value.controlList should not contain LargeBusiness
        }

        "parse missing trader correctly" in {
          tryParse(setupTestData(MISSING_TRADER -> '0')).right.value.controlList should contain(MissingTrader)
          tryParse(setupTestData(MISSING_TRADER -> '1')).right.value.controlList should not contain MissingTrader
        }

        "parse EU sales/purchases  correctly" in {
          tryParse(setupTestData(EU_SALES_OR_PURCHASES -> '0')).right.value.controlList should contain(EuSalesOrPurchases)
          tryParse(setupTestData(EU_SALES_OR_PURCHASES -> '1')).right.value.controlList should not contain EuSalesOrPurchases
        }

        "parse Stagger without none standard tax period  correctly" in {
          tryParse(setupTestData(STAGGER_1 -> '1', ANNUAL_STAGGER -> '0')).right.value.controlList should contain(AnnualStagger)
          tryParse(setupTestData(STAGGER_1 -> '1', MONTHLY_STAGGER -> '0')).right.value.controlList should contain(MonthlyStagger)
          tryParse(setupTestData(STAGGER_1 -> '0')).right.value.controlList should contain(Stagger1)
          tryParse(setupTestData(STAGGER_1 -> '1', STAGGER_2 -> '0')).right.value.controlList should contain(Stagger2)
          tryParse(setupTestData(STAGGER_1 -> '1', STAGGER_3 -> '0')).right.value.controlList should contain(Stagger3)
        }

        "parse Stagger with none standard tax period correctly" in {
          tryParse(setupTestData(STAGGER_1 -> '1', NONE_STANDARD_TAX_PERIOD -> '0')).right.value.controlList should contain(NonStandardTaxPeriod)
          tryParse(setupTestData(STAGGER_1 -> '0', NONE_STANDARD_TAX_PERIOD -> '0')).right.value.controlList should contain(Stagger1)
          tryParse(setupTestData(STAGGER_1 -> '1', STAGGER_2 -> '0', NONE_STANDARD_TAX_PERIOD -> '0')).right.value.controlList should contain(Stagger2)
          tryParse(setupTestData(STAGGER_1 -> '1', STAGGER_3 -> '0', NONE_STANDARD_TAX_PERIOD -> '0')).right.value.controlList should contain(Stagger3)
          tryParse(setupTestData(STAGGER_1 -> '1', ANNUAL_STAGGER -> '0', NONE_STANDARD_TAX_PERIOD -> '0')).right.value.controlList should contain(AnnualStagger)
          tryParse(setupTestData(STAGGER_1 -> '1', MONTHLY_STAGGER -> '0', NONE_STANDARD_TAX_PERIOD -> '0')).right.value.controlList should contain(MonthlyStagger)

          tryParse(setupTestData(STAGGER_1 -> '1', NONE_STANDARD_TAX_PERIOD -> '1')) shouldBe Left(StaggerConflict)
          tryParse(setupTestData(STAGGER_1 -> '1', ANNUAL_STAGGER -> '0', MONTHLY_STAGGER -> '0', NONE_STANDARD_TAX_PERIOD -> '0')) shouldBe Left(StaggerConflict)
        }

        "parse over seas trader correctly" in {
          tryParse(setupTestData(OVERSEAS_TRADER -> '0')).right.value.controlList should contain(OverseasTrader)
          tryParse(setupTestData(OVERSEAS_TRADER -> '1')).right.value.controlList should not contain OverseasTrader
        }

        "parse POA trader correctly" in {
          tryParse(setupTestData(POA_TRADER -> '0')).right.value.controlList should contain(PoaTrader)
          tryParse(setupTestData(POA_TRADER -> '1')).right.value.controlList should not contain PoaTrader
        }

        "parse Business Entity correctly" in {
          tryParse(setupTestData(COMPANY -> '0')).right.value.controlList should contain(Company)
          tryParse(setupTestData(COMPANY -> '1', DIVISION -> '0')).right.value.controlList should contain(Division)
          tryParse(setupTestData(COMPANY -> '1', GROUP -> '0')).right.value.controlList should contain(Group)
          tryParse(setupTestData(COMPANY -> '1', PARTNERSHIP -> '0')).right.value.controlList should contain(Partnership)
          tryParse(setupTestData(COMPANY -> '1', PUBLIC_CORPORATION -> '0')).right.value.controlList should contain(PublicCorporation)
          tryParse(setupTestData(COMPANY -> '1', SOLE_TRADER -> '0')).right.value.controlList should contain(SoleTrader)
          tryParse(setupTestData(COMPANY -> '1', LOCAL_AUTHORITY -> '0')).right.value.controlList should contain(LocalAuthority)
          tryParse(setupTestData(COMPANY -> '1', NON_PROFIT -> '0')).right.value.controlList should contain(NonProfitMakingBody)
        }

        "parse DIFIC trader correctly" in {
          tryParse(setupTestData(DIFIC_TRADER -> '0')).right.value.controlList should contain(DificTrader)
          tryParse(setupTestData(DIFIC_TRADER -> '1')).right.value.controlList should not contain DificTrader
        }

        "parse anything under appeal correctly" in {
          tryParse(setupTestData(ANYTHING_UNDER_APPEAL -> '0')).right.value.controlList should contain(AnythingUnderAppeal)
          tryParse(setupTestData(ANYTHING_UNDER_APPEAL -> '1')).right.value.controlList should not contain AnythingUnderAppeal
        }

        "parse repayment trader correctly" in {
          tryParse(setupTestData(REPAYMENT_TRADER -> '0')).right.value.controlList should contain(RepaymentTrader)
          tryParse(setupTestData(REPAYMENT_TRADER -> '1')).right.value.controlList should not contain RepaymentTrader
        }

        "parse MOSS trader correctly" in {
          tryParse(setupTestData(MOSS_TRADER -> '0')).right.value.controlList should contain(MossTrader)
          tryParse(setupTestData(MOSS_TRADER -> '1')).right.value.controlList should not contain MossTrader
        }
      }
    }

    "fail and return InvalidFormat" when {
      "the string is not exactly 32 characters long" in {
        tryParse(valid.drop(1)) shouldBe Left(InvalidFormat)
      }
      "the string does not represent a binary number" in {
        tryParse(valid.replaceFirst("1", "2")) shouldBe Left(InvalidFormat)
      }
    }
    "fail and return EntityConflict" when {
      "multiple business entity is defined" in {
        tryParse(businessEntityConflict) shouldBe Left(EntityConflict)
      }
    }
    "fail and return StaggerConflict" when {
      "multiple stagger is defined" in {
        tryParse(staggerConflict) shouldBe Left(StaggerConflict)
      }
    }
  }

}
