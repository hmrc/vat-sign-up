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
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.vatsignup.httpparsers.EnrolmentStoreProxyHttpParser.principalGroupKey

object EnrolmentStoreProxyStub extends WireMockMethods {
  val testGroupID1 = "ABCEDEFGI1234567"
  val testGroupID2 = "ABCEDEFGI1234568"

  val jsonResponseBody: JsValue = Json.parse(input =
    s"""
      {
        "$principalGroupKey": [
          "$testGroupID1",
          "$testGroupID2"
        ]
      }
    """
  )

  def stubGetAllocatedMtdVatEnrolmentStatus(vatNumber: String)(status: Int): StubMapping = {
    when(method = GET, uri = s"/enrolment-store-proxy/enrolment-store/enrolments/HMRC-MTD-VAT~VRN~$vatNumber/groups\\?type=principal")
      .thenReturn(status = status, body = jsonResponseBody)
  }

}
