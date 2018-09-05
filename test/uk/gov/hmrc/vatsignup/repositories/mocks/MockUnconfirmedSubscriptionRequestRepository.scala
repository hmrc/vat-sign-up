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

package uk.gov.hmrc.vatsignup.repositories.mocks

import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, Suite}
import uk.gov.hmrc.vatsignup.repositories.UnconfirmedSubscriptionRequestRepository

import scala.concurrent.Future

trait MockUnconfirmedSubscriptionRequestRepository extends MockitoSugar with BeforeAndAfterEach {
  this: Suite =>
  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockUnconfirmedSubscriptionRequestRepository)
  }

  val mockUnconfirmedSubscriptionRequestRepository: UnconfirmedSubscriptionRequestRepository = mock[UnconfirmedSubscriptionRequestRepository]

  def mockGetRequestIdByCredential(credentialId: String)(response: Future[String]): Unit =
    when(mockUnconfirmedSubscriptionRequestRepository.getRequestIdByCredential(ArgumentMatchers.eq(credentialId)))
      .thenReturn(response)

}
