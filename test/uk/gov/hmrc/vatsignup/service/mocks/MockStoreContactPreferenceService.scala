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

package uk.gov.hmrc.vatsignup.service.mocks

import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, Suite}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.vatsignup.models.ContactPreference
import uk.gov.hmrc.vatsignup.services.StoreContactPreferenceService
import uk.gov.hmrc.vatsignup.services.StoreContactPreferenceService._

import scala.concurrent.Future

trait MockStoreContactPreferenceService extends MockitoSugar with BeforeAndAfterEach {
  self: Suite =>

  val mockStoreContactPreferenceService: StoreContactPreferenceService = mock[StoreContactPreferenceService]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockStoreContactPreferenceService)
  }

  def mockStoreContactPreference(vatNumber: String,
                                 contactPreference: ContactPreference
                                )(response: Future[Either[StoreContactPreferenceFailure, ContactPreferenceStored.type]]): Unit = {
    when(mockStoreContactPreferenceService.storeContactPreference(
      ArgumentMatchers.eq(vatNumber),
      ArgumentMatchers.eq(contactPreference)
    )) thenReturn response
  }

}
