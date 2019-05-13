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
import uk.gov.hmrc.vatsignup.connectors.TaxEnrolmentsConnector
import uk.gov.hmrc.vatsignup.httpparsers.AllocateEnrolmentResponseHttpParser.AllocateEnrolmentResponse
import uk.gov.hmrc.vatsignup.httpparsers.TaxEnrolmentsHttpParser.TaxEnrolmentsResponse
import uk.gov.hmrc.vatsignup.httpparsers.UpsertEnrolmentResponseHttpParser.UpsertEnrolmentResponse

import scala.concurrent.Future

trait MockTaxEnrolmentsConnector extends MockitoSugar with BeforeAndAfterEach {
  this: Suite =>

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockTaxEnrolmentsConnector)
  }

  val mockTaxEnrolmentsConnector: TaxEnrolmentsConnector = mock[TaxEnrolmentsConnector]

  def mockUpsertEnrolment(vatNumber: String,
                          postcode: String,
                          vatRegistrationDate: String)(response: Future[UpsertEnrolmentResponse]): Unit = {
    when(mockTaxEnrolmentsConnector.upsertEnrolment(
      ArgumentMatchers.eq(vatNumber),
      ArgumentMatchers.eq(postcode),
      ArgumentMatchers.eq(vatRegistrationDate)
    )(ArgumentMatchers.any[HeaderCarrier])) thenReturn response

  }

  def mockRegisterEnrolment(vatNumber: String, safeId: String)(response: Future[TaxEnrolmentsResponse]): Unit = {
    when(mockTaxEnrolmentsConnector.registerEnrolment(
      ArgumentMatchers.eq(vatNumber),
      ArgumentMatchers.eq(safeId)
    )(ArgumentMatchers.any[HeaderCarrier])) thenReturn response
  }

  def mockAllocateEnrolment(groupId: String,
                            credentialId: String,
                            vatNumber: String,
                            postcode: String,
                            vatRegistrationDate: String)(response: Future[AllocateEnrolmentResponse]): Unit =
    when(mockTaxEnrolmentsConnector.allocateEnrolment(
      ArgumentMatchers.eq(groupId),
      ArgumentMatchers.eq(credentialId),
      ArgumentMatchers.eq(vatNumber),
      ArgumentMatchers.eq(postcode),
      ArgumentMatchers.eq(vatRegistrationDate)
    )(ArgumentMatchers.any[HeaderCarrier])) thenReturn response

  def mockAllocateEnrolmentWithoutKnownFacts(groupId: String,
                            credentialId: String,
                            vatNumber: String)(response: Future[AllocateEnrolmentResponse]): Unit =
    when(mockTaxEnrolmentsConnector.allocateEnrolmentWithoutKnownFacts(
      ArgumentMatchers.eq(groupId),
      ArgumentMatchers.eq(credentialId),
      ArgumentMatchers.eq(vatNumber)
    )(ArgumentMatchers.any[HeaderCarrier])) thenReturn response
}
