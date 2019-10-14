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
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.vatsignup.utils.EnrolmentUtils
import uk.gov.hmrc.vatsignup.config.Constants._

class EnrolmentSpec extends UnitSpec with GuiceOneAppPerSuite {

  "vatNumber" when {
    "the user has a legacy vat enrolment" should {
      "return Some(vatNumber) from the legacy VAT enrolment" in {

      }

      "get mtdVatEnrolment" should {
        "return the vat number from the MTD VAT enrolment" in {


          "the user has no enrolment" should {
            "return None" in {

            }
      }
    }
  }
}
