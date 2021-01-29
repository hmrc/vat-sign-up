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

package uk.gov.hmrc.vatsignup.connectors.mocks

import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatest.{BeforeAndAfterEach, Suite}
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.vatsignup.connectors.EmailPasscodeVerificationConnector
import uk.gov.hmrc.vatsignup.httpparsers.EmailPasscodeVerificationHttpParser.EmailPasscodeVerificationResult

import scala.concurrent.Future

trait MockEmailPasscodeVerificationConnector extends MockitoSugar with BeforeAndAfterEach {
  this: Suite =>

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockEmailPasscodeVerificationConnector)
  }

  val mockEmailPasscodeVerificationConnector: EmailPasscodeVerificationConnector = mock[EmailPasscodeVerificationConnector]

  def mockVerifyEmailPasscode(emailAddress: String,
                                    passcode: String)
                                   (response: Future[EmailPasscodeVerificationResult]): Unit =
    when(mockEmailPasscodeVerificationConnector.verifyEmailPasscode(
      ArgumentMatchers.eq(emailAddress),
      ArgumentMatchers.eq(passcode)
    )(ArgumentMatchers.any[HeaderCarrier])) thenReturn response
}
