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

import uk.gov.hmrc.vatsignup.models.NinoSource._

sealed trait NinoSource {
  def toString: String
  def isFromEnrolment: Boolean
}

case object UserEntered extends NinoSource {
  override def toString: String = UserEnteredKey
  override def isFromEnrolment: Boolean = false
}

case object IRSA extends NinoSource {
  override def toString: String = IRSAKey
  override def isFromEnrolment: Boolean = true
}

case object AuthProfile extends NinoSource {
  override def toString: String = AuthProfileKey
  override def isFromEnrolment: Boolean = true
}


object NinoSource {

  val ninoSourceFrontEndKey = "ninoSource"

  val UserEnteredKey = "User entered"
  val IRSAKey = "IR-SA"
  val AuthProfileKey = "Auth profile"

  import play.api.libs.json._

  val reader: Reads[NinoSource] = JsPath.read[String].map {
    case UserEnteredKey => UserEntered
    case IRSAKey => IRSA
    case AuthProfileKey => AuthProfile
  }
  val writer: Writes[NinoSource] = new Writes[NinoSource] {
    def writes(ninoSource: NinoSource): JsValue =
      JsString(ninoSource.toString)
  }

  implicit val format: Format[NinoSource] = Format(reader, writer)
}
