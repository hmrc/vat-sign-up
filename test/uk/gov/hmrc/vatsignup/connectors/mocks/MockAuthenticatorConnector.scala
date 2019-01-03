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

package uk.gov.hmrc.vatsignup.connectors.mocks

import org.mockito.ArgumentMatchers
import org.mockito.Mockito.{reset, _}
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, Suite}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.vatsignup.connectors.AuthenticatorConnector
import uk.gov.hmrc.vatsignup.helpers.TestConstants._
import uk.gov.hmrc.vatsignup.httpparsers.MatchUserHttpParser.{MatchUserResponse, UserMatchFailureResponseModel}
import uk.gov.hmrc.vatsignup.models.UserDetailsModel

import scala.concurrent.Future

trait MockAuthenticatorConnector extends MockitoSugar with BeforeAndAfterEach {
  this: Suite =>

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockAuthenticatorConnector)
  }

  val mockAuthenticatorConnector: AuthenticatorConnector = mock[AuthenticatorConnector]

  private def mockMatchUser(userDetails: UserDetailsModel)(response: Future[MatchUserResponse]): Unit =
    when(mockAuthenticatorConnector.matchUser(ArgumentMatchers.eq(userDetails))(ArgumentMatchers.any[HeaderCarrier])) thenReturn response

  def mockMatchUserMatched(userDetails: UserDetailsModel): Unit =
    mockMatchUser(userDetails)(Future.successful(Right(Some(testNino))))

  def mockMatchUserNotMatched(userDetails: UserDetailsModel): Unit =
    mockMatchUser(userDetails)(Future.successful(Right(None)))

  def mockMatchUserFailure(userDetails: UserDetailsModel): Unit =
    mockMatchUser(userDetails)(Future.successful(Left(UserMatchFailureResponseModel(testErrorMsg))))
}

