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


sealed trait NinoSource {
  def toString: String
}

case object UserEntered extends NinoSource {
  override def toString: String = "User entered"
}

case object IRSA extends NinoSource {
  override def toString: String = "IR-SA"
}


object NinoSource {

  val ninoSourceFrontEndKey = "ninoSource"

  import play.api.libs.json._

  val reader: Reads[NinoSource] = JsPath.read[String].map {
    case "User entered" => UserEntered
    case "IR-SA" => IRSA
  }
  val writer: Writes[NinoSource] = new Writes[NinoSource] {
    def writes(ninoSource: NinoSource): JsValue =
      JsString(ninoSource.toString)
  }

  implicit val format: Format[NinoSource] = Format(reader, writer)
}
