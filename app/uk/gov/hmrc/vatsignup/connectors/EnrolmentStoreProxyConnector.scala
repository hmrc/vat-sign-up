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
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.vatsignup.config.AppConfig
import uk.gov.hmrc.vatsignup.httpparsers.EnrolmentStoreProxyHttpParser.EnrolmentStoreProxyResponse
import uk.gov.hmrc.vatsignup.httpparsers.QueryUsersHttpParser.QueryUsersResponse
import EnrolmentStoreProxyConnector._
import play.api.libs.json.{JsObject, Json}
import uk.gov.hmrc.vatsignup.httpparsers.AllocateEnrolmentResponseHttpParser.AllocateEnrolmentResponse
import uk.gov.hmrc.vatsignup.httpparsers.AssignEnrolmentToUserHttpParser.AssignEnrolmentToUserResponse
import uk.gov.hmrc.vatsignup.httpparsers.UpsertEnrolmentResponseHttpParser.UpsertEnrolmentResponse

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EnrolmentStoreProxyConnector @Inject()(http: HttpClient,
                                             appConfig: AppConfig)(implicit ec: ExecutionContext) {

  def getAllocatedEnrolments(enrolmentKey: String, ignoreAssignments: Boolean)(implicit hc: HeaderCarrier): Future[EnrolmentStoreProxyResponse] = {
    http.GET[EnrolmentStoreProxyResponse](
      url = appConfig.getAllocatedEnrolmentUrl(enrolmentKey),
      queryParams = Seq(principalQueryParameter, ignoreAssignmentsQueryParameter(ignoreAssignments))
    )
  }

  def getUserIds(vatNumber: String)(implicit hc: HeaderCarrier): Future[QueryUsersResponse] = {
    http.GET[QueryUsersResponse](
      url = appConfig.queryUsersUrl(vatNumber),
      queryParams = Seq(principalQueryParameter))
  }

  def upsertEnrolment(vatNumber: String,
                      postcode: Option[String],
                      vatRegistrationDate: String)
                     (implicit hc: HeaderCarrier): Future[UpsertEnrolmentResponse] = {
    val enrolmentKey = s"HMRC-MTD-VAT~VRN~$vatNumber"

    val requestBody = Json.obj(
      "verifiers" -> Json.arr(
        postcode.map(pc => Json.obj(
          "key" -> "Postcode",
          "value" -> pc
        )),
        Json.obj(
          "key" -> "VATRegistrationDate",
          "value" -> vatRegistrationDate
        )
      )
    )
    http.PUT[JsObject, UpsertEnrolmentResponse](
      url = appConfig.upsertEnrolmentEnrolmentStoreUrl(enrolmentKey),
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
      "type" -> "principal",
      "action" -> "enrolAndActivate"
    )
    http.POST[JsObject, AllocateEnrolmentResponse](
      url = appConfig.allocateEnrolmentEnrolmentStoreUrl(groupId, enrolmentKey),
      body = requestBody
    )
  }


  def assignEnrolment(credentialId: String,
                      vatNumber: String
                     )(implicit hc: HeaderCarrier): Future[AssignEnrolmentToUserResponse] = {
    val enrolmentKey = s"HMRC-MTD-VAT~VRN~$vatNumber"

    http.POSTEmpty[AssignEnrolmentToUserResponse](
      url = appConfig.assignEnrolmentUrl(credentialId, enrolmentKey)
    )
  }
}

object EnrolmentStoreProxyConnector {
  val principalQueryParameter: (String, String) = "type" -> "principal"
  def ignoreAssignmentsQueryParameter(ignoreAssignments: Boolean): (String, String) = "ignore-assignments" -> ignoreAssignments.toString
}
