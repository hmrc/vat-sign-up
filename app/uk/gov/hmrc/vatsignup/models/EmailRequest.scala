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

import java.time.Instant

import play.api.libs.json.{Json, OFormat}

case class EmailRequest(vatNumber: String,
                        email: String,
                        isDelegated: Boolean)

object EmailRequest {

  val idKey = "_id"
  val emailKey = "email"
  val delegatedKey = "isDelegated"
  val creationTimestampKey = "creationTimestamp"

  val mongoFormat: OFormat[EmailRequest] = OFormat(
    json =>
      for {
        vatNumber <- (json \ idKey).validate[String]
        email <- (json \ emailKey).validate[String]
        isDelegated <- (json \ delegatedKey).validate[Boolean]
      } yield EmailRequest(vatNumber, email, isDelegated),
    emailRequest =>
      Json.obj(
        idKey -> emailRequest.vatNumber,
        emailKey -> emailRequest.email,
        delegatedKey -> emailRequest.isDelegated,
        creationTimestampKey -> Json.obj("$date" -> Instant.now.toEpochMilli)
      )
  )

}
