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
import org.scalatest.{BeforeAndAfterEach, Suite}
import org.scalatest.mockito.MockitoSugar
import org.mockito.Mockito._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.vatsignup.connectors.MigratedCustomerSignUpConnector
import uk.gov.hmrc.vatsignup.httpparsers.CustomerSignUpHttpParser.CustomerSignUpResponse

import scala.concurrent.Future

trait MockMigratedCustomerSignUpConnector extends MockitoSugar with BeforeAndAfterEach {
  this: Suite =>

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockMigratedCustomerSignUpConnector)
  }

  val mockMigratedCustomerSignUpConnector: MigratedCustomerSignUpConnector = mock[MigratedCustomerSignUpConnector]

  def mockSignUpMigrated(safeId: String,
                         vatNumber: String
                        )(response: Future[CustomerSignUpResponse]): Unit = {
    when(mockMigratedCustomerSignUpConnector.signUp(
      ArgumentMatchers.eq(safeId),
      ArgumentMatchers.eq(vatNumber)
    )(ArgumentMatchers.any[HeaderCarrier])) thenReturn response
  }

}
