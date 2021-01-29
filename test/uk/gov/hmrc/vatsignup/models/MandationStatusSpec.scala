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

package uk.gov.hmrc.vatsignup.models

import play.api.libs.json.JsString
import org.scalatest.{WordSpec, Matchers}


class MandationStatusSpec extends WordSpec with Matchers {

  "reader" should {
    "parse 'MTDfB Mandated' into MTDfBMandated" in {
      val data = MandationStatus.reader.reads(JsString("MTDfB Mandated")).get
      data shouldBe MTDfBMandated
    }
    "parse 'MTDfB Voluntary' into MTDfBVoluntary" in {
      val data = MandationStatus.reader.reads(JsString("MTDfB Voluntary")).get
      data shouldBe MTDfBVoluntary
    }

    "parse 'MTDfB' into MTDfB" in {
      val data = MandationStatus.reader.reads(JsString("MTDfB")).get
      data shouldBe MTDfB
    }

    "parse 'MTDfB Exempt' into MTDfBExempt" in {
      val data = MandationStatus.reader.reads(JsString("MTDfB Exempt")).get
      data shouldBe MTDfBExempt
    }

    "parse 'Non MTDfB' into NonMTDfB" in {
      val data = MandationStatus.reader.reads(JsString("Non MTDfB")).get
      data shouldBe NonMTDfB
    }
    "parse 'Non Digital' into NonDigital" in {
      val data = MandationStatus.reader.reads(JsString("Non Digital")).get
      data shouldBe NonDigital
    }
  }

  "writer" should {
    "write the status correctly" in {
      MandationStatus.writer.writes(MTDfBMandated) shouldBe JsString(MTDfBMandated.Name)
      MandationStatus.writer.writes(MTDfBVoluntary) shouldBe JsString(MTDfBVoluntary.Name)
      MandationStatus.writer.writes(MTDfB) shouldBe JsString(MTDfB.Name)
      MandationStatus.writer.writes(MTDfBExempt) shouldBe JsString(MTDfBExempt.Name)
      MandationStatus.writer.writes(NonMTDfB) shouldBe JsString(NonMTDfB.Name)
      MandationStatus.writer.writes(NonDigital) shouldBe JsString(NonDigital.Name)
    }
  }

}
