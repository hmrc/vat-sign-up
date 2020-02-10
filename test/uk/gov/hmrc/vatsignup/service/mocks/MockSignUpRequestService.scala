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

package uk.gov.hmrc.vatsignup.service.mocks

import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, Suite}
import uk.gov.hmrc.auth.core.Enrolments
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.vatsignup.models.SignUpRequest
import uk.gov.hmrc.vatsignup.services.SignUpRequestService
import uk.gov.hmrc.vatsignup.services.SignUpRequestService._

import scala.concurrent.Future

trait MockSignUpRequestService extends MockitoSugar with BeforeAndAfterEach {
  this: Suite =>

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockSignUpRequestService)
  }

  val mockSignUpRequestService: SignUpRequestService = mock[SignUpRequestService]

  def mockGetSignUpRequest(vatNumber: String,
                           enrolments: Enrolments
                          )(response: Future[Either[GetSignUpRequestFailure, SignUpRequest]]): Unit = {
    when(mockSignUpRequestService.getSignUpRequest(
      ArgumentMatchers.eq(vatNumber),
      ArgumentMatchers.eq(enrolments)
    )(ArgumentMatchers.any[HeaderCarrier])) thenReturn response
  }

  def mockEmailVerificationFailure(vatNumber: String, enrolments: Enrolments): Unit =
    mockGetSignUpRequest(vatNumber, enrolments)(Future.successful(Left(EmailVerificationFailure)))

  def mockInsufficientData(vatNumber: String, enrolments: Enrolments): Unit =
    mockGetSignUpRequest(vatNumber, enrolments)(Future.successful(Left(InsufficientData)))

  def mockDatabaseFailure(vatNumber: String, enrolments: Enrolments): Unit =
    mockGetSignUpRequest(vatNumber, enrolments)(Future.successful(Left(DatabaseFailure)))

  def mockEmailVerificationRequired(vatNumber: String, enrolments: Enrolments): Unit =
    mockGetSignUpRequest(vatNumber, enrolments)(Future.successful(Left(EmailVerificationRequired)))

  def mockRequestNotAuthorised(vatNumber: String, enrolments: Enrolments): Unit =
    mockGetSignUpRequest(vatNumber, enrolments)(Future.successful(Left(RequestNotAuthorised)))

  def mockSignUpRequestNotFound(vatNumber: String, enrolments: Enrolments): Unit =
    mockGetSignUpRequest(vatNumber, enrolments)(Future.successful(Left(SignUpRequestNotFound)))

}
