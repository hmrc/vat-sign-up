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

package uk.gov.hmrc.vatsignup.helpers.servicemocks

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.libs.json.Json
import uk.gov.hmrc.vatsignup.config.Constants.Des._
import uk.gov.hmrc.vatsignup.models.ContactPreference


object SignUpStub extends WireMockMethods {

  def stubSignUp[T](safeId: String,
                                         vatNumber: String,
                                         email: Option[String],
                                         emailVerified: Option[Boolean],
                                         optIsPartialMigration: Option[Boolean],
                                         optContactPreference: Option[ContactPreference] = None
                                        )(status: Int): StubMapping =
    when(method = POST, uri = "/cross-regime/signup/VATC",
      body = Json.obj(
        "signUpRequest" -> Json.obj(
          "identification" ->
            Json.arr(
              Json.obj(IdTypeKey -> SafeIdKey, IdValueKey -> safeId),
              Json.obj(IdTypeKey -> VrnKey, IdValueKey -> vatNumber)
            )).++(
          (email, emailVerified) match {
            case (Some(address), Some(isVerified)) =>
              Json.obj("additionalInformation" ->
                Json.arr(
                  Json.obj(
                    "typeOfField" -> emailKey,
                    "fieldContents" -> address,
                    "infoVerified" -> isVerified
                  )
                )
              )
            case _ => Json.obj()
          }
        ).++(
          optIsPartialMigration match {
            case Some(isPartialMigration) => Json.obj("isPartialMigration" -> isPartialMigration)
            case _ => Json.obj()
          }
        ).++(
          optContactPreference match {
            case Some(contactPreference) => Json.obj("channel" -> contactPreference)
            case _ => Json.obj()
          }
        )

      ),
      headers = Map(
        "Authorization" -> "Bearer dev",
        "Environment" -> "dev",
        "Content-Type" -> "application/json"
      )
    ).thenReturn(status = status)

}
