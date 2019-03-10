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

package uk.gov.hmrc.vatsignup.models

import play.api.libs.json.{Format, JsResult, JsString, JsValue}

sealed trait ContactPreference

case object Paper extends ContactPreference

case object Digital extends ContactPreference

case object ContactPreference {
  val PaperKey = "Paper"
  val DigitalKey = "Digital"

  implicit val contactPreferenceFormat: Format[ContactPreference] = new Format[ContactPreference] {
    override def writes(contactPreference: ContactPreference): JsValue = contactPreference match {
      case Paper => JsString(PaperKey)
      case Digital => JsString(DigitalKey)
    }

    override def reads(json: JsValue): JsResult[ContactPreference] =
      json.validate[String] map {
        case PaperKey => Paper
        case DigitalKey => Digital
      }
  }
}
