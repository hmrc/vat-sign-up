/*
 * Copyright 2019 HM Revenue & Customs
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
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.vatsignup.config.AppConfig
import uk.gov.hmrc.vatsignup.httpparsers.EnrolmentStoreProxyHttpParser.EnrolmentStoreProxyResponse
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._
import uk.gov.hmrc.vatsignup.httpparsers.QueryUsersHttpParser.QueryUsersResponse

import scala.concurrent.Future

@Singleton
class EnrolmentStoreProxyConnector @Inject()(http: HttpClient,
                                             appConfig: AppConfig){

  def getAllocatedEnrolments(vatNumber: String)(implicit hc: HeaderCarrier): Future[EnrolmentStoreProxyResponse] = {
    http.GET[EnrolmentStoreProxyResponse](appConfig.getAllocatedEnrolmentUrl(vatNumber))
  }

  def getUserIds(vatNumber: String)(implicit hc: HeaderCarrier): Future[QueryUsersResponse] = {
    http.GET[QueryUsersResponse](
      url = appConfig.queryUsersUrl(vatNumber),
      queryParams = Seq(EnrolmentStoreProxyConnector.principalQueryKey))
  }

}

object EnrolmentStoreProxyConnector {

  val principalQueryKey: (String, String) = "type" -> "principal"

}