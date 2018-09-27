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

package uk.gov.hmrc.vatsignup.service.mocks

import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatest.Suite
import org.scalatest.mockito.MockitoSugar
import play.api.mvc.Request
import uk.gov.hmrc.auth.core.Enrolments
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.vatsignup.services.StoreVatNumberWithRequestIdService._
import uk.gov.hmrc.vatsignup.services._

import scala.concurrent.Future

trait MockStoreVatNumberWithRequestIdService extends MockitoSugar {
  self: Suite =>

  val mockStoreVatNumberWithRequestIdService: StoreVatNumberWithRequestIdService = mock[StoreVatNumberWithRequestIdService]

  def mockStoreVatNumber(requestId: String,
                         vatNumber: String,
                         enrolments: Enrolments,
                         businessPostcode: Option[String] = None,
                         vatRegistrationDate: Option[String] = None,
                         isFromBta: Option[Boolean] = None
                        )(response: Future[Either[StoreVatNumberFailure, StoreVatNumberSuccess.type]]): Unit =
    when(mockStoreVatNumberWithRequestIdService.storeVatNumber(
      ArgumentMatchers.eq(requestId),
      ArgumentMatchers.eq(vatNumber),
      ArgumentMatchers.any[Enrolments],
      ArgumentMatchers.eq(businessPostcode),
      ArgumentMatchers.eq(vatRegistrationDate),
      ArgumentMatchers.eq(isFromBta)
    )(ArgumentMatchers.any[HeaderCarrier],
      ArgumentMatchers.any[Request[_]])) thenReturn response

}
