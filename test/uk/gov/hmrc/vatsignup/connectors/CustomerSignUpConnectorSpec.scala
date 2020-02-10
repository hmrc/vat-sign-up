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

import play.api.libs.json.Json
import org.scalatest.{WordSpec, Matchers}
import uk.gov.hmrc.vatsignup.helpers.TestConstants._

class CustomerSignUpConnectorSpec extends WordSpec with Matchers {

  "CustomerSignUpConnector" should {
    import CustomerSignUpConnector._
    "convert the request into the correct DES format" in {
      val requestJson = buildRequest(testSafeId,
        testVatNumber,
        Some(testEmail),
        emailVerified = Some(true),
        isPartialMigration = true,
        testContactPreference
      )

      val expectedJson = Json.parse(
        s"""
           |{
           |  "signUpRequest": {
           |    "identification": [
           |      {
           |        "idType": "SAFEID",
           |        "idValue": "$testSafeId"
           |      },
           |     {
           |        "idType": "VRN",
           |        "idValue": "$testVatNumber"
           |      }
           |    ],
           |    "additionalInformation": [
           |      {
           |        "typeOfField": "EMAIL",
           |        "fieldContents": "$testEmail",
           |        "infoVerified": true
           |      }
           |    ],
           |    "isPartialMigration" : true,
           |    "channel" : "Digital"
           |  }
           |}
        """.stripMargin
      )
      requestJson shouldBe expectedJson
    }

    "convert the request into the correct DES format when email is not defined" in {
      val requestJson = buildRequest(testSafeId,
        testVatNumber,
        None,
        emailVerified = None,
        isPartialMigration = false,
        testContactPreference
      )

      val expectedJson = Json.parse(
        s"""
           |{
           |  "signUpRequest": {
           |    "identification": [
           |      {
           |        "idType": "SAFEID",
           |        "idValue": "$testSafeId"
           |      },
           |     {
           |        "idType": "VRN",
           |        "idValue": "$testVatNumber"
           |      }
           |    ],
           |    "isPartialMigration" : false,
           |    "channel" : "Digital"
           |  }
           |}
        """.stripMargin
      )
      requestJson shouldBe expectedJson
    }
  }

}
