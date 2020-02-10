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

import java.time.Month

import org.scalatest.{WordSpec, Matchers}

class VatKnownFactsSpec extends WordSpec with Matchers {
  "fromMMM" should {
    "convert FEB to FEBRUARY" in {
      VatKnownFacts.fromMMM("FEB") shouldBe Month.FEBRUARY
    }

    "convert SEP to SEPTEMBER" in {
      VatKnownFacts.fromMMM("SEP") shouldBe Month.SEPTEMBER
    }

    "convert DEC to DECEMBER" in {
      VatKnownFacts.fromMMM("DEC") shouldBe Month.DECEMBER
    }
  }
  "fromDisplayName" should {
    "convert February to FEBRUARY" in {
      VatKnownFacts.fromDisplayName("February") shouldBe Month.FEBRUARY
    }
    "convert September to SEPTEMBER" in {
      VatKnownFacts.fromDisplayName("September") shouldBe Month.SEPTEMBER
    }
    "convert December to DECEMBER" in {
      VatKnownFacts.fromDisplayName("December") shouldBe Month.DECEMBER
    }
  }
}
