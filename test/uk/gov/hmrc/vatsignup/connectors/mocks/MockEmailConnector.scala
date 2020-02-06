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

package uk.gov.hmrc.vatsignup.connectors.mocks

import org.mockito.ArgumentMatchers
import org.mockito.Mockito.{reset, _}
import org.scalatest.{BeforeAndAfterEach, Suite}
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.vatsignup.connectors.EmailConnector
import uk.gov.hmrc.vatsignup.httpparsers.SendEmailHttpParser.SendEmailResponse

import scala.concurrent.Future

trait MockEmailConnector extends MockitoSugar with BeforeAndAfterEach {
  this: Suite =>

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockEmailConnector)
  }

  val mockEmailConnector: EmailConnector = mock[EmailConnector]

  def mockSendEmail(emailAddress: String,
                    emailTemplate: String,
                    vatNumber: Option[String])(response: Future[SendEmailResponse]): Unit =
    when(mockEmailConnector.sendEmail(
      ArgumentMatchers.eq(emailAddress),
      ArgumentMatchers.eq(emailTemplate),
      ArgumentMatchers.eq(vatNumber)
    )(
      ArgumentMatchers.any[HeaderCarrier]
    )) thenReturn response
}
