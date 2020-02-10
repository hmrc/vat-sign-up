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

import play.api.http.Status.NON_AUTHORITATIVE_INFORMATION
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.{Admin, Assistant}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.vatsignup.helpers.ComponentSpecBase
import uk.gov.hmrc.vatsignup.helpers.IntegrationTestConstants._
import uk.gov.hmrc.vatsignup.helpers.servicemocks.UsersGroupsSearchStub._
import uk.gov.hmrc.vatsignup.httpparsers.GetUsersForGroupHttpParser.UsersFound

class UsersGroupsSearchConnectorISpec extends ComponentSpecBase {
  lazy val connector: UsersGroupsSearchConnector = app.injector.instanceOf[UsersGroupsSearchConnector]
  private implicit val headerCarrier: HeaderCarrier = HeaderCarrier()

  "GetUserIds" should {
    "Return UsersFound and a Set of User IDs" when {
      "EnrolmentStoreProxy ES0 returns OK and Json response" in {
        stubGetUsersForGroup(testGroupId)(NON_AUTHORITATIVE_INFORMATION, successfulResponseBody)

        val res = connector.getUsersForGroup(testGroupId)

        await(res) shouldBe Right(UsersFound(Map(testCredentialId -> Admin, testCredentialId2 -> Assistant)))
      }
    }
  }
}
