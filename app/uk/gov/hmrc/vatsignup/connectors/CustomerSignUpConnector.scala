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

package uk.gov.hmrc.vatsignup.connectors

import javax.inject.{Inject, Singleton}

import play.api.libs.json.{JsObject, Json, Writes}
import uk.gov.hmrc.http.logging.Authorization
import uk.gov.hmrc.http.{HeaderCarrier, HttpReads}
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._
import uk.gov.hmrc.vatsignup.config.AppConfig
import uk.gov.hmrc.vatsignup.httpparsers.CustomerSignUpHttpParser._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CustomerSignUpConnector @Inject()(val http: HttpClient,
                                        val applicationConfig: AppConfig) {

  private def url = applicationConfig.desUrl + "/cross-regime/signup/VATC"

  import CustomerSignUpConnector._

  def signUp(safeId: String, vatNumber: String, email: Option[String], emailVerified: Option[Boolean]
            )(implicit hc: HeaderCarrier): Future[CustomerSignUpResponse] = {
    val headerCarrier = hc
      .withExtraHeaders(applicationConfig.desEnvironmentHeader)
      .copy(authorization = Some(Authorization(applicationConfig.desAuthorisationToken)))

    http.POST[JsObject, CustomerSignUpResponse](
      url = url,
      body = buildRequest(safeId, vatNumber, email, emailVerified)
    )(
      implicitly[Writes[JsObject]],
      implicitly[HttpReads[CustomerSignUpResponse]],
      headerCarrier,
      implicitly[ExecutionContext]
    )
  }

}

object CustomerSignUpConnector {

  import uk.gov.hmrc.vatsignup.config.Constants.Des._

  private[connectors] def buildRequest(safeId: String, vatNumber: String, email: Option[String], emailVerified: Option[Boolean]): JsObject = {
    Json.obj(
      "signUpRequest" -> Json.obj(
        "identification" ->
          Json.arr(
            Json.obj(IdTypeKey -> SafeIdKey, IdValueKey -> safeId),
            Json.obj(IdTypeKey -> VrnKey, IdValueKey -> vatNumber)
          ))
        .++(
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
        )
    )
  }

}
