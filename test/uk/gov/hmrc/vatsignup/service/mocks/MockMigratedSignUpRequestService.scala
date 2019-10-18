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

package uk.gov.hmrc.vatsignup.service.mocks

import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatest.{BeforeAndAfterEach, Suite}
import org.scalatest.mockito.MockitoSugar
import uk.gov.hmrc.auth.core.Enrolments
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.vatsignup.models.MigratedSignUpRequest
import uk.gov.hmrc.vatsignup.services.MigratedSignUpRequestService
import uk.gov.hmrc.vatsignup.services.MigratedSignUpRequestService.{SignUpRequestDeleted, SignUpRequestFailure}

import scala.concurrent.Future

trait MockMigratedSignUpRequestService extends MockitoSugar with BeforeAndAfterEach {
  this: Suite =>

  val mockMigratedSignUpRequestService = mock[MigratedSignUpRequestService]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockMigratedSignUpRequestService)
  }

  def mockGetSignUpRequest(vatNumber: String, enrolments: Enrolments)
                          (response: Future[MigratedSignUpRequest]): Unit =
    when(mockMigratedSignUpRequestService.getSignUpRequest(
      ArgumentMatchers.eq(vatNumber),
      ArgumentMatchers.eq(enrolments)
    )(
      ArgumentMatchers.any[HeaderCarrier]
    )) thenReturn response

  def mockDeleteSignUpRequest(vatNumber: String)(response: Future[SignUpRequestDeleted.type]): Unit =
    when(mockMigratedSignUpRequestService.deleteSignUpRequest(
      ArgumentMatchers.eq(vatNumber)
    )(
      ArgumentMatchers.any[HeaderCarrier]
    )) thenReturn response

}
