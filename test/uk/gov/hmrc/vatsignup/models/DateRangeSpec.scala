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

package uk.gov.hmrc.vatsignup.models

import java.time.LocalDate

import uk.gov.hmrc.play.test.UnitSpec


class DateRangeSpec extends UnitSpec {

  "contains" should {

    val testStartDate = LocalDate.of(2018, 9, 1)
    val testEndDate = testStartDate.plusMonths(1)
    val testRange = DateRange(testStartDate, testEndDate)

    "return true if the date is on the start date" in {
      testRange.contains(testStartDate) shouldBe true
    }
    "return true if the date is on the end date" in {
      testRange.contains(testEndDate) shouldBe true
    }
    "return true if the date is between the start and end dates" in {
      testRange.contains(testStartDate.plusDays(1)) shouldBe true
      testRange.contains(testEndDate.minusDays(1)) shouldBe true
    }
    "return false if the date is before the start date" in {
      testRange.contains(testStartDate.minusDays(1)) shouldBe false

    }
    "return false if the date is after the end date" in {
      testRange.contains(testEndDate.plusDays(1)) shouldBe false
    }

  }

}
