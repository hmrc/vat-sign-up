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
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, Suite}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.vatsignup.services.StoreRegisteredSocietyService
import uk.gov.hmrc.vatsignup.services.StoreRegisteredSocietyService._

import scala.concurrent.Future

trait MockStoreRegisteredSocietyService extends MockitoSugar with BeforeAndAfterEach {
  self: Suite =>

  val mockStoreRegisteredSocietyService: StoreRegisteredSocietyService = mock[StoreRegisteredSocietyService]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockStoreRegisteredSocietyService)
  }

  def mockStoreRegisteredSociety(vatNumber: String, companyNumber: String)(response: Future[Either[StoreRegisteredSocietyFailure, StoreRegisteredSocietySuccess.type]]): Unit = {
    when(mockStoreRegisteredSocietyService.storeRegisteredSociety(
      ArgumentMatchers.eq(vatNumber),
      ArgumentMatchers.eq(companyNumber)
    )(ArgumentMatchers.any[HeaderCarrier])) thenReturn response
  }

}
