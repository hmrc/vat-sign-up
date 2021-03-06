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

package uk.gov.hmrc.vatsignup.controllers

import play.api.libs.json.Json
import play.api.test.Helpers._
import uk.gov.hmrc.vatsignup.helpers.IntegrationTestConstants._
import uk.gov.hmrc.vatsignup.helpers.servicemocks.{EnrolmentStoreProxyStub, KnownFactsStub, UsersGroupsSearchStub}
import uk.gov.hmrc.vatsignup.helpers.{ComponentSpecBase, CustomMatchers}
import uk.gov.hmrc.vatsignup.utils.KnownFactsDateFormatter.KnownFactsDateFormatter

class BulkMigrationAutoClaimEnrolmentControllerISpec extends ComponentSpecBase with CustomMatchers {

  s"/migration-notification/vat-number/$testVatNumber" should {
    "successfully add the enrolment and return NO_CONTENT" in {
      EnrolmentStoreProxyStub.stubGetAllocatedMtdVatEnrolmentStatus(testVatNumber, ignoreAssignments = true)(NO_CONTENT)
      EnrolmentStoreProxyStub.stubGetAllocatedLegacyVatEnrolmentStatus(testVatNumber, ignoreAssignments = false)(OK)
      EnrolmentStoreProxyStub.stubGetUserId(testVatNumber)(OK)
      UsersGroupsSearchStub.stubGetUsersForGroup(testGroupId)(NON_AUTHORITATIVE_INFORMATION, UsersGroupsSearchStub.successfulResponseBody)
      KnownFactsStub.stubSuccessGetKnownFacts(testVatNumber)
      EnrolmentStoreProxyStub.stubUpsertEnrolment(testVatNumber, Some(testPostCode), testDateOfRegistration.toTaxEnrolmentsFormat)(NO_CONTENT)
      EnrolmentStoreProxyStub.stubAllocateEnrolmentWithoutKnownFacts(testVatNumber, testGroupId, testCredentialId)(CREATED)

      val res = post(s"/migration-notification/vat-number/$testVatNumber")(Json.obj(), basicAuthHeader)
      res should have(
        httpStatus(NO_CONTENT)
      )

      EnrolmentStoreProxyStub.verifyUpsertEnrolment(testVatNumber, testPostCode, testDateOfRegistration.toTaxEnrolmentsFormat)
      EnrolmentStoreProxyStub.verifyAllocateEnrolmentWithoutKnownFacts(testVatNumber, testGroupId, testCredentialId)
    }

    "return NO_CONTENT if group ids containing the MTDVAT enrolments are found" in {
      EnrolmentStoreProxyStub.stubGetAllocatedMtdVatEnrolmentStatus(testVatNumber, ignoreAssignments = true)(OK)

      val res = post(s"/migration-notification/vat-number/$testVatNumber")(Json.obj(), basicAuthHeader)
      res should have(
        httpStatus(NO_CONTENT)
      )
    }

    "return NO_CONTENT if no group ids containing the Legacy and MTDVAT enrolments are found" in {
      EnrolmentStoreProxyStub.stubGetAllocatedMtdVatEnrolmentStatus(testVatNumber, ignoreAssignments = true)(NO_CONTENT)
      EnrolmentStoreProxyStub.stubGetAllocatedLegacyVatEnrolmentStatus(testVatNumber, ignoreAssignments = false)(NO_CONTENT)

      val res = post(s"/migration-notification/vat-number/$testVatNumber")(Json.obj(), basicAuthHeader)
      res should have(
        httpStatus(NO_CONTENT)
      )
    }

    "return NO_CONTENT if no user ids containing the legacy enrolment are found" in {
      EnrolmentStoreProxyStub.stubGetAllocatedMtdVatEnrolmentStatus(testVatNumber, ignoreAssignments = true)(NO_CONTENT)
      EnrolmentStoreProxyStub.stubGetAllocatedLegacyVatEnrolmentStatus(testVatNumber, ignoreAssignments = false)(OK)
      EnrolmentStoreProxyStub.stubGetUserId(testVatNumber)(NO_CONTENT)

      val res = post(s"/migration-notification/vat-number/$testVatNumber")(Json.obj(), basicAuthHeader)
      res should have(
        httpStatus(NO_CONTENT)
      )
    }

    "return NO_CONTENT if the found user group already has an enrolment of the same type" in {
      EnrolmentStoreProxyStub.stubGetAllocatedMtdVatEnrolmentStatus(testVatNumber, ignoreAssignments = true)(NO_CONTENT)
      EnrolmentStoreProxyStub.stubGetAllocatedLegacyVatEnrolmentStatus(testVatNumber, ignoreAssignments = false)(OK)
      EnrolmentStoreProxyStub.stubGetUserId(testVatNumber)(OK)
      UsersGroupsSearchStub.stubGetUsersForGroup(testGroupId)(NON_AUTHORITATIVE_INFORMATION, UsersGroupsSearchStub.successfulResponseBody)
      KnownFactsStub.stubSuccessGetKnownFacts(testVatNumber)
      EnrolmentStoreProxyStub.stubUpsertEnrolment(testVatNumber, Some(testPostCode), testDateOfRegistration.toTaxEnrolmentsFormat)(NO_CONTENT)
      EnrolmentStoreProxyStub.stubAllocateEnrolmentWithoutKnownFactsFailure(testVatNumber, testGroupId, testCredentialId)(
        CONFLICT,
        Json.obj("code" -> "MULTIPLE_ENROLMENTS_INVALID")
      )

      val res = post(s"/migration-notification/vat-number/$testVatNumber")(Json.obj(), basicAuthHeader)
      res should have(
        httpStatus(NO_CONTENT)
      )
    }

    "throw an exception" in {
      EnrolmentStoreProxyStub.stubGetAllocatedMtdVatEnrolmentStatus(testVatNumber, ignoreAssignments = true)(NO_CONTENT)
      EnrolmentStoreProxyStub.stubGetAllocatedLegacyVatEnrolmentStatus(testVatNumber, ignoreAssignments = false)(OK)
      EnrolmentStoreProxyStub.stubGetUserId(testVatNumber)(OK)
      UsersGroupsSearchStub.stubGetUsersForGroup(testGroupId)(NON_AUTHORITATIVE_INFORMATION, UsersGroupsSearchStub.successfulResponseBody)
      KnownFactsStub.stubSuccessGetKnownFacts(testVatNumber)
      EnrolmentStoreProxyStub.stubUpsertEnrolment(testVatNumber, Some(testPostCode), testDateOfRegistration.toTaxEnrolmentsFormat)(NO_CONTENT)
      EnrolmentStoreProxyStub.stubAllocateEnrolmentWithoutKnownFacts(testVatNumber, testGroupId, testCredentialId)(INTERNAL_SERVER_ERROR)

      val res = post(s"/migration-notification/vat-number/$testVatNumber")(Json.obj(), basicAuthHeader)

      res should have(
        httpStatus(INTERNAL_SERVER_ERROR)
      )

    }

    "return UNAUTHORIZED if the correct authentication details are not supplied" in {
      val res = post(s"/migration-notification/vat-number/$testVatNumber")(Json.obj())

      res should have(
        httpStatus(UNAUTHORIZED),
        httpHeader(WWW_AUTHENTICATE -> "Basic realm=\"realm\"")
      )
    }
  }
}
