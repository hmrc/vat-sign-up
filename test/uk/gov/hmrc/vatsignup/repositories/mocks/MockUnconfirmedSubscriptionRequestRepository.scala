/*
 * Copyright 2018 HM Revenue & Customs
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

package uk.gov.hmrc.vatsignup.repositories.mocks

import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, Suite}
import reactivemongo.api.commands.UpdateWriteResult
import uk.gov.hmrc.vatsignup.models.{NinoSource, PartnershipInformation, UnconfirmedSubscriptionRequest}
import uk.gov.hmrc.vatsignup.repositories.UnconfirmedSubscriptionRequestRepository

import scala.concurrent.Future

trait MockUnconfirmedSubscriptionRequestRepository extends MockitoSugar with BeforeAndAfterEach {
  this: Suite =>
  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockUnconfirmedSubscriptionRequestRepository)
  }

  val mockUnconfirmedSubscriptionRequestRepository: UnconfirmedSubscriptionRequestRepository = mock[UnconfirmedSubscriptionRequestRepository]

  def mockGetRequestIdByCredential(credentialId: String)(response: Future[UnconfirmedSubscriptionRequest]): Unit =
    when(mockUnconfirmedSubscriptionRequestRepository.getRequestIdByCredential(ArgumentMatchers.eq(credentialId)))
      .thenReturn(response)

  def mockUpsertPartnership(requestId: String, partnershipInformation: PartnershipInformation)(response: Future[UpdateWriteResult]): Unit =
    when(mockUnconfirmedSubscriptionRequestRepository.upsertPartnership(
      ArgumentMatchers.eq(requestId),
      ArgumentMatchers.eq(partnershipInformation))
    ).thenReturn(response)

  def mockUpsertCompanyNumber(requestId: String, companyNumber: String)(response: Future[UpdateWriteResult]): Unit =
    when(mockUnconfirmedSubscriptionRequestRepository.upsertCompanyNumber(ArgumentMatchers.eq(requestId), ArgumentMatchers.eq(companyNumber)))
      .thenReturn(response)

  def mockUpsertCtReference(requestId: String, ctReference: String)(response: Future[UpdateWriteResult]): Unit =
    when(mockUnconfirmedSubscriptionRequestRepository.upsertCtReference(ArgumentMatchers.eq(requestId), ArgumentMatchers.eq(ctReference)))
      .thenReturn(response)

  def mockUpsertVatNumber(requestId: String, vatNumber: String, isMigratable: Boolean)(response: Future[UpdateWriteResult]): Unit =
    when(mockUnconfirmedSubscriptionRequestRepository.upsertVatNumber(
      requestId = ArgumentMatchers.eq(requestId),
      vatNumber = ArgumentMatchers.eq(vatNumber),
      isMigratable = ArgumentMatchers.eq(isMigratable)
    )).thenReturn(response)

  def mockUpsertEmail(requestId: String, email: String)(response: Future[UpdateWriteResult]): Unit =
    when(mockUnconfirmedSubscriptionRequestRepository.upsertEmail(ArgumentMatchers.eq(requestId), ArgumentMatchers.eq(email)))
      .thenReturn(response)

  def upsertTransactionEmail(requestId: String, email: String)(response: Future[UpdateWriteResult]): Unit =
    when(mockUnconfirmedSubscriptionRequestRepository.upsertTransactionEmail(ArgumentMatchers.eq(requestId), ArgumentMatchers.eq(email)))
      .thenReturn(response)

  def mockUpsertNino(requestId: String, nino: String, ninoSource: NinoSource)(response: Future[UpdateWriteResult]): Unit =
    when(mockUnconfirmedSubscriptionRequestRepository.upsertNino(
      ArgumentMatchers.eq(requestId),
      ArgumentMatchers.eq(nino),
      ArgumentMatchers.eq(ninoSource)
    )).thenReturn(response)

  def mockUpsertIdentityVerified(requestId: String)(response: Future[UpdateWriteResult]): Unit =
    when(mockUnconfirmedSubscriptionRequestRepository.upsertIdentityVerified(ArgumentMatchers.eq(requestId)))
      .thenReturn(response)
}
