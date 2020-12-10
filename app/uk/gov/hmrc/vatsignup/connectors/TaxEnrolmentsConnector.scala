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
import play.api.libs.json.{JsObject, Json}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.vatsignup.config.AppConfig
import uk.gov.hmrc.vatsignup.config.Constants.TaxEnrolments._
import uk.gov.hmrc.vatsignup.httpparsers.TaxEnrolmentsHttpParser._
import uk.gov.hmrc.vatsignup.httpparsers.UpsertEnrolmentResponseHttpParser.UpsertEnrolmentResponse

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class TaxEnrolmentsConnector @Inject()(http: HttpClient,
                                       applicationConfig: AppConfig)(implicit ec: ExecutionContext) {

  private def taxEnrolmentsCallbackUrl(vatNumber: String) =
    s"${applicationConfig.baseUrl}/vat-sign-up/subscription-request/vat-number/$vatNumber/callback"

  def registerEnrolment(vatNumber: String, safeId: String)
                       (implicit hc: HeaderCarrier): Future[TaxEnrolmentsResponse] = {

    val enrolmentRequestBody = {
      Json.obj(
        "serviceName" -> MtdEnrolmentKey,
        "callback" -> taxEnrolmentsCallbackUrl(vatNumber),
        "etmpId" -> safeId
      )
    }

    http.PUT[JsObject, TaxEnrolmentsResponse](
      url = s"${applicationConfig.taxEnrolmentsUrl}/subscriptions/$vatNumber/subscriber",
      body = enrolmentRequestBody
    )
  }

}
