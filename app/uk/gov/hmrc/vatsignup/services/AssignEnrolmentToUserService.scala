/*
 * Copyright 2019 HM Revenue & Customs
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

package uk.gov.hmrc.vatsignup.services


import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.vatsignup.connectors.TaxEnrolmentsConnector
import uk.gov.hmrc.vatsignup.services.AssignEnrolmentToUserService._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AssignEnrolmentToUserService @Inject()(taxEnrolmentsConnector: TaxEnrolmentsConnector) {

  def assignEnrolment(userIds: Set[String],
                      vatNumber: String
                     )(implicit ec: ExecutionContext, hc: HeaderCarrier): Future[EnrolmentAssignmentResponse] = {

    Future.sequence(
      userIds map {
        userId =>
          taxEnrolmentsConnector.assignEnrolment(userId, vatNumber)
      }
    ) map {
      userIds =>
        if (userIds.forall(_.isRight)) Right(EnrolmentAssignedToUsers) else Left(EnrolmentAssignmentFailed)
    }

  }

}

object AssignEnrolmentToUserService {

  type EnrolmentAssignmentResponse = Either[EnrolmentAssignmentFailed.type, EnrolmentAssignedToUsers.type]

  case object EnrolmentAssignedToUsers

  case object EnrolmentAssignmentFailed

}