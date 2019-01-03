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
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, Suite}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.vatsignup.models.PartnershipInformation
import uk.gov.hmrc.vatsignup.services.StorePartnershipInformationWithRequestIdService
import uk.gov.hmrc.vatsignup.services.StorePartnershipInformationWithRequestIdService.{StorePartnershipInformationFailure, StorePartnershipInformationSuccess}

import scala.concurrent.Future

trait MockStorePartnershipInformationWithRequestIdService extends MockitoSugar with BeforeAndAfterEach {
  self: Suite =>

  val mockStorePartnershipInformationWithRequestIdService: StorePartnershipInformationWithRequestIdService = mock[StorePartnershipInformationWithRequestIdService]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockStorePartnershipInformationWithRequestIdService)
  }

  def mockStorePartnershipInformation(requestId: String,
                                      partnershipInformation: PartnershipInformation
                                     )(response: Future[Either[StorePartnershipInformationFailure, StorePartnershipInformationSuccess.type]]): Unit = {
    when(mockStorePartnershipInformationWithRequestIdService.storePartnershipInformation(
      ArgumentMatchers.eq(requestId),
      ArgumentMatchers.eq(partnershipInformation)
    )(ArgumentMatchers.any[HeaderCarrier])) thenReturn response
  }

  def mockStorePartnershipInformationSuccess(vatNumber: String,
                                             partnershipInformation: PartnershipInformation
                                            ): Unit =
    mockStorePartnershipInformation(vatNumber, partnershipInformation)(Future.successful(Right(StorePartnershipInformationSuccess)))

}
