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

package uk.gov.hmrc.vatsignup.connectors

import javax.inject.{Inject, Singleton}
import play.api.libs.json.{JsObject, Json, Writes}
import uk.gov.hmrc.http.logging.Authorization
import uk.gov.hmrc.http.{HeaderCarrier, HttpReads}
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.vatsignup.config.AppConfig
import uk.gov.hmrc.vatsignup.config.Constants.Des.{IdTypeKey, IdValueKey, SafeIdKey, VrnKey}
import uk.gov.hmrc.vatsignup.httpparsers.CustomerSignUpHttpParser._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class MigratedCustomerSignUpConnector @Inject()(http: HttpClient,
                                                appConfig: AppConfig
                                               )(implicit ec: ExecutionContext) {

  private val url = appConfig.desUrl + "/cross-regime/signup/VATC"

  def signUp(safeId: String,
             vatNumber: String,
             isPartialMigration: Boolean
            )(implicit hc: HeaderCarrier): Future[CustomerSignUpResponse] = {

    val headerCarrier = hc.withExtraHeaders(appConfig.desEnvironmentHeader)
      .copy(authorization = Some(Authorization(appConfig.desAuthorisationToken)))

    http.POST[JsObject, CustomerSignUpResponse](
      url = url,
      body = buildRequest(safeId, vatNumber, isPartialMigration)
    )(
      implicitly[Writes[JsObject]],
      implicitly[HttpReads[CustomerSignUpResponse]],
      headerCarrier,
      implicitly[ExecutionContext]
    )
  }

  private def buildRequest(safeId: String, vatNumber: String, isPartialMigration: Boolean): JsObject =
    Json.obj("signUpRequest" -> Json.obj(
      "identification" -> Json.arr(
        Json.obj(IdTypeKey -> SafeIdKey, IdValueKey -> safeId),
        Json.obj(IdTypeKey -> VrnKey, IdValueKey -> vatNumber)
      ),
      "isPartialMigration" -> isPartialMigration
    ))

}
