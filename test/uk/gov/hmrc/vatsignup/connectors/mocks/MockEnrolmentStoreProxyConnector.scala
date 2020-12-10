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

package uk.gov.hmrc.vatsignup.connectors.mocks

import org.mockito.ArgumentMatchers
import org.mockito.Mockito.{reset, _}
import org.scalatest.{BeforeAndAfterEach, Suite}
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.vatsignup.connectors.EnrolmentStoreProxyConnector
import uk.gov.hmrc.vatsignup.httpparsers.AllocateEnrolmentResponseHttpParser.AllocateEnrolmentResponse
import uk.gov.hmrc.vatsignup.httpparsers.AssignEnrolmentToUserHttpParser.AssignEnrolmentToUserResponse
import uk.gov.hmrc.vatsignup.httpparsers.EnrolmentStoreProxyHttpParser.EnrolmentStoreProxyResponse
import uk.gov.hmrc.vatsignup.httpparsers.QueryUsersHttpParser.QueryUsersResponse

import scala.concurrent.Future

trait MockEnrolmentStoreProxyConnector extends MockitoSugar with BeforeAndAfterEach {
  this: Suite =>

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockEnrolmentStoreProxyConnector)
  }

  val mockEnrolmentStoreProxyConnector: EnrolmentStoreProxyConnector = mock[EnrolmentStoreProxyConnector]

  def mockGetAllocatedEnrolment(vatNumber: String, ignoreAssignments: Boolean)(response: Future[EnrolmentStoreProxyResponse]): Unit = {
    when(mockEnrolmentStoreProxyConnector.getAllocatedEnrolments(
      ArgumentMatchers.eq(vatNumber), ArgumentMatchers.eq(ignoreAssignments)
    )(ArgumentMatchers.any[HeaderCarrier])) thenReturn response
  }

  def mockGetUserIds(vatNumber: String)(response: Future[QueryUsersResponse]): Unit = {
    when(mockEnrolmentStoreProxyConnector.getUserIds(
      ArgumentMatchers.eq(vatNumber)
    )(ArgumentMatchers.any[HeaderCarrier])) thenReturn response
  }

  def mockAllocateEnrolmentWithoutKnownFacts(groupId: String,
                                             credentialId: String,
                                             vatNumber: String)(response: Future[AllocateEnrolmentResponse]): Unit =
    when(mockEnrolmentStoreProxyConnector.allocateEnrolmentWithoutKnownFacts(
      ArgumentMatchers.eq(groupId),
      ArgumentMatchers.eq(credentialId),
      ArgumentMatchers.eq(vatNumber)
    )(ArgumentMatchers.any[HeaderCarrier])) thenReturn response

  def mockAssignEnrolment(userId: String, vatNumber: String)(response: Future[AssignEnrolmentToUserResponse]): Unit =
    when(mockEnrolmentStoreProxyConnector.assignEnrolment(
      ArgumentMatchers.eq(userId),
      ArgumentMatchers.eq(vatNumber)
    )(ArgumentMatchers.any[HeaderCarrier])) thenReturn response

}
