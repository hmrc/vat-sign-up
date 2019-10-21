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

package uk.gov.hmrc.vatsignup.controllers

import play.api.http.Status._
import play.api.libs.json.Json
import uk.gov.hmrc.vatsignup.helpers.IntegrationTestConstants._
import uk.gov.hmrc.vatsignup.helpers.servicemocks.AuthStub._
import uk.gov.hmrc.vatsignup.helpers.servicemocks.EmailVerificationStub._
import uk.gov.hmrc.vatsignup.helpers.servicemocks.RegistrationStub._
import uk.gov.hmrc.vatsignup.helpers.servicemocks.SignUpStub._
import uk.gov.hmrc.vatsignup.helpers.servicemocks.TaxEnrolmentsStub._
import uk.gov.hmrc.vatsignup.helpers.{ComponentSpecBase, CustomMatchers, TestSubmissionRequestRepository}
import uk.gov.hmrc.vatsignup.models._

import scala.concurrent.ExecutionContext.Implicits.global

class MigratedSubmissionControllerISpec extends ComponentSpecBase with CustomMatchers with TestSubmissionRequestRepository {

  "/subscription-request/migrated/vat-number/:vatNumber/submit" when {
    "the user is a delegate and" when {
      "all downstream services behave as expected" should {
        s"return $NO_CONTENT for individual sign up" in {

          val testSubscriptionRequest = SubscriptionRequest(
            vatNumber = testVatNumber,
            businessEntity = Some(SoleTrader(testNino)),
            ninoSource = Some(UserEntered),
            email = Some(testEmail),
            isMigratable = true,
            isDirectDebit = false,
            contactPreference = Some(Paper)
          )

          stubAuth(OK, successfulAuthResponse(agentEnrolment))
          stubGetEmailVerified(testEmail)
          stubRegisterBusinessEntity(testVatNumber, SoleTrader(testNino))(testSafeId)
          stubMigratedSignUp(
            testSafeId,
            testVatNumber
          )(OK)
          stubRegisterEnrolment(testVatNumber, testSafeId)(NO_CONTENT)

          await(submissionRequestRepo.insert(testSubscriptionRequest))
          val res = await(post(s"/subscription-request/migrated/vat-number/$testVatNumber/submit")(Json.obj()))

          res should have(
            httpStatus(NO_CONTENT),
            emptyBody
          )
        }

        s"return $NO_CONTENT for individual sign up where the user is not direct debit and has paper preference" in {

          val testSubscriptionRequest = SubscriptionRequest(
            vatNumber = testVatNumber,
            businessEntity = Some(SoleTrader(testNino)),
            ninoSource = Some(UserEntered),
            transactionEmail = Some(testEmail),
            email = None,
            isMigratable = true,
            isDirectDebit = false,
            contactPreference = Some(Paper)
          )

          stubAuth(OK, successfulAuthResponse(agentEnrolment))
          stubGetEmailVerified(testEmail)
          stubRegisterBusinessEntity(testVatNumber, SoleTrader(testNino))(testSafeId)
          stubMigratedSignUp(
            testSafeId,
            testVatNumber
          )(OK)
          stubRegisterEnrolment(testVatNumber, testSafeId)(NO_CONTENT)

          await(submissionRequestRepo.insert(testSubscriptionRequest))
          val res = await(post(s"/subscription-request/migrated/vat-number/$testVatNumber/submit")(Json.obj()))

          res should have(
            httpStatus(NO_CONTENT),
            emptyBody
          )
        }

        s"return $NO_CONTENT for individual sign up when ninoSource is IRSA" in {
          val testSubscriptionRequest = SubscriptionRequest(
            vatNumber = testVatNumber,
            businessEntity = Some(SoleTrader(testNino)),
            ninoSource = Some(IRSA),
            email = Some(testEmail),
            isMigratable = true,
            isDirectDebit = false,
            contactPreference = Some(testContactPreference)
          )

          stubAuth(OK, successfulAuthResponse(agentEnrolment))
          stubGetEmailVerified(testEmail)
          stubRegisterBusinessEntity(testVatNumber, SoleTrader(testNino))(testSafeId)
          stubMigratedSignUp(
            testSafeId,
            testVatNumber
          )(OK)
          stubRegisterEnrolment(testVatNumber, testSafeId)(NO_CONTENT)

          await(submissionRequestRepo.insert(testSubscriptionRequest))
          val res = await(post(s"/subscription-request/migrated/vat-number/$testVatNumber/submit")(Json.obj()))

          res should have(
            httpStatus(NO_CONTENT),
            emptyBody
          )
        }

        s"return $NO_CONTENT for individual sign up when ninoSource is Auth Profile" in {
          val testSubscriptionRequest = SubscriptionRequest(
            vatNumber = testVatNumber,
            businessEntity = Some(SoleTrader(testNino)),
            ninoSource = Some(AuthProfile),
            email = Some(testEmail),
            isMigratable = true,
            isDirectDebit = false,
            contactPreference = Some(testContactPreference)
          )

          stubAuth(OK, successfulAuthResponse(agentEnrolment))
          stubGetEmailVerified(testEmail)
          stubRegisterBusinessEntity(testVatNumber, SoleTrader(testNino))(testSafeId)
          stubRegisterEnrolment(testVatNumber, testSafeId)(NO_CONTENT)
          stubMigratedSignUp(
            testSafeId,
            testVatNumber
          )(OK)
          await(submissionRequestRepo.insert(testSubscriptionRequest))
          val res = await(post(s"/subscription-request/migrated/vat-number/$testVatNumber/submit")(Json.obj()))

          res should have(
            httpStatus(NO_CONTENT),
            emptyBody
          )
        }

        s"return $NO_CONTENT for $LimitedCompany sign up" in {
          val testSubscriptionRequest = SubscriptionRequest(
            vatNumber = testVatNumber,
            businessEntity = Some(LimitedCompany(testCompanyNumber)),
            email = Some(testEmail),
            isMigratable = true,
            isDirectDebit = false,
            contactPreference = Some(testContactPreference)
          )

          stubAuth(OK, successfulAuthResponse(agentEnrolment))
          stubGetEmailVerified(testEmail)
          stubRegisterBusinessEntity(testVatNumber, LimitedCompany(testCompanyNumber))(testSafeId)
          stubMigratedSignUp(
            testSafeId,
            testVatNumber
          )(OK)
          stubRegisterEnrolment(testVatNumber, testSafeId)(NO_CONTENT)

          await(submissionRequestRepo.insert(testSubscriptionRequest))
          val res = await(post(s"/subscription-request/migrated/vat-number/$testVatNumber/submit")(Json.obj()))

          res should have(
            httpStatus(NO_CONTENT),
            emptyBody
          )
        }

        s"return $NO_CONTENT for $OverseasWithUkEstablishment sign up with FC prefix in CRN" in {
          val testSubscriptionRequest = SubscriptionRequest(
            vatNumber = testVatNumber,
            businessEntity = Some(OverseasWithUkEstablishment(testNonUKCompanyWithUKEstablishmentCompanyNumberFC)),
            email = Some(testEmail),
            isMigratable = true,
            isDirectDebit = false,
            contactPreference = Some(testContactPreference)
          )

          stubAuth(OK, successfulAuthResponse(agentEnrolment))
          stubGetEmailVerified(testEmail)
          stubRegisterBusinessEntity(testVatNumber, OverseasWithUkEstablishment(testNonUKCompanyWithUKEstablishmentCompanyNumberFC))(testSafeId)
          stubMigratedSignUp(
            testSafeId,
            testVatNumber
          )(OK)
          stubRegisterEnrolment(testVatNumber, testSafeId)(NO_CONTENT)

          await(submissionRequestRepo.insert(testSubscriptionRequest))
          val res = await(post(s"/subscription-request/migrated/vat-number/$testVatNumber/submit")(Json.obj()))

          res should have(
            httpStatus(NO_CONTENT),
            emptyBody
          )
        }

        s"return $NO_CONTENT for $OverseasWithUkEstablishment sign up with SF prefix in CRN" in {
          val testSubscriptionRequest = SubscriptionRequest(
            vatNumber = testVatNumber,
            businessEntity = Some(OverseasWithUkEstablishment(testNonUKCompanyWithUKEstablishmentCompanyNumberSF)),
            email = Some(testEmail),
            isMigratable = true,
            isDirectDebit = false,
            contactPreference = Some(testContactPreference)
          )

          stubAuth(OK, successfulAuthResponse(agentEnrolment))
          stubGetEmailVerified(testEmail)
          stubRegisterBusinessEntity(testVatNumber, OverseasWithUkEstablishment(testNonUKCompanyWithUKEstablishmentCompanyNumberSF))(testSafeId)
          stubMigratedSignUp(
            testSafeId,
            testVatNumber
          )(OK)
          stubRegisterEnrolment(testVatNumber, testSafeId)(NO_CONTENT)

          await(submissionRequestRepo.insert(testSubscriptionRequest))
          val res = await(post(s"/subscription-request/migrated/vat-number/$testVatNumber/submit")(Json.obj()))

          res should have(
            httpStatus(NO_CONTENT),
            emptyBody
          )
        }

        s"return $NO_CONTENT for $OverseasWithUkEstablishment sign up with NF prefix in CRN" in {
          val testSubscriptionRequest = SubscriptionRequest(
            vatNumber = testVatNumber,
            businessEntity = Some(OverseasWithUkEstablishment(testNonUKCompanyWithUKEstablishmentCompanyNumberNF)),
            email = Some(testEmail),
            isMigratable = true,
            isDirectDebit = false,
            contactPreference = Some(testContactPreference)
          )

          stubAuth(OK, successfulAuthResponse(agentEnrolment))
          stubGetEmailVerified(testEmail)
          stubRegisterBusinessEntity(testVatNumber, OverseasWithUkEstablishment(testNonUKCompanyWithUKEstablishmentCompanyNumberNF))(testSafeId)
          stubMigratedSignUp(
            testSafeId,
            testVatNumber
          )(OK)
          stubRegisterEnrolment(testVatNumber, testSafeId)(NO_CONTENT)

          await(submissionRequestRepo.insert(testSubscriptionRequest))
          val res = await(post(s"/subscription-request/migrated/vat-number/$testVatNumber/submit")(Json.obj()))

          res should have(
            httpStatus(NO_CONTENT),
            emptyBody
          )
        }
        s"return $NO_CONTENT for $GeneralPartnership sign up" in {
          val testSubscriptionRequest = SubscriptionRequest(
            vatNumber = testVatNumber,
            businessEntity = Some(GeneralPartnership(Some(testUtr))),
            email = Some(testEmail),
            isMigratable = true,
            isDirectDebit = false,
            contactPreference = Some(testContactPreference)
          )

          stubAuth(OK, successfulAuthResponse(agentEnrolment))
          stubGetEmailVerified(testEmail)
          stubRegisterBusinessEntity(testVatNumber, GeneralPartnership(Some(testUtr)))(testSafeId)
          stubMigratedSignUp(
            testSafeId,
            testVatNumber
          )(OK)
          stubRegisterEnrolment(testVatNumber, testSafeId)(NO_CONTENT)

          await(submissionRequestRepo.insert(testSubscriptionRequest))
          val res = await(post(s"/subscription-request/migrated/vat-number/$testVatNumber/submit")(Json.obj()))

          res should have(
            httpStatus(NO_CONTENT),
            emptyBody
          )
        }
        s"return $NO_CONTENT for $LimitedPartnership sign up" in {
          val testSubscriptionRequest = SubscriptionRequest(
            vatNumber = testVatNumber,
            businessEntity = Some(LimitedPartnership(Some(testUtr), testCompanyNumber)),
            email = Some(testEmail),
            isMigratable = true,
            isDirectDebit = false,
            contactPreference = Some(testContactPreference)
          )

          stubAuth(OK, successfulAuthResponse(agentEnrolment))
          stubGetEmailVerified(testEmail)
          stubRegisterBusinessEntity(testVatNumber, LimitedPartnership(Some(testUtr), testCompanyNumber))(testSafeId)
          stubMigratedSignUp(
            testSafeId,
            testVatNumber
          )(OK)
          stubRegisterEnrolment(testVatNumber, testSafeId)(NO_CONTENT)

          await(submissionRequestRepo.insert(testSubscriptionRequest))
          val res = await(post(s"/subscription-request/migrated/vat-number/$testVatNumber/submit")(Json.obj()))

          res should have(
            httpStatus(NO_CONTENT),
            emptyBody
          )
        }
        s"return $NO_CONTENT for $LimitedLiabilityPartnership sign up" in {
          val testSubscriptionRequest = SubscriptionRequest(
            vatNumber = testVatNumber,
            businessEntity = Some(LimitedLiabilityPartnership(Some(testUtr), testCompanyNumber)),
            email = Some(testEmail),
            isMigratable = true,
            isDirectDebit = false,
            contactPreference = Some(testContactPreference)
          )

          stubAuth(OK, successfulAuthResponse(agentEnrolment))
          stubGetEmailVerified(testEmail)
          stubRegisterBusinessEntity(testVatNumber, LimitedLiabilityPartnership(Some(testUtr), testCompanyNumber))(testSafeId)
          stubMigratedSignUp(
            testSafeId,
            testVatNumber
          )(OK)
          stubRegisterEnrolment(testVatNumber, testSafeId)(NO_CONTENT)

          await(submissionRequestRepo.insert(testSubscriptionRequest))
          val res = await(post(s"/subscription-request/migrated/vat-number/$testVatNumber/submit")(Json.obj()))

          res should have(
            httpStatus(NO_CONTENT),
            emptyBody
          )
        }
        s"return $NO_CONTENT for $ScottishLimitedPartnership sign up" in {
          val testSubscriptionRequest = SubscriptionRequest(
            vatNumber = testVatNumber,
            businessEntity = Some(ScottishLimitedPartnership(Some(testUtr), testCompanyNumber)),
            email = Some(testEmail),
            isMigratable = true,
            isDirectDebit = false,
            contactPreference = Some(testContactPreference)
          )

          stubAuth(OK, successfulAuthResponse(agentEnrolment))
          stubGetEmailVerified(testEmail)
          stubRegisterBusinessEntity(testVatNumber, ScottishLimitedPartnership(Some(testUtr), testCompanyNumber))(testSafeId)
          stubMigratedSignUp(
            testSafeId,
            testVatNumber
          )(OK)
          stubRegisterEnrolment(testVatNumber, testSafeId)(NO_CONTENT)

          await(submissionRequestRepo.insert(testSubscriptionRequest))
          val res = await(post(s"/subscription-request/migrated/vat-number/$testVatNumber/submit")(Json.obj()))

          res should have(
            httpStatus(NO_CONTENT),
            emptyBody
          )
        }
        s"return $NO_CONTENT for $VatGroup sign up" in {
          val testSubscriptionRequest = SubscriptionRequest(
            vatNumber = testVatNumber,
            businessEntity = Some(VatGroup),
            email = Some(testEmail),
            isMigratable = true,
            isDirectDebit = false,
            contactPreference = Some(testContactPreference)
          )

          stubAuth(OK, successfulAuthResponse(agentEnrolment))
          stubGetEmailVerified(testEmail)
          stubRegisterBusinessEntity(testVatNumber, VatGroup)(testSafeId)
          stubMigratedSignUp(
            testSafeId,
            testVatNumber
          )(OK)
          stubRegisterEnrolment(testVatNumber, testSafeId)(NO_CONTENT)

          await(submissionRequestRepo.insert(testSubscriptionRequest))
          val res = await(post(s"/subscription-request/migrated/vat-number/$testVatNumber/submit")(Json.obj()))

          res should have(
            httpStatus(NO_CONTENT),
            emptyBody
          )
        }
        s"return $NO_CONTENT for $Trust sign up" in {
          val testSubscriptionRequest = SubscriptionRequest(
            vatNumber = testVatNumber,
            businessEntity = Some(Trust),
            email = Some(testEmail),
            isMigratable = true,
            isDirectDebit = false,
            contactPreference = Some(testContactPreference)
          )

          stubAuth(OK, successfulAuthResponse(agentEnrolment))
          stubGetEmailVerified(testEmail)
          stubRegisterBusinessEntity(testVatNumber, Trust)(testSafeId)
          stubMigratedSignUp(
            testSafeId,
            testVatNumber
          )(OK)
          stubRegisterEnrolment(testVatNumber, testSafeId)(NO_CONTENT)

          await(submissionRequestRepo.insert(testSubscriptionRequest))
          val res = await(post(s"/subscription-request/migrated/vat-number/$testVatNumber/submit")(Json.obj()))

          res should have(
            httpStatus(NO_CONTENT),
            emptyBody
          )
        }
        s"return $NO_CONTENT for $Overseas sign up" in {
          val testSubscriptionRequest = SubscriptionRequest(
            vatNumber = testVatNumber,
            businessEntity = Some(Overseas),
            email = Some(testEmail),
            isMigratable = true,
            isDirectDebit = false,
            contactPreference = Some(testContactPreference)
          )

          stubAuth(OK, successfulAuthResponse(agentEnrolment))
          stubGetEmailVerified(testEmail)
          stubRegisterBusinessEntity(testVatNumber, Overseas)(testSafeId)
          stubMigratedSignUp(
            testSafeId,
            testVatNumber
          )(OK)
          stubRegisterEnrolment(testVatNumber, testSafeId)(NO_CONTENT)

          await(submissionRequestRepo.insert(testSubscriptionRequest))
          val res = await(post(s"/subscription-request/migrated/vat-number/$testVatNumber/submit")(Json.obj()))

          res should have(
            httpStatus(NO_CONTENT),
            emptyBody
          )
        }
        s"return $NO_CONTENT for $JointVenture sign up" in {
          val testSubscriptionRequest = SubscriptionRequest(
            vatNumber = testVatNumber,
            businessEntity = Some(JointVenture),
            email = Some(testEmail),
            isMigratable = true,
            isDirectDebit = false,
            contactPreference = Some(testContactPreference)
          )

          stubAuth(OK, successfulAuthResponse(agentEnrolment))
          stubGetEmailVerified(testEmail)
          stubRegisterBusinessEntity(testVatNumber, JointVenture)(testSafeId)
          stubMigratedSignUp(
            testSafeId,
            testVatNumber
          )(OK)
          stubRegisterEnrolment(testVatNumber, testSafeId)(NO_CONTENT)

          await(submissionRequestRepo.insert(testSubscriptionRequest))
          val res = await(post(s"/subscription-request/migrated/vat-number/$testVatNumber/submit")(Json.obj()))

          res should have(
            httpStatus(NO_CONTENT),
            emptyBody
          )
        }
      }
      "transaction email should not be sent to sign up" in {
        val testSubscriptionRequest = SubscriptionRequest(
          vatNumber = testVatNumber,
          businessEntity = Some(LimitedCompany(testCompanyNumber)),
          transactionEmail = Some(testEmail),
          isMigratable = true,
          isDirectDebit = false,
          contactPreference = Some(testContactPreference)
        )

        stubAuth(OK, successfulAuthResponse(agentEnrolment))
        stubGetEmailVerified(testEmail)
        stubRegisterBusinessEntity(testVatNumber, LimitedCompany(testCompanyNumber))(testSafeId)
        stubMigratedSignUp(
          testSafeId,
          testVatNumber
        )(OK)
        stubRegisterEnrolment(testVatNumber, testSafeId)(NO_CONTENT)

        await(submissionRequestRepo.insert(testSubscriptionRequest))
        val res = await(post(s"/subscription-request/migrated/vat-number/$testVatNumber/submit")(Json.obj()))

        res should have(
          httpStatus(NO_CONTENT),
          emptyBody
        )
      }
    }

    "the user is principal and" when {
      "all downstream services behave as expected" should {
        s"return $NO_CONTENT for individual sign up" in {
          val testSubscriptionRequest = SubscriptionRequest(
            vatNumber = testVatNumber,
            businessEntity = Some(SoleTrader(testNino)),
            ninoSource = Some(UserEntered),
            email = Some(testEmail),
            identityVerified = true,
            isMigratable = true,
            isDirectDebit = false,
            contactPreference = Some(testContactPreference)
          )

          stubAuth(OK, successfulAuthResponse(vatDecEnrolment()))
          stubGetEmailVerified(testEmail)
          stubRegisterBusinessEntity(testVatNumber, SoleTrader(testNino))(testSafeId)
          stubMigratedSignUp(
            testSafeId,
            testVatNumber
          )(OK)
          stubRegisterEnrolment(testVatNumber, testSafeId)(NO_CONTENT)

          await(submissionRequestRepo.insert(testSubscriptionRequest))
          val res = await(post(s"/subscription-request/migrated/vat-number/$testVatNumber/submit")(Json.obj()))

          res should have(
            httpStatus(NO_CONTENT),
            emptyBody
          )
        }
      }
      s"the user is signing up a $LimitedCompany" should {
        s"return $NO_CONTENT for company sign up when the user has provided a CT reference" in {
          val testSubscriptionRequest = SubscriptionRequest(
            vatNumber = testVatNumber,
            businessEntity = Some(LimitedCompany(testCompanyNumber)),
            ctReference = Some(testCtReference),
            email = Some(testEmail),
            isMigratable = true,
            isDirectDebit = false,
            contactPreference = Some(testContactPreference)
          )

          stubAuth(OK, successfulAuthResponse(vatDecEnrolment()))
          stubGetEmailVerified(testEmail)
          stubRegisterBusinessEntity(testVatNumber, LimitedCompany(testCompanyNumber))(testSafeId)
          stubMigratedSignUp(
            testSafeId,
            testVatNumber
          )(OK)
          stubRegisterEnrolment(testVatNumber, testSafeId)(NO_CONTENT)

          await(submissionRequestRepo.insert(testSubscriptionRequest))
          val res = await(post(s"/subscription-request/migrated/vat-number/$testVatNumber/submit")(Json.obj()))

          res should have(
            httpStatus(NO_CONTENT),
            emptyBody
          )
        }
        s"return $NO_CONTENT for $LimitedCompany sign up when the user has not provided a CT reference" in {
          val testSubscriptionRequest = SubscriptionRequest(
            vatNumber = testVatNumber,
            businessEntity = Some(LimitedCompany(testCompanyNumber)),
            ctReference = None,
            email = Some(testEmail),
            isMigratable = true,
            isDirectDebit = false,
            contactPreference = Some(testContactPreference)
          )

          stubAuth(OK, successfulAuthResponse(vatDecEnrolment()))
          stubGetEmailVerified(testEmail)
          stubRegisterBusinessEntity(testVatNumber, LimitedCompany(testCompanyNumber))(testSafeId)
          stubMigratedSignUp(
            testSafeId,
            testVatNumber
          )(OK)
          stubRegisterEnrolment(testVatNumber, testSafeId)(NO_CONTENT)

          await(submissionRequestRepo.insert(testSubscriptionRequest))
          val res = await(post(s"/subscription-request/migrated/vat-number/$testVatNumber/submit")(Json.obj()))

          res should have(
            httpStatus(NO_CONTENT),
            emptyBody
          )
        }
      }
      "the user is signing up a non UK with UK establishment with FC prefix and has provided a CT reference" should {
        s"return $NO_CONTENT for $OverseasWithUkEstablishment sign up" in {
          val testSubscriptionRequest = SubscriptionRequest(
            vatNumber = testVatNumber,
            businessEntity = Some(OverseasWithUkEstablishment(testNonUKCompanyWithUKEstablishmentCompanyNumberFC)),
            ctReference = Some(testCtReference),
            email = Some(testEmail),
            isMigratable = true,
            isDirectDebit = false,
            contactPreference = Some(testContactPreference)
          )

          stubAuth(OK, successfulAuthResponse(vatDecEnrolment()))
          stubGetEmailVerified(testEmail)
          stubRegisterBusinessEntity(testVatNumber, OverseasWithUkEstablishment(testNonUKCompanyWithUKEstablishmentCompanyNumberFC))(testSafeId)
          stubMigratedSignUp(
            testSafeId,
            testVatNumber
          )(OK)
          stubRegisterEnrolment(testVatNumber, testSafeId)(NO_CONTENT)

          await(submissionRequestRepo.insert(testSubscriptionRequest))
          val res = await(post(s"/subscription-request/migrated/vat-number/$testVatNumber/submit")(Json.obj()))

          res should have(
            httpStatus(NO_CONTENT),
            emptyBody
          )
        }
      }
      "the user is signing up a non UK with UK establishment with SF prefix and has provided a CT reference" should {
        s"return $NO_CONTENT for $OverseasWithUkEstablishment sign up" in {
          val testSubscriptionRequest = SubscriptionRequest(
            vatNumber = testVatNumber,
            businessEntity = Some(OverseasWithUkEstablishment(testNonUKCompanyWithUKEstablishmentCompanyNumberSF)),
            ctReference = Some(testCtReference),
            email = Some(testEmail),
            isMigratable = true,
            isDirectDebit = false,
            contactPreference = Some(testContactPreference)
          )

          stubAuth(OK, successfulAuthResponse(vatDecEnrolment()))
          stubGetEmailVerified(testEmail)
          stubRegisterBusinessEntity(testVatNumber, OverseasWithUkEstablishment(testNonUKCompanyWithUKEstablishmentCompanyNumberSF))(testSafeId)
          stubMigratedSignUp(
            testSafeId,
            testVatNumber
          )(OK)
          stubRegisterEnrolment(testVatNumber, testSafeId)(NO_CONTENT)

          await(submissionRequestRepo.insert(testSubscriptionRequest))
          val res = await(post(s"/subscription-request/migrated/vat-number/$testVatNumber/submit")(Json.obj()))

          res should have(
            httpStatus(NO_CONTENT),
            emptyBody
          )
        }
      }
      "the user is signing up a non UK with UK establishment with NF prefix and has provided a CT reference" should {
        s"return $NO_CONTENT for $OverseasWithUkEstablishment sign up" in {
          val testSubscriptionRequest = SubscriptionRequest(
            vatNumber = testVatNumber,
            businessEntity = Some(OverseasWithUkEstablishment(testNonUKCompanyWithUKEstablishmentCompanyNumberNF)),
            ctReference = Some(testCtReference),
            email = Some(testEmail),
            isMigratable = true,
            isDirectDebit = false,
            contactPreference = Some(testContactPreference)
          )

          stubAuth(OK, successfulAuthResponse(vatDecEnrolment()))
          stubGetEmailVerified(testEmail)
          stubRegisterBusinessEntity(testVatNumber, OverseasWithUkEstablishment(testNonUKCompanyWithUKEstablishmentCompanyNumberNF))(testSafeId)
          stubMigratedSignUp(
            testSafeId,
            testVatNumber
          )(OK)
          stubRegisterEnrolment(testVatNumber, testSafeId)(NO_CONTENT)

          await(submissionRequestRepo.insert(testSubscriptionRequest))
          val res = await(post(s"/subscription-request/migrated/vat-number/$testVatNumber/submit")(Json.obj()))

          res should have(
            httpStatus(NO_CONTENT),
            emptyBody
          )
        }
      }
      "the user is signing up a non UK with UK establishment with NF prefix and has not provided a CT reference" should {
        s"return $NO_CONTENT for $OverseasWithUkEstablishment sign up" in {
          val testSubscriptionRequest = SubscriptionRequest(
            vatNumber = testVatNumber,
            businessEntity = Some(OverseasWithUkEstablishment(testNonUKCompanyWithUKEstablishmentCompanyNumberNF)),
            ctReference = None,
            email = Some(testEmail),
            isMigratable = true,
            isDirectDebit = false,
            contactPreference = Some(testContactPreference)
          )

          stubAuth(OK, successfulAuthResponse(vatDecEnrolment()))
          stubGetEmailVerified(testEmail)
          stubRegisterBusinessEntity(testVatNumber, OverseasWithUkEstablishment(testNonUKCompanyWithUKEstablishmentCompanyNumberNF))(testSafeId)
          stubMigratedSignUp(
            testSafeId,
            testVatNumber
          )(OK)
          stubRegisterEnrolment(testVatNumber, testSafeId)(NO_CONTENT)

          await(submissionRequestRepo.insert(testSubscriptionRequest))
          val res = await(post(s"/subscription-request/migrated/vat-number/$testVatNumber/submit")(Json.obj()))

          res should have(
            httpStatus(NO_CONTENT),
            emptyBody
          )
        }
      }
      "EtmpEntityType is enabled" should {
        s"return $NO_CONTENT for $GeneralPartnership sign up" in {
          val testSubscriptionRequest = SubscriptionRequest(
            vatNumber = testVatNumber,
            businessEntity = Some(GeneralPartnership(Some(testUtr))),
            email = Some(testEmail),
            isMigratable = true,
            isDirectDebit = false,
            contactPreference = Some(testContactPreference)
          )

          stubAuth(OK, successfulAuthResponse(vatDecEnrolment()))
          stubGetEmailVerified(testEmail)
          stubRegisterBusinessEntity(testVatNumber, GeneralPartnership(Some(testUtr)))(testSafeId)
          stubMigratedSignUp(
            testSafeId,
            testVatNumber
          )(OK)
          stubRegisterEnrolment(testVatNumber, testSafeId)(NO_CONTENT)

          await(submissionRequestRepo.insert(testSubscriptionRequest))
          val res = await(post(s"/subscription-request/migrated/vat-number/$testVatNumber/submit")(Json.obj()))

          res should have(
            httpStatus(NO_CONTENT),
            emptyBody
          )
        }
        s"return $NO_CONTENT for $LimitedPartnership sign up" in {
          val testSubscriptionRequest = SubscriptionRequest(
            vatNumber = testVatNumber,
            businessEntity = Some(LimitedPartnership(Some(testUtr), testCompanyNumber)),
            email = Some(testEmail),
            isMigratable = true,
            isDirectDebit = false,
            contactPreference = Some(testContactPreference)
          )

          stubAuth(OK, successfulAuthResponse(vatDecEnrolment()))
          stubGetEmailVerified(testEmail)
          stubRegisterBusinessEntity(testVatNumber, LimitedPartnership(Some(testUtr), testCompanyNumber))(testSafeId)
          stubMigratedSignUp(
            testSafeId,
            testVatNumber
          )(OK)
          stubRegisterEnrolment(testVatNumber, testSafeId)(NO_CONTENT)

          await(submissionRequestRepo.insert(testSubscriptionRequest))
          val res = await(post(s"/subscription-request/migrated/vat-number/$testVatNumber/submit")(Json.obj()))

          res should have(
            httpStatus(NO_CONTENT),
            emptyBody
          )
        }
        s"return $NO_CONTENT for $LimitedLiabilityPartnership sign up" in {
          val testSubscriptionRequest = SubscriptionRequest(
            vatNumber = testVatNumber,
            businessEntity = Some(LimitedLiabilityPartnership(Some(testUtr), testCompanyNumber)),
            email = Some(testEmail),
            isMigratable = true,
            isDirectDebit = false,
            contactPreference = Some(testContactPreference)
          )

          stubAuth(OK, successfulAuthResponse(vatDecEnrolment()))
          stubGetEmailVerified(testEmail)
          stubRegisterBusinessEntity(testVatNumber, LimitedLiabilityPartnership(Some(testUtr), testCompanyNumber))(testSafeId)
          stubMigratedSignUp(
            testSafeId,
            testVatNumber
          )(OK)
          stubRegisterEnrolment(testVatNumber, testSafeId)(NO_CONTENT)

          await(submissionRequestRepo.insert(testSubscriptionRequest))
          val res = await(post(s"/subscription-request/migrated/vat-number/$testVatNumber/submit")(Json.obj()))

          res should have(
            httpStatus(NO_CONTENT),
            emptyBody
          )
        }
        s"return $NO_CONTENT for $ScottishLimitedPartnership sign up" in {
          val testSubscriptionRequest = SubscriptionRequest(
            vatNumber = testVatNumber,
            businessEntity = Some(ScottishLimitedPartnership(Some(testUtr), testCompanyNumber)),
            email = Some(testEmail),
            isMigratable = true,
            isDirectDebit = false,
            contactPreference = Some(testContactPreference)
          )

          stubAuth(OK, successfulAuthResponse(vatDecEnrolment()))
          stubGetEmailVerified(testEmail)
          stubRegisterBusinessEntity(testVatNumber, ScottishLimitedPartnership(Some(testUtr), testCompanyNumber))(testSafeId)
          stubMigratedSignUp(
            testSafeId,
            testVatNumber
          )(OK)
          stubRegisterEnrolment(testVatNumber, testSafeId)(NO_CONTENT)

          await(submissionRequestRepo.insert(testSubscriptionRequest))
          val res = await(post(s"/subscription-request/migrated/vat-number/$testVatNumber/submit")(Json.obj()))

          res should have(
            httpStatus(NO_CONTENT),
            emptyBody
          )
        }
        s"return $NO_CONTENT for $VatGroup sign up" in {

          val testSubscriptionRequest = SubscriptionRequest(
            vatNumber = testVatNumber,
            businessEntity = Some(VatGroup),
            email = Some(testEmail),
            identityVerified = true,
            isMigratable = true,
            isDirectDebit = false,
            contactPreference = Some(testContactPreference)
          )

          stubAuth(OK, successfulAuthResponse(vatDecEnrolment()))
          stubGetEmailVerified(testEmail)
          stubRegisterBusinessEntity(testVatNumber, VatGroup)(testSafeId)
          stubMigratedSignUp(
            testSafeId,
            testVatNumber
          )(OK)
          stubRegisterEnrolment(testVatNumber, testSafeId)(NO_CONTENT)

          await(submissionRequestRepo.insert(testSubscriptionRequest))
          val res = await(post(s"/subscription-request/migrated/vat-number/$testVatNumber/submit")(Json.obj()))

          res should have(
            httpStatus(NO_CONTENT),
            emptyBody
          )
        }
        s"return $NO_CONTENT for $Trust sign up" in {

          val testSubscriptionRequest = SubscriptionRequest(
            vatNumber = testVatNumber,
            businessEntity = Some(Trust),
            email = Some(testEmail),
            identityVerified = true,
            isMigratable = true,
            isDirectDebit = false,
            contactPreference = Some(testContactPreference)
          )

          stubAuth(OK, successfulAuthResponse(vatDecEnrolment()))
          stubGetEmailVerified(testEmail)
          stubRegisterBusinessEntity(testVatNumber, Trust)(testSafeId)
          stubMigratedSignUp(
            testSafeId,
            testVatNumber
          )(OK)
          stubRegisterEnrolment(testVatNumber, testSafeId)(NO_CONTENT)

          await(submissionRequestRepo.insert(testSubscriptionRequest))
          val res = await(post(s"/subscription-request/migrated/vat-number/$testVatNumber/submit")(Json.obj()))

          res should have(
            httpStatus(NO_CONTENT),
            emptyBody
          )
        }
        s"return $NO_CONTENT for $UnincorporatedAssociation sign up" in {

          val testSubscriptionRequest = SubscriptionRequest(
            vatNumber = testVatNumber,
            businessEntity = Some(UnincorporatedAssociation),
            email = Some(testEmail),
            isMigratable = true,
            isDirectDebit = false,
            contactPreference = Some(testContactPreference)
          )

          stubAuth(OK, successfulAuthResponse(vatDecEnrolment()))
          stubGetEmailVerified(testEmail)
          stubRegisterBusinessEntity(testVatNumber, UnincorporatedAssociation)(testSafeId)
          stubMigratedSignUp(
            testSafeId,
            testVatNumber
          )(OK)
          stubRegisterEnrolment(testVatNumber, testSafeId)(NO_CONTENT)

          await(submissionRequestRepo.insert(testSubscriptionRequest))
          val res = await(post(s"/subscription-request/migrated/vat-number/$testVatNumber/submit")(Json.obj()))

          res should have(
            httpStatus(NO_CONTENT),
            emptyBody
          )
        }
        s"return $NO_CONTENT for $JointVenture sign up" in {

          val testSubscriptionRequest = SubscriptionRequest(
            vatNumber = testVatNumber,
            businessEntity = Some(JointVenture),
            email = Some(testEmail),
            identityVerified = true,
            isMigratable = true,
            isDirectDebit = false,
            contactPreference = Some(testContactPreference)
          )

          stubAuth(OK, successfulAuthResponse(vatDecEnrolment()))
          stubGetEmailVerified(testEmail)
          stubRegisterBusinessEntity(testVatNumber, JointVenture)(testSafeId)
          stubMigratedSignUp(
            testSafeId,
            testVatNumber
          )(OK)
          stubRegisterEnrolment(testVatNumber, testSafeId)(NO_CONTENT)

          await(submissionRequestRepo.insert(testSubscriptionRequest))
          val res = await(post(s"/subscription-request/migrated/vat-number/$testVatNumber/submit")(Json.obj()))

          res should have(
            httpStatus(NO_CONTENT),
            emptyBody
          )
        }

      }
    }
  }
}
