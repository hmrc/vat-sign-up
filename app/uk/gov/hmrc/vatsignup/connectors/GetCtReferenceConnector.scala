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

import play.api.Logger
import uk.gov.hmrc.http.logging.Authorization
import uk.gov.hmrc.http.{GatewayTimeoutException, HeaderCarrier, HttpReads}
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.vatsignup.config.AppConfig
import uk.gov.hmrc.vatsignup.httpparsers.GetCtReferenceHttpParser._

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class GetCtReferenceConnector @Inject()(http: HttpClient,
                                        appConfig: AppConfig
                                       )(implicit ec: ExecutionContext) {
  def getCtReference(companyNumber: String)(implicit hc: HeaderCarrier): Future[GetCtReferenceResponse] = {
    val headerCarrier = hc
      .withExtraHeaders(appConfig.desEnvironmentHeader)
      .copy(authorization = Some(Authorization(appConfig.desAuthorisationToken)))

    def request: Future[GetCtReferenceResponse] =
      http.GET(
        appConfig.getCtReferenceUrl(companyNumber)
      )(
        implicitly[HttpReads[GetCtReferenceResponse]],
        headerCarrier,
        implicitly[ExecutionContext]
      )

    request
      .flatMap {
        case Left(GetCtReferenceFailure(status, _)) if status >= 500 & status < 600 =>
          Logger.warn(s"[GetCtReferenceConnector] retrying once due to a $status response from DES")
          request
        case response => Future.successful(response)
      }
      .recoverWith {
        case ex: GatewayTimeoutException =>
          Logger.warn(s"[GetCtReferenceConnector] retrying once due to a 'GatewayTimeoutException: ${ex.message}'")
          request
      }
  }
}
