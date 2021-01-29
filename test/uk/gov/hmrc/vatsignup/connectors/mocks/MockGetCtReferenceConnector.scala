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
import org.mockito.Mockito.reset
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, Suite}
import uk.gov.hmrc.vatsignup.connectors.GetCtReferenceConnector
import uk.gov.hmrc.vatsignup.httpparsers.GetCtReferenceHttpParser.GetCtReferenceResponse
import org.mockito.Mockito._
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

trait MockGetCtReferenceConnector extends BeforeAndAfterEach with MockitoSugar {
  this: Suite =>

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockGetCtReferenceConnector)
  }

  val mockGetCtReferenceConnector: GetCtReferenceConnector = mock[GetCtReferenceConnector]

  def mockGetCtReference(companyNumber: String)(response: Future[GetCtReferenceResponse]): Unit =
    when(mockGetCtReferenceConnector.getCtReference(
      ArgumentMatchers.eq(companyNumber)
    )(ArgumentMatchers.any[HeaderCarrier])
    ) thenReturn response
}
