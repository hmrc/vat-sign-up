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

import org.scalatest.EitherValues
import play.api.test.Helpers._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.vatsignup.helpers.ComponentSpecBase
import uk.gov.hmrc.vatsignup.helpers.IntegrationTestConstants._
import uk.gov.hmrc.vatsignup.helpers.servicemocks.RegistrationStub._
import uk.gov.hmrc.vatsignup.httpparsers.RegisterWithMultipleIdentifiersHttpParser.RegisterWithMultipleIdsSuccess
import uk.gov.hmrc.vatsignup.models._

class RegistrationConnectorISpec extends ComponentSpecBase with EitherValues {
  private lazy val registrationConnector: RegistrationConnector = app.injector.instanceOf[RegistrationConnector]

  private implicit val headerCarrier: HeaderCarrier = HeaderCarrier()

  "registerBusinessEntity" when {
    "the business entity is a general partnership" when {
      "DES returns a successful response" should {
        "return a RegistrationSuccess with the SAFE ID" in {
          stubRegisterBusinessEntity(testVatNumber, GeneralPartnership(Some(testUtr)))(testSafeId)

          val res = await(registrationConnector.registerBusinessEntity(testVatNumber, GeneralPartnership(Some(testUtr))))

          res shouldBe Right(RegisterWithMultipleIdsSuccess(testSafeId))
        }
      }
    }

    "tbe business entity is a limited partnership" when {
      "DES returns a successful response" should {
        "return a RegistrationSuccess with the SAFE ID" in {
          stubRegisterBusinessEntity(testVatNumber, LimitedPartnership(Some(testUtr), testCompanyNumber))(testSafeId)

          val res = await(registrationConnector.registerBusinessEntity(testVatNumber, LimitedPartnership(Some(testUtr), testCompanyNumber)))

          res shouldBe Right(RegisterWithMultipleIdsSuccess(testSafeId))
        }
      }
    }

    "the business entity is a limited liability partnership" when {
      "DES returns a successful response" should {
        "return a RegistrationSuccess with the SAFE ID" in {
          stubRegisterBusinessEntity(testVatNumber, LimitedLiabilityPartnership(Some(testUtr), testCompanyNumber))(testSafeId)

          val res = await(registrationConnector.registerBusinessEntity(testVatNumber, LimitedLiabilityPartnership(Some(testUtr), testCompanyNumber)))

          res shouldBe Right(RegisterWithMultipleIdsSuccess(testSafeId))
        }
      }
    }

    "the business entity is a scottish limited partnership" when {
      "DES returns a successful response" should {
        "return a RegistrationSuccess with the SAFE ID" in {
          stubRegisterBusinessEntity(testVatNumber, ScottishLimitedPartnership(Some(testUtr), testCompanyNumber))(testSafeId)

          val res = await(registrationConnector.registerBusinessEntity(testVatNumber, ScottishLimitedPartnership(Some(testUtr), testCompanyNumber)))

          res shouldBe Right(RegisterWithMultipleIdsSuccess(testSafeId))
        }
      }
    }

    "the business entity is a limited company" when {
      "DES returns a successful response" should {
        "return a RegistrationSuccess with the SAFE ID" in {
          stubRegisterBusinessEntity(testVatNumber, LimitedCompany(testCompanyNumber))(testSafeId)

          val res = await(registrationConnector.registerBusinessEntity(testVatNumber, LimitedCompany(testCompanyNumber)))

          res shouldBe Right(RegisterWithMultipleIdsSuccess(testSafeId))
        }
      }
    }

    "the business entity is a non uk with uk establishment limited company with a FC prefix on CRN" when {
      "DES returns a successful response" should {
        "return a RegistrationSuccess with the SAFE ID" in {
          stubRegisterBusinessEntity(testVatNumber, OverseasWithUkEstablishment(testNonUKCompanyWithUKEstablishmentCompanyNumberFC))(testSafeId)

          val res = await(registrationConnector.registerBusinessEntity(
            vatNumber = testVatNumber,
            businessEntity = OverseasWithUkEstablishment(testNonUKCompanyWithUKEstablishmentCompanyNumberFC)
          ))

          res shouldBe Right(RegisterWithMultipleIdsSuccess(testSafeId))
        }
      }
    }

    "the business entity is a non uk with uk establishment limited company with a SF" when {
      "DES returns a successful response" should {
        "return a RegistrationSuccess with the SAFE ID" in {
          stubRegisterBusinessEntity(testVatNumber, OverseasWithUkEstablishment(testNonUKCompanyWithUKEstablishmentCompanyNumberSF))(testSafeId)

          val res = await(registrationConnector.registerBusinessEntity(
            vatNumber = testVatNumber,
            businessEntity = OverseasWithUkEstablishment(testNonUKCompanyWithUKEstablishmentCompanyNumberSF)
          ))

          res shouldBe Right(RegisterWithMultipleIdsSuccess(testSafeId))
        }
      }
    }

    "the business entity is a non uk with uk establishment limited company with a NF" when {
      "DES returns a successful response" should {
        "return a RegistrationSuccess with the SAFE ID" in {
          stubRegisterBusinessEntity(testVatNumber, OverseasWithUkEstablishment(testNonUKCompanyWithUKEstablishmentCompanyNumberNF))(testSafeId)

          val res = await(registrationConnector.registerBusinessEntity(
            vatNumber = testVatNumber,
            businessEntity = OverseasWithUkEstablishment(testNonUKCompanyWithUKEstablishmentCompanyNumberNF)
          ))

          res shouldBe Right(RegisterWithMultipleIdsSuccess(testSafeId))
        }
      }
    }

    "the business entity is a sole trader" when {
      "DES returns a successful response" should {
        "return a RegistrationSuccess with the SAFE ID" in {
          stubRegisterBusinessEntity(testVatNumber, SoleTrader(testNino))(testSafeId)

          val res = await(registrationConnector.registerBusinessEntity(testVatNumber, SoleTrader(testNino)))

          res shouldBe Right(RegisterWithMultipleIdsSuccess(testSafeId))
        }
      }
    }

    "the business entity is a VAT group" when {
      "DES returns a successful response" should {
        "return a RegistrationSuccess with the SAFE ID" in {
          stubRegisterBusinessEntity(testVatNumber, VatGroup)(testSafeId)

          val res = await(registrationConnector.registerBusinessEntity(testVatNumber, VatGroup))

          res shouldBe Right(RegisterWithMultipleIdsSuccess(testSafeId))
        }
      }
    }
    "the business entity is a Division" when {
      "DES returns a successful response" should {
        "return a RegistrationSuccess with the SAFE ID" in {
          stubRegisterBusinessEntity(testVatNumber, AdministrativeDivision)(testSafeId)

          val res = await(registrationConnector.registerBusinessEntity(testVatNumber, AdministrativeDivision))

          res shouldBe Right(RegisterWithMultipleIdsSuccess(testSafeId))
        }
      }
    }
    "the business entity is a Trust" when {
      "DES returns a successful response" should {
        "return a RegistrationSuccess with the SAFE ID" in {
          stubRegisterBusinessEntity(testVatNumber, Trust)(testSafeId)

          val res = await(registrationConnector.registerBusinessEntity(testVatNumber, Trust))

          res shouldBe Right(RegisterWithMultipleIdsSuccess(testSafeId))
        }
      }
    }
    "the business entity is a Charity" when {
      "DES returns a successful response" should {
        "return a RegistrationSuccess with the SAFE ID" in {
          stubRegisterBusinessEntity(testVatNumber, Charity)(testSafeId)

          val res = await(registrationConnector.registerBusinessEntity(testVatNumber, Charity))

          res shouldBe Right(RegisterWithMultipleIdsSuccess(testSafeId))
        }
      }
    }
    "the business entity is a Government Organisation" when {
      "DES returns a success" should {
        "return a RegistrationSuccess with the SAFEID" in {
          stubRegisterBusinessEntity(testVatNumber, GovernmentOrganisation)(testSafeId)

          val res = await(registrationConnector.registerBusinessEntity(testVatNumber, GovernmentOrganisation))

          res shouldBe Right(RegisterWithMultipleIdsSuccess(testSafeId))
        }
      }
    }

  }
}
