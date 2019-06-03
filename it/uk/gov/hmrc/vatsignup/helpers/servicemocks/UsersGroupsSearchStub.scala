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

package uk.gov.hmrc.vatsignup.helpers.servicemocks

import play.api.libs.json.{JsObject, JsValue, Json}
import uk.gov.hmrc.vatsignup.helpers.IntegrationTestConstants._
import uk.gov.hmrc.vatsignup.httpparsers.GetUsersForGroupHttpParser.CredentialRoleReads._
import uk.gov.hmrc.vatsignup.httpparsers.GetUsersForGroupHttpParser.UserReads.{credentialRoleKey, userIdKey}



object UsersGroupsSearchStub extends WireMockMethods {
  private def getUsersForGroupUrl(groupId: String) = s"/users-groups-search/groups/$groupId/users"

  def stubGetUsersForGroup(groupId: String)(responseStatus: Int, responseBody: JsValue): Unit = {
    when(
      method = GET,
      uri = getUsersForGroupUrl(groupId)
    ) thenReturn(responseStatus, responseBody)
  }

  val successfulResponseBody = Json.arr(
    Json.obj(
      userIdKey -> testCredentialId,
      credentialRoleKey -> AdminKey
    ),
    Json.obj(
      userIdKey -> testCredentialId2,
      credentialRoleKey -> AssistantKey
    )
  )

}
