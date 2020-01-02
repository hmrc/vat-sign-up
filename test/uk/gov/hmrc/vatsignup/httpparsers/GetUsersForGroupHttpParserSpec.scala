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

import org.scalatest.EitherValues
import play.api.http.Status._
import play.api.libs.json.Json
import uk.gov.hmrc.auth.core.{Admin, Assistant, CredentialRole}
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.vatsignup.helpers.TestConstants._
import uk.gov.hmrc.vatsignup.httpparsers.GetUsersForGroupHttpParser.CredentialRoleReads._
import uk.gov.hmrc.vatsignup.httpparsers.GetUsersForGroupHttpParser.GetUsersForGroupHttpReads.read
import uk.gov.hmrc.vatsignup.httpparsers.GetUsersForGroupHttpParser.UserReads._
import uk.gov.hmrc.vatsignup.httpparsers.GetUsersForGroupHttpParser._

class GetUsersForGroupHttpParserSpec extends UnitSpec with EitherValues {
  val testMethod = "GET"
  val testUrl = "/"

  val testUsers: Map[String, CredentialRole] = Map(testCredentialId -> Admin, testCredentialId2 -> Assistant)

  "GetUsersForGroupHttpReads#read" when {
    s"the http status is $NON_AUTHORITATIVE_INFORMATION" when {
      s"the json is valid" should {
        s"return ${UsersFound(testUsers)}" in {
          val testResponse = HttpResponse(
            responseStatus = NON_AUTHORITATIVE_INFORMATION,
            responseJson = Some(
              Json.arr(
                Json.obj(
                  userIdKey -> testCredentialId,
                  credentialRoleKey -> AdminKey
                ),
                Json.obj(
                  userIdKey -> testCredentialId2,
                  credentialRoleKey -> AssistantKey
                )
              )
            )
          )

          read(testMethod, testUrl, testResponse) shouldBe Right(UsersFound(testUsers))
        }
      }
    }


    "the http status is anything else" should {
      s"return ${UsersGroupsSearchConnectionFailure(INTERNAL_SERVER_ERROR)}" in {
        val testResponse = HttpResponse(INTERNAL_SERVER_ERROR)

        read(testMethod, testUrl, testResponse) shouldBe Left(UsersGroupsSearchConnectionFailure(INTERNAL_SERVER_ERROR))
      }
    }
  }
}
