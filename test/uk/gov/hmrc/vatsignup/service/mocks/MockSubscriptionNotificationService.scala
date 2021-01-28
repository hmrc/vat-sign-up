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
import org.mockito.Mockito.reset
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, Suite}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.vatsignup.models.SubscriptionState
import uk.gov.hmrc.vatsignup.services.SubscriptionNotificationService
import org.mockito.Mockito._
import play.api.mvc.Request
import uk.gov.hmrc.vatsignup.services.SubscriptionNotificationService.{NotificationFailure, NotificationSent, NotificationSuccess}

import scala.concurrent.Future

trait MockSubscriptionNotificationService extends MockitoSugar with BeforeAndAfterEach {
  this: Suite =>

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockSubscriptionNotificationService)
  }

  val mockSubscriptionNotificationService: SubscriptionNotificationService = mock[SubscriptionNotificationService]

  def mockSendEmailNotification(vatNumber: String,
                                subscriptionState: SubscriptionState
                               )(response: Future[Either[NotificationFailure, NotificationSuccess]]): Unit =
    when(mockSubscriptionNotificationService.sendEmailNotification(
      ArgumentMatchers.eq(vatNumber),
      ArgumentMatchers.eq(subscriptionState)
    )(ArgumentMatchers.any[HeaderCarrier],
      ArgumentMatchers.any[Request[_]])
    ) thenReturn response
}
