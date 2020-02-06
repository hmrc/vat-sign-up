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
import org.scalatest.{BeforeAndAfterEach, Suite}
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.vatsignup.services.StoreOverseasService
import uk.gov.hmrc.vatsignup.services.StoreOverseasService.{StoreOverseasFailure, StoreOverseasSuccess}

import scala.concurrent.Future

trait MockStoreOverseasService extends MockitoSugar with BeforeAndAfterEach {
  self: Suite =>

  val mockStoreOverseasService: StoreOverseasService = mock[StoreOverseasService]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockStoreOverseasService)
  }

  def mockStoreOverseas(vatNumber: String)(response: Future[Either[StoreOverseasFailure, StoreOverseasSuccess.type]]): Unit =
    when(mockStoreOverseasService.storeOverseas(
      ArgumentMatchers.eq(vatNumber)
    )(ArgumentMatchers.any[HeaderCarrier])) thenReturn response

}
