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

import play.api.libs.json._

sealed trait MandationStatus {
  def Name: String
}

case object MTDfBMandated extends MandationStatus {
  override val Name: String = "MTDfB Mandated"
}

case object MTDfBVoluntary extends MandationStatus {
  override val Name: String = "MTDfB Voluntary"
}

case object MTDfBExempt extends MandationStatus {
  override val Name: String = "MTDfB Exempt"
}

case object MTDfB extends MandationStatus {
  override val Name: String = "MTDfB"
}

case object NonMTDfB extends MandationStatus {
  override val Name: String = "Non MTDfB"
}


case object NonDigital extends MandationStatus {
  override val Name: String = "Non Digital"
}


object MandationStatus {

  def unapply(arg: MandationStatus): Option[String] = Some(arg.Name)

  val reader: Reads[MandationStatus] = for {
    value <- JsPath.read[String].map {
      case MTDfB.Name => MTDfB
      case MTDfBExempt.Name => MTDfBExempt
      case MTDfBMandated.Name => MTDfBMandated
      case MTDfBVoluntary.Name => MTDfBVoluntary
      case NonMTDfB.Name => NonMTDfB
      case NonDigital.Name => NonDigital
    }
  } yield value

  val writer: Writes[MandationStatus] = Writes(
    (status: MandationStatus) => JsString(status.Name)
  )

  implicit val format: Format[MandationStatus] = Format[MandationStatus](
    reader,
    writer
  )

}
