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

package uk.gov.hmrc.vatsignup.utils.controllist

import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import uk.gov.hmrc.auth.core.{Enrolment, Enrolments}
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.vatsignup.config.Constants.Des.VrnKey
import uk.gov.hmrc.vatsignup.config.Constants.TaxEnrolments.MtdEnrolmentKey
import uk.gov.hmrc.vatsignup.config.Constants.{VatDecEnrolmentKey, VatReferenceKey}
import uk.gov.hmrc.vatsignup.utils.EnrolmentUtils.{EnrolmentUtils, NoEnrolment, VatNumberMismatch}


class EnrolmentUtilsSpec extends UnitSpec with GuiceOneAppPerSuite {

  val testVRN = "222222227"
  val testPrincipalDecEnrolment: Enrolment = Enrolment(VatDecEnrolmentKey).withIdentifier(VatReferenceKey, testVRN)
  val testPrincipalMtdEnrolment: Enrolment = Enrolment(MtdEnrolmentKey).withIdentifier(VrnKey, testVRN)
  val testPrincipalMtdEnrolment2: Enrolment = Enrolment(MtdEnrolmentKey).withIdentifier(VrnKey, testVRN ++ "1")
  val testPrincipalDecUser = Enrolments(Set(testPrincipalDecEnrolment))
  val testPrincipalMtdUser = Enrolments(Set(testPrincipalMtdEnrolment))

  ".vatNumber" when {

    "the user has both a legacy Vat enrolment and an mtd Vat enrolment" when {
      "legacy Vat number equals to mtd Vat number" should {
        "return Vat number from the mtd VAT enrolment" in {

          val principalMtdDecMatchUser = Enrolments(Set(testPrincipalMtdEnrolment, testPrincipalDecEnrolment))

          EnrolmentUtils(principalMtdDecMatchUser).vatNumber shouldBe Right(testVRN)
        }
      }
    }

    "the user has both a legacy Vat enrolment and an mtd Vat enrolment" when {
      "legacy Vat number does not equal to mtd Vat number" should {
        "return VatNumberMismatch" in {
          val principalMtdDecMismatchUser = Enrolments(Set(testPrincipalMtdEnrolment2, testPrincipalDecEnrolment))

          EnrolmentUtils(principalMtdDecMismatchUser).vatNumber shouldBe Left(VatNumberMismatch)
        }
      }
    }

    "the user has a legacy Vat enrolment" should {
      "return Vat number from the legacy VAT enrolment" in {
        EnrolmentUtils(testPrincipalDecUser).vatNumber shouldBe Right(testVRN)
      }
    }

    "the user has an mtd Vat enrolment" should {
      "return Vat number from the MTD VAT enrolment" in {
        EnrolmentUtils(testPrincipalMtdUser).vatNumber shouldBe Right(testVRN)
      }
    }

    "the user has no enrolment" should {
      "return NoEnrolment" in {
        val noEnrolmentUser = Enrolments(Set())

        EnrolmentUtils(noEnrolmentUser).vatNumber shouldBe Left(NoEnrolment)
      }
    }
  }
}



