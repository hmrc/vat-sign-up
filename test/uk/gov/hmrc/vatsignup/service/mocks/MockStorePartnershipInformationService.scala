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

package uk.gov.hmrc.vatsignup.service.mocks

import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, Suite}
import play.api.mvc.Request
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.vatsignup.models.PartnershipBusinessEntity
import uk.gov.hmrc.vatsignup.services.StorePartnershipInformationService
import uk.gov.hmrc.vatsignup.services.StorePartnershipInformationService.StorePartnershipResponse

import scala.concurrent.Future

trait MockStorePartnershipInformationService extends MockitoSugar with BeforeAndAfterEach {
  self: Suite =>

  val mockStorePartnershipInformationService: StorePartnershipInformationService = mock[StorePartnershipInformationService]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockStorePartnershipInformationService)
  }

  def mockStorePartnershipInformationWithEnrolment(vatNumber: String,
                                                   partnership: PartnershipBusinessEntity,
                                                   enrolmentSautr: String
                                                  )(response: Future[StorePartnershipResponse]): Unit = {
    when(mockStorePartnershipInformationService.storePartnershipInformationWithEnrolment(
      ArgumentMatchers.eq(vatNumber),
      ArgumentMatchers.eq(partnership),
      ArgumentMatchers.eq(enrolmentSautr)
    )(ArgumentMatchers.any[HeaderCarrier])) thenReturn response
  }

  def mockStorePartnershipInformation(vatNumber: String,
                                      partnershipInformation: PartnershipBusinessEntity,
                                      businessPostcode: Option[String]
                                     )(response: Future[StorePartnershipResponse]): Unit = {
    when(mockStorePartnershipInformationService.storePartnershipInformation(
      ArgumentMatchers.eq(vatNumber),
      ArgumentMatchers.eq(partnershipInformation),
      ArgumentMatchers.eq(businessPostcode)
    )(ArgumentMatchers.any[HeaderCarrier], ArgumentMatchers.any[Request[_]])) thenReturn response
  }
}
