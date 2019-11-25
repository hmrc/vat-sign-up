package uk.gov.hmrc.vatsignup.controllers

import play.api.libs.json.Json
import play.api.test.Helpers._
import uk.gov.hmrc.vatsignup.helpers.IntegrationTestConstants._
import uk.gov.hmrc.vatsignup.helpers.servicemocks.{EnrolmentStoreProxyStub, KnownFactsStub, UsersGroupsSearchStub}
import uk.gov.hmrc.vatsignup.helpers.{ComponentSpecBase, CustomMatchers}
import uk.gov.hmrc.vatsignup.utils.KnownFactsDateFormatter.KnownFactsDateFormatter


class BulkMIgrationAutoClaimEnrolmentControllerISpec extends ComponentSpecBase with CustomMatchers {


  s"/migration-notification/vat-number/$testVatNumber" should {
    "successfully add the enrolment and return NO_CONTENT" in {
      EnrolmentStoreProxyStub.stubGetAllocatedLegacyVatEnrolmentStatus(testVatNumber)(OK)
      EnrolmentStoreProxyStub.stubGetUserId(testVatNumber)(OK)
      UsersGroupsSearchStub.stubGetUsersForGroup(testGroupId)(NON_AUTHORITATIVE_INFORMATION, UsersGroupsSearchStub.successfulResponseBody)
      KnownFactsStub.stubSuccessGetKnownFacts(testVatNumber)
      EnrolmentStoreProxyStub.stubUpsertEnrolment(testVatNumber, testPostCode, testDateOfRegistration.toTaxEnrolmentsFormat)(NO_CONTENT)
      EnrolmentStoreProxyStub.stubAllocateEnrolmentWithoutKnownFacts(testVatNumber, testGroupId, testCredentialId)(CREATED)

      val res = post(s"/migration-notification/vat-number/$testVatNumber")(Json.obj())
      res should have(
        httpStatus(NO_CONTENT)
      )

      EnrolmentStoreProxyStub.verifyUpsertEnrolment(testVatNumber, testPostCode, testDateOfRegistration.toTaxEnrolmentsFormat)
      EnrolmentStoreProxyStub.verifyAllocateEnrolmentWithoutKnownFacts(testVatNumber, testGroupId, testCredentialId)
    }

    "return NO_CONTENT if no group ids are found" in {
      EnrolmentStoreProxyStub.stubGetAllocatedLegacyVatEnrolmentStatus(testVatNumber)(NO_CONTENT)
      val res = post(s"/migration-notification/vat-number/$testVatNumber")(Json.obj())
      res should have(
        httpStatus(NO_CONTENT)
      )
    }

    "return NO_CONTENT if no user ids are found" in {
      EnrolmentStoreProxyStub.stubGetAllocatedLegacyVatEnrolmentStatus(testVatNumber)(OK)
      EnrolmentStoreProxyStub.stubGetUserId(testVatNumber)(NO_CONTENT)

      val res = post(s"/migration-notification/vat-number/$testVatNumber")(Json.obj())
      res should have(
        httpStatus(NO_CONTENT)
      )
    }

    "throw an exception" in {
      EnrolmentStoreProxyStub.stubGetAllocatedLegacyVatEnrolmentStatus(testVatNumber)(OK)
      EnrolmentStoreProxyStub.stubGetUserId(testVatNumber)(OK)
      UsersGroupsSearchStub.stubGetUsersForGroup(testGroupId)(NON_AUTHORITATIVE_INFORMATION, UsersGroupsSearchStub.successfulResponseBody)
      KnownFactsStub.stubSuccessGetKnownFacts(testVatNumber)
      EnrolmentStoreProxyStub.stubUpsertEnrolment(testVatNumber, testPostCode, testDateOfRegistration.toTaxEnrolmentsFormat)(NO_CONTENT)
      EnrolmentStoreProxyStub.stubAllocateEnrolmentWithoutKnownFacts(testVatNumber, testGroupId, testCredentialId)(INTERNAL_SERVER_ERROR)

      val res = post(s"/migration-notification/vat-number/$testVatNumber")(Json.obj())

      res should have(
        httpStatus(INTERNAL_SERVER_ERROR)
      )

    }
  }
}
