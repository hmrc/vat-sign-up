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

package uk.gov.hmrc.vatsignup.httpparsers

import org.scalatest.EitherValues
import play.api.test.Helpers._
import play.api.libs.json.Json
import uk.gov.hmrc.http.HttpResponse
import org.scalatest.{WordSpec, Matchers}
import uk.gov.hmrc.vatsignup.helpers.TestConstants._
import uk.gov.hmrc.vatsignup.httpparsers.QueryUsersHttpParser.QueryUsersHttpReads.read
import uk.gov.hmrc.vatsignup.httpparsers.QueryUsersHttpParser._

class QueryUsersHttpParserSpec extends WordSpec with Matchers with EitherValues {
  val testMethod = "GET"
  val testUrl = "/"

  "QueryUserHttpReads#read" when {
    "the http status is OK" when {
      s"the json is valid" should {
        s"return ${UsersFound(Set(testGroupId, testGroupId))}" in {
          val testResponse = HttpResponse(
            responseStatus = OK,
            responseJson = Some(
              Json.obj(
                principalUserIdKey -> Json.arr(testGroupId, testGroupId)
              )
            )
          )

          read(testMethod, testUrl, testResponse) shouldBe Right(UsersFound(Set(testGroupId, testGroupId)))
        }
      }
    }

    "the http status is NoContent" should {
      "return EnrolmentNotAllocated" in {
        val testResponse = HttpResponse(NO_CONTENT)

        read(testMethod, testUrl, testResponse) shouldBe Right(NoUsersFound)
      }
    }

    "the http status is anything else" should {
      s"return ${EnrolmentStoreProxyConnectionFailure(INTERNAL_SERVER_ERROR)}" in {
        val testResponse = HttpResponse(INTERNAL_SERVER_ERROR)

        read(testMethod, testUrl, testResponse) shouldBe Left(EnrolmentStoreProxyConnectionFailure(INTERNAL_SERVER_ERROR))
      }
    }
  }
}
