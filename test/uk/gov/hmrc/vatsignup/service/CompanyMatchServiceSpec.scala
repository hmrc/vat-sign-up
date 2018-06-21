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

package uk.gov.hmrc.vatsignup.service

import java.util.UUID

import play.api.http.Status._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.vatsignup.connectors.mocks.MockGetCtReferenceConnector
import uk.gov.hmrc.vatsignup.helpers.TestConstants._
import uk.gov.hmrc.vatsignup.httpparsers.GetCtReferenceHttpParser
import uk.gov.hmrc.vatsignup.services.CompanyMatchService
import uk.gov.hmrc.vatsignup.services.CompanyMatchService._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CompanyMatchServiceSpec extends UnitSpec
  with MockGetCtReferenceConnector {

  object TestCompanyMatchService extends CompanyMatchService(
    mockGetCtReferenceConnector
  )

  implicit val hc: HeaderCarrier = HeaderCarrier()

  "checkCompanyMatch" when {
    "DES returns a CT reference" when {
      "the CT reference matches the one provided by the user" should {
        "return CompanyVerified" in {
          mockGetCtReference(testCompanyNumber)(Future.successful(Right(testCtReference)))

          val res = TestCompanyMatchService.checkCompanyMatch(testCompanyNumber, testCtReference)

          await(res) shouldBe Right(CompanyVerified)
        }
      }
      "the CT reference does not match" should {
        "return CtReferenceMismatch" in {
          val nonMatchingCtReference = UUID.randomUUID().toString

          mockGetCtReference(testCompanyNumber)(Future.successful(Right(nonMatchingCtReference)))

          val res = TestCompanyMatchService.checkCompanyMatch(testCompanyNumber, testCtReference)

          await(res) shouldBe Left(CtReferenceMismatch)
        }
      }
    }
    "DES returns a failure" should {
      "return GetCtReferenceFailure" in {
        mockGetCtReference(testCompanyNumber)(Future.successful(Left(GetCtReferenceHttpParser.GetCtReferenceFailure(BAD_REQUEST, ""))))

        val res = TestCompanyMatchService.checkCompanyMatch(testCompanyNumber, testCtReference)

        await(res) shouldBe Left(GetCtReferenceFailure)
      }
    }
  }
}
