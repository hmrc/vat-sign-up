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
import play.api.libs.json.{JsObject, Json}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.vatsignup.config.AppConfig
import uk.gov.hmrc.vatsignup.config.Constants.TaxEnrolments._
import uk.gov.hmrc.vatsignup.httpparsers.AllocateEnrolmentResponseHttpParser.AllocateEnrolmentResponse
import uk.gov.hmrc.vatsignup.httpparsers.AssignEnrolmentToUserHttpParser.AssignEnrolmentToUserResponse
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
        "serviceName" -> ServiceName,
        "callback" -> taxEnrolmentsCallbackUrl(vatNumber),
        "etmpId" -> safeId
      )
    }

    http.PUT[JsObject, TaxEnrolmentsResponse](
      url = s"${applicationConfig.taxEnrolmentsUrl}/subscriptions/$vatNumber/subscriber",
      body = enrolmentRequestBody
    )
  }

  def upsertEnrolment(vatNumber: String,
                      postcode: String,
                      vatRegistrationDate: String)
                     (implicit hc: HeaderCarrier): Future[UpsertEnrolmentResponse] = {
    val enrolmentKey = s"HMRC-MTD-VAT~VRN~$vatNumber"

    val requestBody = Json.obj(
      "verifiers" -> Json.arr(
        Json.obj(
          "key" -> "Postcode",
          "value" -> postcode
        ),
        Json.obj(
          "key" -> "VATRegistrationDate",
          "value" -> vatRegistrationDate
        )
      )
    )
    http.PUT[JsObject, UpsertEnrolmentResponse](
      url = applicationConfig.upsertEnrolmentUrl(enrolmentKey),
      body = requestBody
    )

  }

  def allocateEnrolment(groupId: String,
                        credentialId: String,
                        vatNumber: String,
                        postcode: String,
                        vatRegistrationDate: String
                       )(implicit hc: HeaderCarrier): Future[AllocateEnrolmentResponse] = {
    val enrolmentKey = s"HMRC-MTD-VAT~VRN~$vatNumber"

    val requestBody = Json.obj(
      "userId" -> credentialId,
      "friendlyName" -> "Making Tax Digital - VAT",
      "type" -> "principal",
      "verifiers" -> Json.arr(
        Json.obj(
          "key" -> "Postcode",
          "value" -> postcode
        ),
        Json.obj(
          "key" -> "VATRegistrationDate",
          "value" -> vatRegistrationDate
        )
      )
    )

    http.POST[JsObject, AllocateEnrolmentResponse](
      url = applicationConfig.allocateEnrolmentUrl(groupId, enrolmentKey),
      body = requestBody
    )
  }


  def allocateEnrolmentWithoutKnownFacts(groupId: String,
                        credentialId: String,
                        vatNumber: String
                       )(implicit hc: HeaderCarrier): Future[AllocateEnrolmentResponse] = {
    val enrolmentKey = s"HMRC-MTD-VAT~VRN~$vatNumber"

    val requestBody = Json.obj(
      "userId" -> credentialId,
      "friendlyName" -> "Making Tax Digital - VAT",
      "type" -> "principal"
    )
    http.POST[JsObject, AllocateEnrolmentResponse](
      url = applicationConfig.allocateEnrolmentUrl(groupId, enrolmentKey),
      body = requestBody
    )
  }


  def assignEnrolment(credentialId: String,
                      vatNumber: String
                     )(implicit hc: HeaderCarrier): Future[AssignEnrolmentToUserResponse] = {
    val enrolmentKey = s"HMRC-MTD-VAT~VRN~$vatNumber"

    http.POSTEmpty[AssignEnrolmentToUserResponse](
      url = applicationConfig.assignEnrolmentUrl(credentialId, enrolmentKey)
    )
  }

}
