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
import org.scalatestplus.mockito.MockitoSugar
import org.mockito.Mockito._
import org.mockito.internal.invocation.ArgumentMatcherAction
import org.scalatest.{BeforeAndAfterEach, Suite}
import play.api.mvc.Request
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.vatsignup.services.ControlListEligibilityService
import uk.gov.hmrc.vatsignup.services.ControlListEligibilityService.Eligibility

import scala.concurrent.Future

trait MockControlListEligibilityService extends MockitoSugar with BeforeAndAfterEach {
  this: Suite =>

  override def beforeEach(): Unit = {
    reset(mockControlListEligibilityService)
    super.beforeEach()
  }

  val mockControlListEligibilityService: ControlListEligibilityService = mock[ControlListEligibilityService]

  def mockGetEligibilityStatus(vatNumber: String)(response: Future[Eligibility]): Unit =
    when(mockControlListEligibilityService.getEligibilityStatus(
      ArgumentMatchers.eq(vatNumber)
    )(
      ArgumentMatchers.any[HeaderCarrier],
      ArgumentMatchers.any[Request[_]]
    )) thenReturn response
}
