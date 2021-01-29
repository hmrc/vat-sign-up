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

package uk.gov.hmrc.vatsignup.services


import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.vatsignup.connectors.EnrolmentStoreProxyConnector
import uk.gov.hmrc.vatsignup.services.AssignEnrolmentToUserService._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AssignEnrolmentToUserService @Inject()(enrolmentStoreProxyConnector: EnrolmentStoreProxyConnector) {

  def assignEnrolment(userIds: Set[String],
                      vatNumber: String
                     )(implicit ec: ExecutionContext, hc: HeaderCarrier): Future[EnrolmentAssignmentResponse] = {

    Future.sequence(
      userIds.toSeq.map { userId =>
        enrolmentStoreProxyConnector.assignEnrolment(userId, vatNumber)
      }
    ).map { userIdResponses =>
      if (userIdResponses.forall(_.isRight)) Right(EnrolmentAssignedToUsers)
      else {
        Left(EnrolmentAssignmentFailed(
          userIds.zip(
            userIdResponses.map { res =>
              res.isLeft
            }
          ).collect { case (userId, true) =>
            userId
          }
        ))
      }
    }

  }

}

object AssignEnrolmentToUserService {

  type EnrolmentAssignmentResponse = Either[EnrolmentAssignmentFailed, EnrolmentAssignedToUsers.type]

  case object EnrolmentAssignedToUsers

  case class EnrolmentAssignmentFailed(failedIds: Set[String])

}
