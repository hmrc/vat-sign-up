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

package uk.gov.hmrc.vatsignup.service

import org.scalatest.{Matchers, WordSpec}
import play.api.test.Helpers._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.vatsignup.connectors.mocks.MockEnrolmentStoreProxyConnector
import uk.gov.hmrc.vatsignup.helpers.TestConstants._
import uk.gov.hmrc.vatsignup.httpparsers.EnrolmentStoreProxyHttpParser
import uk.gov.hmrc.vatsignup.httpparsers.EnrolmentStoreProxyHttpParser._
import uk.gov.hmrc.vatsignup.services.CheckEnrolmentAllocationService
import uk.gov.hmrc.vatsignup.services.CheckEnrolmentAllocationService._
import uk.gov.hmrc.vatsignup.utils.EnrolmentUtils._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CheckEnrolmentAllocationServiceSpec extends WordSpec with Matchers
  with MockEnrolmentStoreProxyConnector {

  object TestCheckEnrolmentAllocationService extends CheckEnrolmentAllocationService(
    mockEnrolmentStoreProxyConnector
  )

  implicit val hc: HeaderCarrier = HeaderCarrier()

  "getGroupIdForMtdVatEnrolment" when {
    "EnrolmentStoreProxy returns EnrolmentNotAllocated" should {
      "return EnrolmentNotAllocated" in {
        mockGetAllocatedEnrolment(mtdVatEnrolmentKey(testVatNumber))(Future.successful(Right(EnrolmentStoreProxyHttpParser.EnrolmentNotAllocated)))

        val res = TestCheckEnrolmentAllocationService.getGroupIdForMtdVatEnrolment(testVatNumber)

        await(res) shouldBe Right(CheckEnrolmentAllocationService.EnrolmentNotAllocated)
      }
    }
    "EnrolmentStoreProxy returns EnrolmentAlreadyAllocated" should {
      "return EnrolmentAlreadyAllocated" in {
        mockGetAllocatedEnrolment(mtdVatEnrolmentKey(testVatNumber))(
          Future.successful(Right(EnrolmentStoreProxyHttpParser.EnrolmentAlreadyAllocated(testGroupId)))
        )

        val res = TestCheckEnrolmentAllocationService.getGroupIdForMtdVatEnrolment(testVatNumber)

        await(res) shouldBe Left(CheckEnrolmentAllocationService.EnrolmentAlreadyAllocated(testGroupId))
      }
    }
    "EnrolmentStoreProxy returns an unexpected failure" should {
      "return Failure" in {
        mockGetAllocatedEnrolment(mtdVatEnrolmentKey(testVatNumber))(Future.successful(Left(EnrolmentStoreProxyFailure(BAD_REQUEST))))

        val res = TestCheckEnrolmentAllocationService.getGroupIdForMtdVatEnrolment(testVatNumber)

        await(res) shouldBe Left(UnexpectedEnrolmentStoreProxyFailure(BAD_REQUEST))
      }
    }
  }


  "getGroupIdForLegacyVatEnrolment" when {
    "EnrolmentStoreProxy returns EnrolmentNotAllocated" should {
      "return EnrolmentNotAllocated" in {
        mockGetAllocatedEnrolment(legacyVatEnrolmentKey(testVatNumber))(Future.successful(Right(EnrolmentStoreProxyHttpParser.EnrolmentNotAllocated)))

        val res = TestCheckEnrolmentAllocationService.getGroupIdForLegacyVatEnrolment(testVatNumber)

        await(res) shouldBe Right(CheckEnrolmentAllocationService.EnrolmentNotAllocated)
      }
    }
    "EnrolmentStoreProxy returns EnrolmentAlreadyAllocated" should {
      "return EnrolmentAlreadyAllocated" in {
        mockGetAllocatedEnrolment(legacyVatEnrolmentKey(testVatNumber))(
          Future.successful(Right(EnrolmentStoreProxyHttpParser.EnrolmentAlreadyAllocated(testGroupId)))
        )

        val res = TestCheckEnrolmentAllocationService.getGroupIdForLegacyVatEnrolment(testVatNumber)

        await(res) shouldBe Left(CheckEnrolmentAllocationService.EnrolmentAlreadyAllocated(testGroupId))
      }
    }
    "EnrolmentStoreProxy returns an unexpected failure" should {
      "return Failure" in {
        mockGetAllocatedEnrolment(legacyVatEnrolmentKey(testVatNumber))(Future.successful(Left(EnrolmentStoreProxyFailure(BAD_REQUEST))))

        val res = TestCheckEnrolmentAllocationService.getGroupIdForLegacyVatEnrolment(testVatNumber)

        await(res) shouldBe Left(UnexpectedEnrolmentStoreProxyFailure(BAD_REQUEST))
      }
    }
  }
}
