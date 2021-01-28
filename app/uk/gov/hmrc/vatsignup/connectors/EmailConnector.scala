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

package uk.gov.hmrc.vatsignup.connectors

import javax.inject.{Inject, Singleton}
import play.api.libs.json.Json
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.vatsignup.config.AppConfig
import uk.gov.hmrc.vatsignup.connectors.EmailConnector._
import uk.gov.hmrc.vatsignup.httpparsers.SendEmailHttpParser._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EmailConnector @Inject()(http: HttpClient,
                               appConfig: AppConfig
                              )(implicit ec: ExecutionContext) {
  def sendEmail(emailAddress: String,
                emailTemplate: String,
                vatNumber: Option[String])(implicit hc: HeaderCarrier): Future[SendEmailResponse] = {
    val body = Json.obj(
      toKey -> Json.arr(emailAddress),
      templateIdKey -> emailTemplate
    ) ++ (vatNumber match {
      case None => Json.obj()
      case Some(vn) => Json.obj(parametersKey -> Json.obj(vatNumberKey -> vn))
    })

    http.POST(appConfig.sendEmailUrl, body)
  }
}

object EmailConnector {
  val toKey = "to"
  val templateIdKey = "templateId"
  val vatNumberKey = "vatNumber"
  val parametersKey = "parameters"
}
