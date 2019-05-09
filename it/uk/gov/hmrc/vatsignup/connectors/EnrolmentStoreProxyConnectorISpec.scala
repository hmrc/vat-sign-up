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
import uk.gov.hmrc.vatsignup.helpers.IntegrationTestConstants._
import uk.gov.hmrc.vatsignup.helpers.servicemocks.EnrolmentStoreProxyStub
import uk.gov.hmrc.vatsignup.helpers.servicemocks.EnrolmentStoreProxyStub.testGroupID1
import uk.gov.hmrc.vatsignup.httpparsers.EnrolmentStoreProxyHttpParser._
import uk.gov.hmrc.vatsignup.utils.EnrolmentUtils._


class EnrolmentStoreProxyConnectorISpec extends ComponentSpecBase {

  lazy val connector: EnrolmentStoreProxyConnector = app.injector.instanceOf[EnrolmentStoreProxyConnector]

  private implicit val headerCarrier: HeaderCarrier = HeaderCarrier()

  "The enrolment is already allocated" when {
    "EnrolmentStoreProxy ES1 returns an OK and Json Response" should {
      "return a group ID" in {
        EnrolmentStoreProxyStub.stubGetAllocatedMtdVatEnrolmentStatus(testVatNumber)(OK)

        val res = connector.getAllocatedEnrolments(mtdVatEnrolmentKey(testVatNumber))

        await(res) shouldBe Right(EnrolmentAlreadyAllocated(testGroupID1))
      }
    }
  }

  "The enrolment is not allocated" when {
    "EnrolmentStoreProxy ES1 returns No Content" should {
      "return a success" in {
        EnrolmentStoreProxyStub.stubGetAllocatedMtdVatEnrolmentStatus(testVatNumber)(NO_CONTENT)

        val res = connector.getAllocatedEnrolments(mtdVatEnrolmentKey(testVatNumber))

        await(res) shouldBe Right(EnrolmentNotAllocated)

      }
    }
  }

  "The request is invalid" when {
    "EnrolmentStoreProxy ES1 returns Bad Request" should {
      "return a success" in {
        EnrolmentStoreProxyStub.stubGetAllocatedMtdVatEnrolmentStatus(testVatNumber)(BAD_REQUEST)

        val res = connector.getAllocatedEnrolments(mtdVatEnrolmentKey(testVatNumber))

        await(res) shouldBe Left(EnrolmentStoreProxyFailure(BAD_REQUEST))

      }
    }
  }
}
