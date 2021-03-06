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
import play.api.test.Helpers._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.vatsignup.helpers.ComponentSpecBase
import uk.gov.hmrc.vatsignup.helpers.IntegrationTestConstants._
import uk.gov.hmrc.vatsignup.helpers.servicemocks.EnrolmentStoreProxyStub._
import uk.gov.hmrc.vatsignup.httpparsers.AllocateEnrolmentResponseHttpParser.{EnrolSuccess, MultipleEnrolmentsInvalid, UnexpectedEnrolFailure}
import uk.gov.hmrc.vatsignup.httpparsers.AssignEnrolmentToUserHttpParser.EnrolmentAssigned
import uk.gov.hmrc.vatsignup.httpparsers.EnrolmentStoreProxyHttpParser._
import uk.gov.hmrc.vatsignup.httpparsers.QueryUsersHttpParser._
import uk.gov.hmrc.vatsignup.httpparsers.UpsertEnrolmentResponseHttpParser.{UpsertEnrolmentFailure, UpsertEnrolmentSuccess}
import uk.gov.hmrc.vatsignup.utils.EnrolmentUtils._


class EnrolmentStoreProxyConnectorISpec extends ComponentSpecBase {

  lazy val connector: EnrolmentStoreProxyConnector = app.injector.instanceOf[EnrolmentStoreProxyConnector]

  private implicit val headerCarrier: HeaderCarrier = HeaderCarrier()

  "GetUserIds" should {
    "Return UsersFound and a Set of User IDs" when {
      "EnrolmentStoreProxy ES0 returns OK and Json response" in {
        stubGetUserIds(testVatNumber)(OK)

        val res = connector.getUserIds(testVatNumber)

        await(res) shouldBe Right(UsersFound(Set(testCredentialId, testCredentialId2, testCredentialId3)))
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
      "EnrolmentStoreProxy ES1 returns an OK and Json Response" when {
        "ignore assignments is set" in {
          stubGetAllocatedMtdVatEnrolmentStatus(testVatNumber, ignoreAssignments = true)(OK)

          val res = connector.getAllocatedEnrolments(mtdVatEnrolmentKey(testVatNumber), ignoreAssignments = true)

          await(res) shouldBe Right(EnrolmentAlreadyAllocated(testGroupId))
        }
      }
    }

    "Return EnrolmentNotAllocated" when {
      "EnrolmentStoreProxy ES1 returns No Content" in {
        stubGetAllocatedMtdVatEnrolmentStatus(testVatNumber, ignoreAssignments = false)(NO_CONTENT)

        val res = connector.getAllocatedEnrolments(mtdVatEnrolmentKey(testVatNumber), ignoreAssignments = false)

        await(res) shouldBe Right(EnrolmentNotAllocated)
      }
    }

    "Return EnrolmentStoreProxyFailure and status code" when {
      "EnrolmentStoreProxy ES1 returns Bad Request" in {
        stubGetAllocatedMtdVatEnrolmentStatus(testVatNumber, ignoreAssignments = false)(BAD_REQUEST)

        val res = connector.getAllocatedEnrolments(mtdVatEnrolmentKey(testVatNumber), ignoreAssignments = false)

        await(res) shouldBe Left(EnrolmentStoreProxyFailure(BAD_REQUEST))
      }
    }
  }

  "allocateEnrolmentWithoutKnownFacts" when {
    "Enrolment Store Proxy returns a Created" should {
      "return an EnrolSuccess" in {
        stubAllocateEnrolmentWithoutKnownFacts(testVatNumber, testGroupId, testCredentialId)(CREATED)

        val res = connector.allocateEnrolmentWithoutKnownFacts(testGroupId, testCredentialId, testVatNumber)

        await(res) shouldBe Right(EnrolSuccess)
      }
    }

    "Enrolment Store Proxy returns a Bad Request" should {
      "return an EnrolFailure" in {
        stubAllocateEnrolmentWithoutKnownFacts(testVatNumber, testGroupId, testCredentialId)(BAD_REQUEST)

        val res = connector.allocateEnrolmentWithoutKnownFacts(testGroupId, testCredentialId, testVatNumber)

        await(res) shouldBe Left(UnexpectedEnrolFailure(""))
      }
    }

    "Enrolment Store Proxy returns a multiple enrolments invalid failure" should {
      "return MultipleEnrolmentsInvalid" in {
        stubAllocateEnrolmentWithoutKnownFactsFailure(testVatNumber, testGroupId, testCredentialId)(
          CONFLICT,
          Json.obj("code" -> "MULTIPLE_ENROLMENTS_INVALID")
        )

        val res = connector.allocateEnrolmentWithoutKnownFacts(testGroupId, testCredentialId, testVatNumber)

        await(res) shouldBe Left(MultipleEnrolmentsInvalid)
      }
    }
  }

  "assignEnrolment" when {
    "Enrolment Store Proxy returns a Created" should {
      "return an EnrolSuccess" in {
        stubAssignEnrolment(vatNumber = testVatNumber, userId = testCredentialId)(CREATED)

        val res = connector.assignEnrolment(testCredentialId, testVatNumber)

        await(res) shouldBe Right(EnrolmentAssigned)
      }
    }
  }

  "upsertEnrolment" when {
    "Enrolment Store Proxy returns a successful response" should {
      "return an EnrolSuccess when there is a postcode" in {
        stubUpsertEnrolment(testVatNumber, Some(testPostCode), testDateOfRegistration)(NO_CONTENT)

        val res = connector.upsertEnrolment(testVatNumber, Some(testPostCode), testDateOfRegistration)

        await(res) shouldBe Right(UpsertEnrolmentSuccess)
      }

      "return an EnrolSuccess when there is not a postcode" in {
        stubUpsertEnrolment(testVatNumber, None, testDateOfRegistration)(NO_CONTENT)

        val res = connector.upsertEnrolment(testVatNumber, None, testDateOfRegistration)

        await(res) shouldBe Right(UpsertEnrolmentSuccess)
      }
    }

    "Enrolment Store Proxy returns an unsuccessful response" should {
      "return an EnrolFailure when there is a postcode" in {
        stubUpsertEnrolment(testVatNumber, Some(testPostCode), testDateOfRegistration)(BAD_REQUEST)

        val res = connector.upsertEnrolment(testVatNumber, Some(testPostCode), testDateOfRegistration)

        await(res) shouldBe Left(UpsertEnrolmentFailure(BAD_REQUEST, ""))
      }

      "return an EnrolFailure when there is not postcode" in {
        stubUpsertEnrolment(testVatNumber, None, testDateOfRegistration)(BAD_REQUEST)

        val res = connector.upsertEnrolment(testVatNumber, None, testDateOfRegistration)

        await(res) shouldBe Left(UpsertEnrolmentFailure(BAD_REQUEST, ""))
      }
    }
  }
}
