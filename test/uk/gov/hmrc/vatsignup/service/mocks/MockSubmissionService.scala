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

package uk.gov.hmrc.vatsignup.service.mocks

import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, Suite}
import play.api.test.Helpers._
import play.api.mvc.Request
import uk.gov.hmrc.auth.core.Enrolments
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.vatsignup.models.SignUpRequest
import uk.gov.hmrc.vatsignup.services.SubmissionService
import uk.gov.hmrc.vatsignup.services.SubmissionService._

import scala.concurrent.Future

trait MockSubmissionService extends MockitoSugar with BeforeAndAfterEach {
  this: Suite =>

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockSubmissionService)
  }

  val mockSubmissionService: SubmissionService = mock[SubmissionService]

  def mockSubmitSignUpRequest(signUpRequest: SignUpRequest, enrolments: Enrolments)(response: Future[SignUpRequestSubmissionResponse]): Unit =
    when(mockSubmissionService.submitSignUpRequest(
      ArgumentMatchers.eq(signUpRequest),
      ArgumentMatchers.eq(enrolments)
    )(ArgumentMatchers.any[HeaderCarrier],
      ArgumentMatchers.any[Request[_]])) thenReturn response

  def mockSubmitEnrolmentFailure(signUpRequest: SignUpRequest, enrolments: Enrolments): Unit =
    mockSubmitSignUpRequest(signUpRequest, enrolments)(Future.successful(Left(EnrolmentFailure)))

  def mockSignUpFailure(signUpRequest: SignUpRequest, enrolments: Enrolments): Unit =
    mockSubmitSignUpRequest(signUpRequest, enrolments)(Future.successful(Left(SignUpFailure(INTERNAL_SERVER_ERROR, ""))))

  def mockRegistrationFailure(signUpRequest: SignUpRequest, enrolments: Enrolments): Unit =
    mockSubmitSignUpRequest(signUpRequest, enrolments)(Future.successful(Left(RegistrationFailure(INTERNAL_SERVER_ERROR, ""))))

  def mockSubmitSignUpRequestSuccessful(signUpRequest: SignUpRequest, enrolments: Enrolments): Unit =
    mockSubmitSignUpRequest(signUpRequest, enrolments)(Future.successful(Right(SignUpRequestSubmitted)))

}
