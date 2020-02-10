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
import uk.gov.hmrc.vatsignup.services.AssignEnrolmentToUserService
import uk.gov.hmrc.vatsignup.services.AssignEnrolmentToUserService._

import scala.concurrent.{ExecutionContext, Future}

trait MockAssignEnrolmentToUserService extends BeforeAndAfterEach with MockitoSugar {
  this: Suite =>

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockAssignEnrolmentToUserService)
  }

  val mockAssignEnrolmentToUserService: AssignEnrolmentToUserService = mock[AssignEnrolmentToUserService]

  def mockAssignEnrolmentToUser(userIds: Set[String], vatNumber: String)(response: Future[EnrolmentAssignmentResponse]): Unit = {
    when(mockAssignEnrolmentToUserService.assignEnrolment(
      ArgumentMatchers.eq(userIds),
      ArgumentMatchers.eq(vatNumber)
    )(ArgumentMatchers.any[ExecutionContext],
      ArgumentMatchers.any[HeaderCarrier])
    ) thenReturn response
  }
}
