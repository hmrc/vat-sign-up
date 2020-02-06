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

package uk.gov.hmrc.vatsignup.httpparsers

import org.scalatest.{EitherValues, Matchers, WordSpec}
import play.api.libs.json.Json
import play.api.test.Helpers._
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.vatsignup.helpers.TestConstants._
import uk.gov.hmrc.vatsignup.httpparsers.EnrolmentStoreProxyHttpParser.EnrolmentStoreProxyHttpReads.read
import uk.gov.hmrc.vatsignup.httpparsers.EnrolmentStoreProxyHttpParser._

class EnrolmentStoreProxyHttpParserSpec extends WordSpec with Matchers {
  val testMethod = "GET"
  val testUrl = "/"

  "EnrolmentStoreProxyHttpReads#read" when {
    "the http status is OK" when {
      s"the json is valid" should {
        s"return ${EnrolmentAlreadyAllocated(testGroupId)}" in {
          val testResponse = HttpResponse(
            responseStatus = OK,
            responseJson = Some(
              Json.obj(
                principalGroupIdKey -> Json.arr(testGroupId)
              )
            )
          )

          read(testMethod, testUrl, testResponse) shouldBe Right(EnrolmentAlreadyAllocated(testGroupId))
        }
      }
    }

    "the http status is NoContent" should {
      "return EnrolmentNotAllocated" in {
        val testResponse = HttpResponse(NO_CONTENT)

        read(testMethod, testUrl, testResponse) shouldBe Right(EnrolmentNotAllocated)
      }
    }

    "the http status is anything else" should {
      s"return ${EnrolmentStoreProxyFailure(INTERNAL_SERVER_ERROR)}" in {
        val testResponse = HttpResponse(INTERNAL_SERVER_ERROR)

        read(testMethod, testUrl, testResponse) shouldBe Left(EnrolmentStoreProxyFailure(INTERNAL_SERVER_ERROR))
      }
    }
  }
}
