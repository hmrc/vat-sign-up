/*
 * Copyright 2021 HM Revenue & Customs
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

import play.api.test.Helpers._
import play.api.mvc.Request
import play.api.test.FakeRequest
import uk.gov.hmrc.http.HeaderCarrier
import org.scalatest.{WordSpec, Matchers}
import uk.gov.hmrc.vatsignup.connectors.mocks.MockGetCtReferenceConnector
import uk.gov.hmrc.vatsignup.helpers.TestConstants._
import uk.gov.hmrc.vatsignup.httpparsers.GetCtReferenceHttpParser
import uk.gov.hmrc.vatsignup.services.CtReferenceLookupService
import uk.gov.hmrc.vatsignup.services.CtReferenceLookupService._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CtReferenceLookupServiceSpec extends WordSpec with Matchers
  with MockGetCtReferenceConnector {

  object TestCtReferenceLookupService extends CtReferenceLookupService(mockGetCtReferenceConnector)

  implicit val hc: HeaderCarrier = HeaderCarrier()
  implicit val request: Request[_] = FakeRequest()

  "checkCtReferenceExists" when {
    "DES returns a CT reference" should {
      "return CtReferenceIsFound" in {
        mockGetCtReference(testCompanyNumber)(
          Future.successful(Right(testCtReference))
        )

        val res = TestCtReferenceLookupService.checkCtReferenceExists(testCompanyNumber)

        await(res) shouldBe Right(CtReferenceIsFound)
      }
    }

    "the CT reference does not exist" should {
      "return CtReferenceNotFound" in {
        mockGetCtReference(testCompanyNumber)(
          Future.successful(Left(GetCtReferenceHttpParser.CtReferenceNotFound))
        )

        val res = TestCtReferenceLookupService.checkCtReferenceExists(testCompanyNumber)

        await(res) shouldBe Left(CtReferenceNotFound)
      }
    }

    "DES returns a failure" should {
      "return CheckCtReferenceExistsServiceFailure" in {
        mockGetCtReference(testCompanyNumber)(
          Future.successful(Left(GetCtReferenceHttpParser.GetCtReferenceFailure(BAD_REQUEST, "")))
        )

        val res = TestCtReferenceLookupService.checkCtReferenceExists(testCompanyNumber)

        await(res) shouldBe Left(CheckCtReferenceExistsServiceFailure)
      }
    }
  }
}
