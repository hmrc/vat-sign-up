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

package uk.gov.hmrc.vatsignup.connectors

import play.api.http.Status._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.vatsignup.helpers.ComponentSpecBase
import uk.gov.hmrc.vatsignup.helpers.IntegrationTestConstants.{testUserIdSet, _}
import uk.gov.hmrc.vatsignup.helpers.servicemocks.EnrolmentStoreProxyStub._
import uk.gov.hmrc.vatsignup.httpparsers.EnrolmentStoreProxyHttpParser._
import uk.gov.hmrc.vatsignup.httpparsers.QueryUsersHttpParser._
import uk.gov.hmrc.vatsignup.utils.EnrolmentUtils._


class EnrolmentStoreProxyConnectorISpec extends ComponentSpecBase {

  lazy val connector: EnrolmentStoreProxyConnector = app.injector.instanceOf[EnrolmentStoreProxyConnector]

  private implicit val headerCarrier: HeaderCarrier = HeaderCarrier()

  "GetUserIds" should {
    "Return UsersFound and a Set of User IDs" when {
      "EnrolmentStoreProxy ES0 returns OK and Json response" in {
        stubGetUserIds(testVatNumber)(OK)

        val res = connector.getUserIds(testVatNumber)

        await(res) shouldBe Right(UsersFound(testUserIdSet))
      }
    }

    "Return NoUsersFound" when {
      "EnrolmentStoreProxy ES0 returns No Content" in {
        stubGetUserIds(testVatNumber)(NO_CONTENT)

        val res = connector.getUserIds(testVatNumber)

        await(res) shouldBe Right(NoUsersFound)
      }
    }

    "Return EnrolmentStoreProxyConnectionFailure and status" when {
      "EnrolmentStoreProxy ES0 returns Bad Request" in {
        stubGetUserIds(testVatNumber)(BAD_REQUEST)

        val res = connector.getUserIds(testVatNumber)

        await(res) shouldBe Left(EnrolmentStoreProxyConnectionFailure(BAD_REQUEST))
      }
    }
  }

  "GetAllocatedEnrolments" should {
    "Return EnrolmentAlreadyAllocated" when {
      "EnrolmentStoreProxy ES1 returns an OK and Json Response" in {
        stubGetAllocatedMtdVatEnrolmentStatus(testVatNumber)(OK)

        val res = connector.getAllocatedEnrolments(mtdVatEnrolmentKey(testVatNumber))

        await(res) shouldBe Right(EnrolmentAlreadyAllocated(testGroupId))
      }
    }

    "Return EnrolmentNotAllocated" when {
      "EnrolmentStoreProxy ES1 returns No Content" in {
        stubGetAllocatedMtdVatEnrolmentStatus(testVatNumber)(NO_CONTENT)

        val res = connector.getAllocatedEnrolments(mtdVatEnrolmentKey(testVatNumber))

        await(res) shouldBe Right(EnrolmentNotAllocated)
      }
    }

    "Return EnrolmentStoreProxyFailure and status code" when {
      "EnrolmentStoreProxy ES1 returns Bad Request" in {
        stubGetAllocatedMtdVatEnrolmentStatus(testVatNumber)(BAD_REQUEST)

        val res = connector.getAllocatedEnrolments(mtdVatEnrolmentKey(testVatNumber))

        await(res) shouldBe Left(EnrolmentStoreProxyFailure(BAD_REQUEST))
      }
    }
  }
}
