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

package uk.gov.hmrc.vatsignup.controllers

import org.scalatest.{Matchers, WordSpec}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.Enrolments
import uk.gov.hmrc.vatsignup.connectors.mocks.MockAuthConnector
import uk.gov.hmrc.vatsignup.helpers.TestConstants._
import uk.gov.hmrc.vatsignup.service.mocks.MockMigratedSubmissionService
import uk.gov.hmrc.vatsignup.services.MigratedSubmissionService.SubmissionSuccess

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class MigratedSubmissionControllerSpec extends WordSpec
  with Matchers
  with MockAuthConnector
  with MockMigratedSubmissionService {

  object TestMigratedSubmissionController extends MigratedSubmissionController(
    mockAuthConnector,
    mockMigratedSubmissionService,
    stubControllerComponents()
  )

  "submit" when {
    "the user is a delegate and" when {
      val enrolments = Enrolments(Set(testAgentEnrolment))
      "the submission orchestration service returns a successful submission" should {
        "return NO_CONTENT" in {
          mockAuthRetrieveAgentEnrolment()
          mockSubmit(testVatNumber, enrolments)(Future.successful(SubmissionSuccess))

          val res = TestMigratedSubmissionController.submit(testVatNumber)(FakeRequest())

          status(res) shouldBe NO_CONTENT
        }
      }
      "the submission orchestration service throws an exception" should {
        "return the exception" in {
          mockAuthRetrieveAgentEnrolment()

          val testException = new Exception()
          mockSubmit(testVatNumber, enrolments)(Future.failed(testException))

          val res = TestMigratedSubmissionController.submit(testVatNumber)(FakeRequest())

          intercept[Exception](status(res)) shouldBe testException
        }
      }
    }
    "the user is principal and" when {
      val enrolments = Enrolments(Set(testPrincipalEnrolment))
      "the submission orchestration service returns a success" should {
        "return NO_CONTENT" in {
          mockAuthRetrievePrincipalEnrolment()
          mockSubmit(testVatNumber, enrolments)(Future.successful(SubmissionSuccess))

          val res = TestMigratedSubmissionController.submit(testVatNumber)(FakeRequest())

          status(res) shouldBe NO_CONTENT
        }
      }
      "the submission orchestration service throws an exception" should {
        "return the exception" in {
          mockAuthRetrievePrincipalEnrolment()

          val testException = new Exception()
          mockSubmit(testVatNumber, enrolments)(Future.failed(testException))

          val res = TestMigratedSubmissionController.submit(testVatNumber)(FakeRequest())

          intercept[Exception](status(res)) shouldBe testException
        }
      }
    }
  }

}
