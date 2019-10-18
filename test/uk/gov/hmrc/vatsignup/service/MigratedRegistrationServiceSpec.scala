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

package uk.gov.hmrc.vatsignup.service

import play.api.test.FakeRequest
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.vatsignup.connectors.mocks.MockRegistrationConnector
import uk.gov.hmrc.vatsignup.models._
import uk.gov.hmrc.vatsignup.service.mocks.monitoring.MockAuditService
import uk.gov.hmrc.vatsignup.services.MigratedRegistrationService
import uk.gov.hmrc.vatsignup.helpers.TestConstants._
import uk.gov.hmrc.vatsignup.httpparsers.RegisterWithMultipleIdentifiersHttpParser.{RegisterWithMultipleIdsErrorResponse, RegisterWithMultipleIdsSuccess}
import uk.gov.hmrc.vatsignup.models.monitoring.RegisterWithMultipleIDsAuditing.RegisterWithMultipleIDsAuditModel
import play.api.http.Status._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class MigratedRegistrationServiceSpec extends UnitSpec with MockRegistrationConnector with MockAuditService {

  object TestMigratedRegistrationService extends MigratedRegistrationService(
    mockEntityTypeRegistrationConnector,
    mockAuditService
  )

  implicit val hc = HeaderCarrier()
  implicit val req = FakeRequest()

  "registerBusinessEntity" when {
    "the registration is successful" should {
      "return a safe ID" when {
        "the business type is SoleTrader" in {
          mockRegisterBusinessEntity(testVatNumber, SoleTrader(testNino))(
            Future.successful(Right(RegisterWithMultipleIdsSuccess(testSafeId)))
          )

          val res = await(TestMigratedRegistrationService.registerBusinessEntity(
            vatNumber = testVatNumber,
            businessEntity = SoleTrader(testNino),
            optArn = None
          )(hc, req))

          verifyAudit(RegisterWithMultipleIDsAuditModel(
            vatNumber = testVatNumber,
            businessEntity = SoleTrader(testNino),
            agentReferenceNumber = None,
            isSuccess = true
          ))

          res shouldBe testSafeId
        }
        "the business type is LimitedCompany" in {
          mockRegisterBusinessEntity(testVatNumber, LimitedCompany(testCompanyNumber))(
            Future.successful(Right(RegisterWithMultipleIdsSuccess(testSafeId)))
          )

          val res = await(TestMigratedRegistrationService.registerBusinessEntity(
            vatNumber = testVatNumber,
            businessEntity = LimitedCompany(testCompanyNumber),
            optArn = None
          )(hc, req))

          verifyAudit(RegisterWithMultipleIDsAuditModel(
            vatNumber = testVatNumber,
            businessEntity = LimitedCompany(testCompanyNumber),
            agentReferenceNumber = None,
            isSuccess = true
          ))

          res shouldBe testSafeId
        }
        "the business type is GeneralPartnership" in {
          mockRegisterBusinessEntity(testVatNumber, GeneralPartnership(Some(testUtr)))(
            Future.successful(Right(RegisterWithMultipleIdsSuccess(testSafeId)))
          )

          val res = await(TestMigratedRegistrationService.registerBusinessEntity(
            vatNumber = testVatNumber,
            businessEntity = GeneralPartnership(Some(testUtr)),
            optArn = None
          )(hc, req))

          verifyAudit(RegisterWithMultipleIDsAuditModel(
            vatNumber = testVatNumber,
            businessEntity = GeneralPartnership(Some(testUtr)),
            agentReferenceNumber = None,
            isSuccess = true
          ))

          res shouldBe testSafeId
        }
        "the business type is LimitedPartnership" in {
          mockRegisterBusinessEntity(testVatNumber, LimitedPartnership(Some(testUtr), testCompanyNumber))(
            Future.successful(Right(RegisterWithMultipleIdsSuccess(testSafeId)))
          )

          val res = await(TestMigratedRegistrationService.registerBusinessEntity(
            vatNumber = testVatNumber,
            businessEntity = LimitedPartnership(Some(testUtr), testCompanyNumber),
            optArn = None
          )(hc, req))

          verifyAudit(RegisterWithMultipleIDsAuditModel(
            vatNumber = testVatNumber,
            businessEntity = LimitedPartnership(Some(testUtr), testCompanyNumber),
            agentReferenceNumber = None,
            isSuccess = true
          ))

          res shouldBe testSafeId
        }
        "the business type is LimitedLiabilityPartnership" in {
          mockRegisterBusinessEntity(testVatNumber, LimitedLiabilityPartnership(Some(testUtr), testCompanyNumber))(
            Future.successful(Right(RegisterWithMultipleIdsSuccess(testSafeId)))
          )

          val res = await(TestMigratedRegistrationService.registerBusinessEntity(
            vatNumber = testVatNumber,
            businessEntity = LimitedLiabilityPartnership(Some(testUtr), testCompanyNumber),
            optArn = None
          )(hc, req))

          verifyAudit(RegisterWithMultipleIDsAuditModel(
            vatNumber = testVatNumber,
            businessEntity = LimitedLiabilityPartnership(Some(testUtr), testCompanyNumber),
            agentReferenceNumber = None,
            isSuccess = true
          ))

          res shouldBe testSafeId
        }
        "the business type is ScottishLimitedPartnership" in {
          mockRegisterBusinessEntity(testVatNumber, ScottishLimitedPartnership(Some(testUtr), testCompanyNumber))(
            Future.successful(Right(RegisterWithMultipleIdsSuccess(testSafeId)))
          )

          val res = await(TestMigratedRegistrationService.registerBusinessEntity(
            vatNumber = testVatNumber,
            businessEntity = ScottishLimitedPartnership(Some(testUtr), testCompanyNumber),
            optArn = None
          )(hc, req))

          verifyAudit(RegisterWithMultipleIDsAuditModel(
            vatNumber = testVatNumber,
            businessEntity = ScottishLimitedPartnership(Some(testUtr), testCompanyNumber),
            agentReferenceNumber = None,
            isSuccess = true
          ))

          res shouldBe testSafeId
        }
        "the business type is Trust" in {
          mockRegisterBusinessEntity(testVatNumber, Trust)(
            Future.successful(Right(RegisterWithMultipleIdsSuccess(testSafeId)))
          )

          val res = await(TestMigratedRegistrationService.registerBusinessEntity(
            vatNumber = testVatNumber,
            businessEntity = Trust,
            optArn = None
          )(hc, req))

          verifyAudit(RegisterWithMultipleIDsAuditModel(
            vatNumber = testVatNumber,
            businessEntity = Trust,
            agentReferenceNumber = None,
            isSuccess = true
          ))

          res shouldBe testSafeId
        }
        "the business type is Charity" in {
          mockRegisterBusinessEntity(testVatNumber, Charity)(
            Future.successful(Right(RegisterWithMultipleIdsSuccess(testSafeId)))
          )

          val res = await(TestMigratedRegistrationService.registerBusinessEntity(
            vatNumber = testVatNumber,
            businessEntity = Charity,
            optArn = None
          )(hc, req))

          verifyAudit(RegisterWithMultipleIDsAuditModel(
            vatNumber = testVatNumber,
            businessEntity = Charity,
            agentReferenceNumber = None,
            isSuccess = true
          ))

          res shouldBe testSafeId
        }
        "the business type is AdministrativeDivision" in {
          mockRegisterBusinessEntity(testVatNumber, AdministrativeDivision)(
            Future.successful(Right(RegisterWithMultipleIdsSuccess(testSafeId)))
          )

          val res = await(TestMigratedRegistrationService.registerBusinessEntity(
            vatNumber = testVatNumber,
            businessEntity = AdministrativeDivision,
            optArn = None
          )(hc, req))

          verifyAudit(RegisterWithMultipleIDsAuditModel(
            vatNumber = testVatNumber,
            businessEntity = AdministrativeDivision,
            agentReferenceNumber = None,
            isSuccess = true
          ))

          res shouldBe testSafeId
        }
        "the business type is VatGroup" in {
          mockRegisterBusinessEntity(testVatNumber, VatGroup)(
            Future.successful(Right(RegisterWithMultipleIdsSuccess(testSafeId)))
          )

          val res = await(TestMigratedRegistrationService.registerBusinessEntity(
            vatNumber = testVatNumber,
            businessEntity = VatGroup,
            optArn = None
          )(hc, req))

          verifyAudit(RegisterWithMultipleIDsAuditModel(
            vatNumber = testVatNumber,
            businessEntity = VatGroup,
            agentReferenceNumber = None,
            isSuccess = true
          ))

          res shouldBe testSafeId
        }
        "the business type is UnincorporatedAssociation" in {
          mockRegisterBusinessEntity(testVatNumber, UnincorporatedAssociation)(
            Future.successful(Right(RegisterWithMultipleIdsSuccess(testSafeId)))
          )

          val res = await(TestMigratedRegistrationService.registerBusinessEntity(
            vatNumber = testVatNumber,
            businessEntity = UnincorporatedAssociation,
            optArn = None
          )(hc, req))

          verifyAudit(RegisterWithMultipleIDsAuditModel(
            vatNumber = testVatNumber,
            businessEntity = UnincorporatedAssociation,
            agentReferenceNumber = None,
            isSuccess = true
          ))

          res shouldBe testSafeId
        }
        "the business type is JointVenture" in {
          mockRegisterBusinessEntity(testVatNumber, JointVenture)(
            Future.successful(Right(RegisterWithMultipleIdsSuccess(testSafeId)))
          )

          val res = await(TestMigratedRegistrationService.registerBusinessEntity(
            vatNumber = testVatNumber,
            businessEntity = JointVenture,
            optArn = None
          )(hc, req))

          verifyAudit(RegisterWithMultipleIDsAuditModel(
            vatNumber = testVatNumber,
            businessEntity = JointVenture,
            agentReferenceNumber = None,
            isSuccess = true
          ))

          res shouldBe testSafeId
        }
        "the business type is Overseas" in {
          mockRegisterBusinessEntity(testVatNumber, Overseas)(
            Future.successful(Right(RegisterWithMultipleIdsSuccess(testSafeId)))
          )

          val res = await(TestMigratedRegistrationService.registerBusinessEntity(
            vatNumber = testVatNumber,
            businessEntity = Overseas,
            optArn = None
          )(hc, req))

          verifyAudit(RegisterWithMultipleIDsAuditModel(
            vatNumber = testVatNumber,
            businessEntity = Overseas,
            agentReferenceNumber = None,
            isSuccess = true
          ))

          res shouldBe testSafeId
        }
      }
    }
    "the registration fails" should {
      "return RegisterWithMultipleIdsFailure" when {
        "the business type is SoleTrader" in {
          mockRegisterBusinessEntity(testVatNumber, SoleTrader(testNino))(
            Future.successful(Left(RegisterWithMultipleIdsErrorResponse(BAD_REQUEST, "")))
          )

          intercept[InternalServerException] {
            await(TestMigratedRegistrationService.registerBusinessEntity(
              vatNumber = testVatNumber,
              businessEntity = SoleTrader(testNino),
              optArn = None
            )(hc, req))
          }

          verifyAudit(RegisterWithMultipleIDsAuditModel(
            vatNumber = testVatNumber,
            businessEntity = SoleTrader(testNino),
            agentReferenceNumber = None,
            isSuccess = false
          ))
        }
        "the business type is LimitedCompany" in {
          mockRegisterBusinessEntity(testVatNumber, LimitedCompany(testCompanyNumber))(
            Future.successful(Left(RegisterWithMultipleIdsErrorResponse(BAD_REQUEST, "")))
          )

          intercept[InternalServerException] {
            await(TestMigratedRegistrationService.registerBusinessEntity(
              vatNumber = testVatNumber,
              businessEntity = LimitedCompany(testCompanyNumber),
              optArn = None
            )(hc, req))
          }

          verifyAudit(RegisterWithMultipleIDsAuditModel(
            vatNumber = testVatNumber,
            businessEntity = LimitedCompany(testCompanyNumber),
            agentReferenceNumber = None,
            isSuccess = false
          ))
        }
        "the business type is GeneralPartnership" in {
          mockRegisterBusinessEntity(testVatNumber, GeneralPartnership(Some(testUtr)))(
            Future.successful(Left(RegisterWithMultipleIdsErrorResponse(BAD_REQUEST, "")))
          )

          intercept[InternalServerException] {
            await(TestMigratedRegistrationService.registerBusinessEntity(
              vatNumber = testVatNumber,
              businessEntity = GeneralPartnership(Some(testUtr)),
              optArn = None
            )(hc, req))
          }

          verifyAudit(RegisterWithMultipleIDsAuditModel(
            vatNumber = testVatNumber,
            businessEntity = GeneralPartnership(Some(testUtr)),
            agentReferenceNumber = None,
            isSuccess = false
          ))
        }
        "the business type is LimitedPartnership" in {
          mockRegisterBusinessEntity(testVatNumber, LimitedPartnership(Some(testUtr), testCompanyNumber))(
            Future.successful(Left(RegisterWithMultipleIdsErrorResponse(BAD_REQUEST, "")))
          )

          intercept[InternalServerException] {
            await(TestMigratedRegistrationService.registerBusinessEntity(
              vatNumber = testVatNumber,
              businessEntity = LimitedPartnership(Some(testUtr), testCompanyNumber),
              optArn = None
            )(hc, req))
          }

          verifyAudit(RegisterWithMultipleIDsAuditModel(
            vatNumber = testVatNumber,
            businessEntity = LimitedPartnership(Some(testUtr), testCompanyNumber),
            agentReferenceNumber = None,
            isSuccess = false
          ))
        }
        "the business type is LimitedLiabilityPartnership" in {
          mockRegisterBusinessEntity(testVatNumber, LimitedLiabilityPartnership(Some(testUtr), testCompanyNumber))(
            Future.successful(Left(RegisterWithMultipleIdsErrorResponse(BAD_REQUEST, "")))
          )

          intercept[InternalServerException] {
            await(TestMigratedRegistrationService.registerBusinessEntity(
              vatNumber = testVatNumber,
              businessEntity = LimitedLiabilityPartnership(Some(testUtr), testCompanyNumber),
              optArn = None
            )(hc, req))
          }

          verifyAudit(RegisterWithMultipleIDsAuditModel(
            vatNumber = testVatNumber,
            businessEntity = LimitedLiabilityPartnership(Some(testUtr), testCompanyNumber),
            agentReferenceNumber = None,
            isSuccess = false
          ))
        }
        "the business type is ScottishLimitedPartnership" in {
          mockRegisterBusinessEntity(testVatNumber, ScottishLimitedPartnership(Some(testUtr), testCompanyNumber))(
            Future.successful(Left(RegisterWithMultipleIdsErrorResponse(BAD_REQUEST, "")))
          )

          intercept[InternalServerException] {
            await(TestMigratedRegistrationService.registerBusinessEntity(
              vatNumber = testVatNumber,
              businessEntity = ScottishLimitedPartnership(Some(testUtr), testCompanyNumber),
              optArn = None
            )(hc, req))
          }

          verifyAudit(RegisterWithMultipleIDsAuditModel(
            vatNumber = testVatNumber,
            businessEntity = ScottishLimitedPartnership(Some(testUtr), testCompanyNumber),
            agentReferenceNumber = None,
            isSuccess = false
          ))
        }
        "the business type is Trust" in {
          mockRegisterBusinessEntity(testVatNumber, Trust)(
            Future.successful(Left(RegisterWithMultipleIdsErrorResponse(BAD_REQUEST, "")))
          )

          intercept[InternalServerException] {
            await(TestMigratedRegistrationService.registerBusinessEntity(
              vatNumber = testVatNumber,
              businessEntity = Trust,
              optArn = None
            )(hc, req))
          }

          verifyAudit(RegisterWithMultipleIDsAuditModel(
            vatNumber = testVatNumber,
            businessEntity = Trust,
            agentReferenceNumber = None,
            isSuccess = false
          ))
        }
        "the business type is Charity" in {
          mockRegisterBusinessEntity(testVatNumber, Charity)(
            Future.successful(Left(RegisterWithMultipleIdsErrorResponse(BAD_REQUEST, "")))
          )

          intercept[InternalServerException] {
            await(TestMigratedRegistrationService.registerBusinessEntity(
              vatNumber = testVatNumber,
              businessEntity = Charity,
              optArn = None
            )(hc, req))
          }

          verifyAudit(RegisterWithMultipleIDsAuditModel(
            vatNumber = testVatNumber,
            businessEntity = Charity,
            agentReferenceNumber = None,
            isSuccess = false
          ))
        }
        "the business type is AdministrativeDivision" in {
          mockRegisterBusinessEntity(testVatNumber, AdministrativeDivision)(
            Future.successful(Left(RegisterWithMultipleIdsErrorResponse(BAD_REQUEST, "")))
          )

          intercept[InternalServerException] {
            await(TestMigratedRegistrationService.registerBusinessEntity(
              vatNumber = testVatNumber,
              businessEntity = AdministrativeDivision,
              optArn = None
            )(hc, req))
          }

          verifyAudit(RegisterWithMultipleIDsAuditModel(
            vatNumber = testVatNumber,
            businessEntity = AdministrativeDivision,
            agentReferenceNumber = None,
            isSuccess = false
          ))
        }
        "the business type is VatGroup" in {
          mockRegisterBusinessEntity(testVatNumber, VatGroup)(
            Future.successful(Left(RegisterWithMultipleIdsErrorResponse(BAD_REQUEST, "")))
          )

          intercept[InternalServerException] {
            await(TestMigratedRegistrationService.registerBusinessEntity(
              vatNumber = testVatNumber,
              businessEntity = VatGroup,
              optArn = None
            )(hc, req))
          }

          verifyAudit(RegisterWithMultipleIDsAuditModel(
            vatNumber = testVatNumber,
            businessEntity = VatGroup,
            agentReferenceNumber = None,
            isSuccess = false
          ))
        }
        "the business type is UnincorporatedAssociation" in {
          mockRegisterBusinessEntity(testVatNumber, UnincorporatedAssociation)(
            Future.successful(Left(RegisterWithMultipleIdsErrorResponse(BAD_REQUEST, "")))
          )

          intercept[InternalServerException] {
            await(TestMigratedRegistrationService.registerBusinessEntity(
                vatNumber = testVatNumber,
                businessEntity = UnincorporatedAssociation,
                optArn = None
            )(hc, req))
          }

          verifyAudit(RegisterWithMultipleIDsAuditModel(
            vatNumber = testVatNumber,
            businessEntity = UnincorporatedAssociation,
            agentReferenceNumber = None,
            isSuccess = false
          ))
        }
        "the business type is JointVenture" in {
          mockRegisterBusinessEntity(testVatNumber, JointVenture)(
            Future.successful(Left(RegisterWithMultipleIdsErrorResponse(BAD_REQUEST, "")))
          )

          intercept[InternalServerException] {
            await(TestMigratedRegistrationService.registerBusinessEntity(
            vatNumber = testVatNumber,
            businessEntity = JointVenture,
            optArn = None
            )(hc, req))
          }

          verifyAudit(RegisterWithMultipleIDsAuditModel(
            vatNumber = testVatNumber,
            businessEntity = JointVenture,
            agentReferenceNumber = None,
            isSuccess = false
          ))
        }
        "the business type is Overseas" in {
          mockRegisterBusinessEntity(testVatNumber, Overseas)(
            Future.successful(Left(RegisterWithMultipleIdsErrorResponse(BAD_REQUEST, "")))
          )

          intercept[InternalServerException] {
            await(TestMigratedRegistrationService.registerBusinessEntity(
                vatNumber = testVatNumber,
                businessEntity = Overseas,
                optArn = None
            )(hc, req))
          }

          verifyAudit(RegisterWithMultipleIDsAuditModel(
            vatNumber = testVatNumber,
            businessEntity = Overseas,
            agentReferenceNumber = None,
            isSuccess = false
          ))
        }
      }
    }
  }

}
