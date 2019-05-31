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

import helpers.WiremockHelper
import play.api.libs.json.Json
import uk.gov.hmrc.vatsignup.config.Constants

object TaxEnrolmentsStub extends WireMockMethods {

  val mockUrl = s"http://${WiremockHelper.wiremockHost}:${WiremockHelper.wiremockPort}"

  private def registerEnrolmentUri(vatNumber: String): String =
    s"/tax-enrolments/subscriptions/$vatNumber/subscriber"

  private def taxEnrolmentsCallbackUrl(vatNumber: String) =
    s"$mockUrl/vat-sign-up/subscription-request/vat-number/$vatNumber/callback"

  private def upsertEnrolmentUrl(enrolmentKey: String) =
    s"/tax-enrolments/enrolments/$enrolmentKey"

  private def allocateEnrolmentUrl(groupId: String, enrolmentKey: String) =
    s"/tax-enrolments/groups/$groupId/enrolments/$enrolmentKey"

  private def assignEnrolmentUrl(userId: String, enrolmentKey: String) =
    s"/tax-enrolments/users/$userId/enrolments/$enrolmentKey"

  def stubRegisterEnrolment(vatNumber: String, safeId: String)(status: Int): Unit = {
    val registerEnrolmentJsonBody = Json.obj(
      "serviceName" -> Constants.TaxEnrolments.ServiceName,
      "callback" -> taxEnrolmentsCallbackUrl(vatNumber),
      "etmpId" -> safeId
    )
    when(
      method = PUT,
      uri = registerEnrolmentUri(vatNumber),
      body = registerEnrolmentJsonBody
    ) thenReturn status
  }

  def stubUpsertEnrolment(vatNumber: String, postcode: String, vatRegistrationDate: String)(status: Int): Unit = {
    val allocateEnrolmentJsonBody = Json.obj(
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

    val enrolmentKey = s"HMRC-MTD-VAT~VRN~$vatNumber"

    when(
      method = PUT,
      uri = upsertEnrolmentUrl(
        enrolmentKey = enrolmentKey
      ),
      body = allocateEnrolmentJsonBody
    ) thenReturn status
  }

  def verifyUpsertEnrolment(vatNumber: String, postcode: String, vatRegistrationDate: String): Unit = {
    val allocateEnrolmentJsonBody = Json.obj(
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

    val enrolmentKey = s"HMRC-MTD-VAT~VRN~$vatNumber"

    verify(
      method = PUT,
      uri = upsertEnrolmentUrl(
        enrolmentKey = enrolmentKey
      ),
      body = allocateEnrolmentJsonBody
    )

  }

  def stubAllocateEnrolment(vatNumber: String, groupId: String, credentialId: String, postcode: String, vatRegistrationDate: String)(status: Int): Unit = {
    val allocateEnrolmentJsonBody = Json.obj(
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

    val enrolmentKey = s"HMRC-MTD-VAT~VRN~$vatNumber"

    when(
      method = POST,
      uri = allocateEnrolmentUrl(
        groupId = groupId,
        enrolmentKey = enrolmentKey
      ),
      body = allocateEnrolmentJsonBody
    ) thenReturn status
  }

  def verifyAllocateEnrolment(vatNumber: String, groupId: String, credentialId: String, postcode: String, vatRegistrationDate: String): Unit = {
    val allocateEnrolmentJsonBody = Json.obj(
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

    val enrolmentKey = s"HMRC-MTD-VAT~VRN~$vatNumber"

    verify(
      method = POST,
      uri = allocateEnrolmentUrl(
        groupId = groupId,
        enrolmentKey = enrolmentKey
      ),
      body = allocateEnrolmentJsonBody
    )
  }

  def stubAssignEnrolment(vatNumber: String, userId: String)(status: Int): Unit = {
    val enrolmentKey = s"HMRC-MTD-VAT~VRN~$vatNumber"

    when(
      method = POST,
      uri = assignEnrolmentUrl(
        userId = userId,
        enrolmentKey = enrolmentKey
      )
    ) thenReturn status
  }

  def verifyAssignEnrolment(vatNumber: String, userId: String): Unit = {
    val enrolmentKey = s"HMRC-MTD-VAT~VRN~$vatNumber"

    verify(
      method = POST,
      uri = assignEnrolmentUrl(
        userId = userId,
        enrolmentKey = enrolmentKey
      )
    )
  }
}
