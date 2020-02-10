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
import play.api.mvc.Request
import uk.gov.hmrc.auth.core.Enrolments
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.vatsignup.models.VatKnownFacts
import uk.gov.hmrc.vatsignup.services.StoreMigratedVRNService._
import uk.gov.hmrc.vatsignup.services._

import scala.concurrent.Future

trait MockStoreMigratedVRNService extends MockitoSugar with BeforeAndAfterEach {
  self: Suite =>

  val mockStoreMigratedVRNService = mock[StoreMigratedVRNService]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockStoreMigratedVRNService)
  }

  def mockStoreVatNumber(vatNumber: String,
                         enrolments: Enrolments = Enrolments(Set.empty),
                         optKnownFacts: Option[VatKnownFacts] = None
                        )(response: Future[Either[StoreMigratedVRNFailure, StoreMigratedVRNSuccess.type]]): Unit =
   when(mockStoreMigratedVRNService.storeVatNumber(
     ArgumentMatchers.eq(vatNumber),
     ArgumentMatchers.eq(enrolments),
     ArgumentMatchers.eq(optKnownFacts)
   )(
     ArgumentMatchers.any[HeaderCarrier],
     ArgumentMatchers.any[Request[_]]
   )) thenReturn response

}
