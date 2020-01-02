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

package uk.gov.hmrc.vatsignup.utils

import java.nio.charset.StandardCharsets.UTF_8
import java.util.Base64

import play.api.http.HeaderNames._
import play.api.mvc.{AnyContent, Request}

import scala.util.matching.Regex

case class BasicAuthentication(username: String, password: String)

object AuthUtils {
  def decodeFromBase64(encodedString: String): String = new String(Base64.getDecoder.decode(encodedString), UTF_8)

  def encodeToBase64(string: String): String = Base64.getEncoder.encodeToString(string.getBytes(UTF_8))

  def getBasicAuth(request: Request[AnyContent]): Option[BasicAuthentication] = {
    request.headers.get(AUTHORIZATION) match {
      case Some(BasicAuthHeader(encodedAuthHeader)) =>
        decodeFromBase64(encodedAuthHeader) match {
          case DecodedAuth(username, password) =>
            Some(BasicAuthentication(username, password))
          case _ =>
            None
        }
      case _ =>
        None
    }
  }

  val BasicAuthHeader: Regex = "Basic (.+)".r
  val DecodedAuth: Regex = "(.+):(.+)".r
}
