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

import play.api.mvc.Request
import play.api.test.FakeRequest
import reactivemongo.api.commands.UpdateWriteResult
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.vatsignup.helpers.TestConstants._
import uk.gov.hmrc.vatsignup.models.LimitedCompany
import uk.gov.hmrc.vatsignup.repositories.mocks.MockSubscriptionRequestRepository
import uk.gov.hmrc.vatsignup.service.mocks.MockCompanyMatchService
import uk.gov.hmrc.vatsignup.services.CompanyMatchService.CompanyVerified
import uk.gov.hmrc.vatsignup.services.StoreCompanyNumberService._
import uk.gov.hmrc.vatsignup.services.{CompanyMatchService, StoreCompanyNumberService}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class StoreCompanyNumberServiceSpec extends UnitSpec
  with MockSubscriptionRequestRepository with MockCompanyMatchService {

  object TestStoreCompanyNumberService extends StoreCompanyNumberService(
    mockSubscriptionRequestRepository,
    mockCompanyMatchService
  )

  implicit val hc: HeaderCarrier = HeaderCarrier()
  implicit val request: Request[_] = FakeRequest()

  "storeCompanyNumber" when {
    "there is no CT reference provided" when {
      "the database stores the company number successfully" should {
        "return StoreCompanyNumberSuccess" in {
          mockUpsertBusinessEntity(testVatNumber, LimitedCompany(testCompanyNumber))(Future.successful(mock[UpdateWriteResult]))

          val res = TestStoreCompanyNumberService.storeCompanyNumber(testVatNumber, testCompanyNumber)

          await(res) shouldBe Right(StoreCompanyNumberSuccess)
        }
      }

      "the database stores a non UK Company without UK Establishment" should {
        "return StoreCompanyNumberSuccess" in {
          mockUpsertBusinessEntity(testVatNumber, LimitedCompany(testNonUkNoEstablishmentCompanyNumber))(
            Future.successful(mock[UpdateWriteResult])
          )
          val res = TestStoreCompanyNumberService.storeCompanyNumber(testVatNumber, testNonUkNoEstablishmentCompanyNumber)

          await(res) shouldBe Right(StoreCompanyNumberSuccess)
        }
      }

      "the database returns a NoSuchElementException" should {
        "return CompanyNumberDatabaseFailureNoVATNumber" in {
          mockUpsertBusinessEntity(testVatNumber, LimitedCompany(testCompanyNumber))(Future.failed(new NoSuchElementException))

          val res = TestStoreCompanyNumberService.storeCompanyNumber(testVatNumber, testCompanyNumber)

          await(res) shouldBe Left(DatabaseFailureNoVATNumber)
        }
      }

      "the database returns any other failure" should {
        "return CompanyNumberDatabaseFailure" in {
          mockUpsertBusinessEntity(testVatNumber, LimitedCompany(testCompanyNumber))(Future.failed(new Exception))

          val res = TestStoreCompanyNumberService.storeCompanyNumber(testVatNumber, testCompanyNumber)

          await(res) shouldBe Left(CompanyNumberDatabaseFailure)
        }
      }
    }
    "there is a CT reference provided" when {
      "the company number matches the CT reference" when {
        "the database stores the company number successfully" when {
          "the database stores the CT reference successfully" should {
            "return StoreCompanyNumberSuccess" in {
              mockCheckCompanyMatch(testCompanyNumber, testCtReference)(Future.successful(Right(CompanyVerified)))
              mockUpsertBusinessEntity(testVatNumber, LimitedCompany(testCompanyNumber))(Future.successful(mock[UpdateWriteResult]))
              mockUpsertCtReference(testVatNumber, testCtReference)(Future.successful(mock[UpdateWriteResult]))

              val res = TestStoreCompanyNumberService.storeCompanyNumber(testVatNumber, testCompanyNumber, testCtReference)

              await(res) shouldBe Right(StoreCompanyNumberSuccess)
            }
          }
          "the database returns a NoSuchElementException" should {
            "return DatabaseFailureNoVATNumber" in {
              mockCheckCompanyMatch(testCompanyNumber, testCtReference)(Future.successful(Right(CompanyVerified)))
              mockUpsertBusinessEntity(testVatNumber, LimitedCompany(testCompanyNumber))(Future.successful(mock[UpdateWriteResult]))
              mockUpsertCtReference(testVatNumber, testCtReference)(Future.failed(new NoSuchElementException))

              val res = TestStoreCompanyNumberService.storeCompanyNumber(testVatNumber, testCompanyNumber, testCtReference)

              await(res) shouldBe Left(DatabaseFailureNoVATNumber)
            }
          }

          "the database returns any other failure" should {
            "return CompanyNumberDatabaseFailure" in {
              mockCheckCompanyMatch(testCompanyNumber, testCtReference)(Future.successful(Right(CompanyVerified)))
              mockUpsertBusinessEntity(testVatNumber, LimitedCompany(testCompanyNumber))(Future.successful(mock[UpdateWriteResult]))
              mockUpsertCtReference(testVatNumber, testCtReference)(Future.failed(new Exception))

              val res = TestStoreCompanyNumberService.storeCompanyNumber(testVatNumber, testCompanyNumber, testCtReference)

              await(res) shouldBe Left(CtReferenceDatabaseFailure)
            }
          }
        }

        "the database returns a NoSuchElementException" should {
          "return DatabaseFailureNoVATNumber" in {
            mockCheckCompanyMatch(testCompanyNumber, testCtReference)(Future.successful(Right(CompanyVerified)))
            mockUpsertBusinessEntity(testVatNumber, LimitedCompany(testCompanyNumber))(Future.failed(new NoSuchElementException))

            val res = TestStoreCompanyNumberService.storeCompanyNumber(testVatNumber, testCompanyNumber, testCtReference)

            await(res) shouldBe Left(DatabaseFailureNoVATNumber)
          }
        }

        "the database returns any other failure" should {
          "return CompanyNumberDatabaseFailure" in {
            mockCheckCompanyMatch(testCompanyNumber, testCtReference)(Future.successful(Right(CompanyVerified)))
            mockUpsertBusinessEntity(testVatNumber, LimitedCompany(testCompanyNumber))(Future.failed(new Exception))

            val res = TestStoreCompanyNumberService.storeCompanyNumber(testVatNumber, testCompanyNumber, testCtReference)

            await(res) shouldBe Left(CompanyNumberDatabaseFailure)
          }
        }
      }
      "the company number does not match the CT reference" should {
        "return CtReferenceMismatch" in {
          mockCheckCompanyMatch(testCompanyNumber, testCtReference)(Future.successful(Left(CompanyMatchService.CtReferenceMismatch)))

          val res = TestStoreCompanyNumberService.storeCompanyNumber(testVatNumber, testCompanyNumber, testCtReference)

          await(res) shouldBe Left(StoreCompanyNumberService.CtReferenceMismatch)
        }
      }

      "the match company number call fails" should {
        "return MatchCtReferenceFailure" in {
          mockCheckCompanyMatch(testCompanyNumber, testCtReference)(Future.successful(Left(CompanyMatchService.GetCtReferenceFailure)))

          val res = TestStoreCompanyNumberService.storeCompanyNumber(testVatNumber, testCompanyNumber, testCtReference)

          await(res) shouldBe Left(StoreCompanyNumberService.MatchCtReferenceFailure)
        }
      }

    }
  }
}
