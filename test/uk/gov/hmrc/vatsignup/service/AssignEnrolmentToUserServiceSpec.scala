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

import play.api.test.Helpers._
import uk.gov.hmrc.http.HeaderCarrier
import org.scalatest.{WordSpec, Matchers}
import uk.gov.hmrc.vatsignup.connectors.mocks.MockEnrolmentStoreProxyConnector
import uk.gov.hmrc.vatsignup.helpers.TestConstants._
import uk.gov.hmrc.vatsignup.httpparsers.AssignEnrolmentToUserHttpParser.{EnrolmentAssigned, EnrolmentAssignmentFailure}
import uk.gov.hmrc.vatsignup.services.AssignEnrolmentToUserService
import uk.gov.hmrc.vatsignup.services.AssignEnrolmentToUserService.{EnrolmentAssignedToUsers, EnrolmentAssignmentFailed}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AssignEnrolmentToUserServiceSpec extends WordSpec with Matchers with MockEnrolmentStoreProxyConnector {

  object TestAssignEnrolmentToUserService extends AssignEnrolmentToUserService(mockEnrolmentStoreProxyConnector)

  implicit val hc: HeaderCarrier = HeaderCarrier()

  "assignEnrolment" should {
    "Return Right(EnrolmentsAssigned)" when {
      "Tax Enrolments returns that all the users have been assigned the enrolment" in {
        testUserIdSet foreach (
          userId => mockAssignEnrolment(userId, testVatNumber)(Future.successful(Right(EnrolmentAssigned)))
          )

        val res = await(TestAssignEnrolmentToUserService.assignEnrolment(testUserIdSet, testVatNumber))

        res shouldBe Right(EnrolmentAssignedToUsers)
      }
    }

    "Return a Left(EnrolmentAssignmentFailed)" when {
      "Tax Enrolments returns that it failed to assign some of the enrolments" in {

        val testUserIdSet = Set("123456", "asdfgh", "qwerty", "zxcvbn")

        mockAssignEnrolment("123456", testVatNumber)(Future.successful(Right(EnrolmentAssigned)))
        mockAssignEnrolment("asdfgh", testVatNumber)(Future.successful(Right(EnrolmentAssigned)))
        mockAssignEnrolment("qwerty", testVatNumber)(Future.successful(Left(EnrolmentAssignmentFailure(BAD_REQUEST, ""))))
        mockAssignEnrolment("zxcvbn", testVatNumber)(Future.successful(Left(EnrolmentAssignmentFailure(BAD_REQUEST, ""))))

        val res = await(TestAssignEnrolmentToUserService.assignEnrolment(testUserIdSet, testVatNumber))

        res shouldBe Left(EnrolmentAssignmentFailed(Set("qwerty", "zxcvbn")))
      }
    }
  }

}
