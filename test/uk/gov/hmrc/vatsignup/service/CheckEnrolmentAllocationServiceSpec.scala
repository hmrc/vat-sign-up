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

import play.api.http.Status._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.vatsignup.connectors.mocks.MockEnrolmentStoreProxyConnector
import uk.gov.hmrc.vatsignup.helpers.TestConstants._
import uk.gov.hmrc.vatsignup.httpparsers.EnrolmentStoreProxyHttpParser
import uk.gov.hmrc.vatsignup.httpparsers.EnrolmentStoreProxyHttpParser._
import uk.gov.hmrc.vatsignup.services.CheckEnrolmentAllocationService
import uk.gov.hmrc.vatsignup.services.CheckEnrolmentAllocationService._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CheckEnrolmentAllocationServiceSpec extends UnitSpec
  with MockEnrolmentStoreProxyConnector {

  object TestCheckEnrolmentAllocationService extends CheckEnrolmentAllocationService(
    mockEnrolmentStoreProxyConnector
  )

  implicit val hc: HeaderCarrier = HeaderCarrier()

  "CheckEnrolmentAllocation" when {
    "EnrolmentStoreProxy returns EnrolmentNotAllocated" should {
      "return EnrolmentNotAllocated" in {
        mockGetAllocatedEnrolment(testVatNumber)(Future.successful(Right(EnrolmentStoreProxyHttpParser.EnrolmentNotAllocated)))

        val res = TestCheckEnrolmentAllocationService.getEnrolmentAllocationStatus(testVatNumber)

        await(res) shouldBe Right(CheckEnrolmentAllocationService.EnrolmentNotAllocated)
      }
    }
    "EnrolmentStoreProxy returns EnrolmentAlreadyAllocated" should {
      "return EnrolmentAlreadyAllocated" in {
        mockGetAllocatedEnrolment(testVatNumber)(Future.successful(Right(EnrolmentStoreProxyHttpParser.EnrolmentAlreadyAllocated)))

        val res = TestCheckEnrolmentAllocationService.getEnrolmentAllocationStatus(testVatNumber)

        await(res) shouldBe Left(CheckEnrolmentAllocationService.EnrolmentAlreadyAllocated)
      }
    }
  }
  "EnrolmentStoreProxy returns an unexpected failure" should {
    "return Failure" in {
      mockGetAllocatedEnrolment(testVatNumber)(Future.successful(Left(EnrolmentStoreProxyFailure(BAD_REQUEST))))

      val res = TestCheckEnrolmentAllocationService.getEnrolmentAllocationStatus(testVatNumber)

      await(res) shouldBe Left(UnexpectedEnrolmentStoreProxyFailure(BAD_REQUEST))
    }
  }
}
