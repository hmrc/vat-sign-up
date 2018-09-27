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
import org.scalatest.{BeforeAndAfterEach, Suite}
import org.scalatest.mockito.MockitoSugar
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.vatsignup.services.ClaimSubscriptionService
import uk.gov.hmrc.vatsignup.services.ClaimSubscriptionService.ClaimSubscriptionResponse
import org.mockito.Mockito._
import play.api.mvc.Request

import scala.concurrent.Future

trait MockClaimSubscriptionService extends BeforeAndAfterEach with MockitoSugar {
  this: Suite =>

  val mockClaimSubscriptionService: ClaimSubscriptionService = mock[ClaimSubscriptionService]

  def mockClaimSubscription(vatNumber: String, isFromtBta: Boolean)(response: Future[ClaimSubscriptionResponse]): Unit =
    when(mockClaimSubscriptionService.claimSubscription(
      ArgumentMatchers.eq(vatNumber),
      ArgumentMatchers.eq(isFromtBta)
    )(
      ArgumentMatchers.any[HeaderCarrier],
      ArgumentMatchers.any[Request[_]]
    )) thenReturn response

}
