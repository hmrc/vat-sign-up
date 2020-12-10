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

package uk.gov.hmrc.vatsignup.services

import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}
import uk.gov.hmrc.vatsignup.connectors.EnrolmentStoreProxyConnector
import uk.gov.hmrc.vatsignup.httpparsers.EnrolmentStoreProxyHttpParser
import uk.gov.hmrc.vatsignup.services.CheckEnrolmentAllocationService._
import uk.gov.hmrc.vatsignup.utils.EnrolmentUtils.{legacyVatEnrolmentKey, mtdVatEnrolmentKey}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CheckEnrolmentAllocationService @Inject()(enrolmentStoreProxyConnector: EnrolmentStoreProxyConnector)
                                               (implicit ec: ExecutionContext) {
  private def getGroupIdForEnrolment(enrolmentKey: String, ignoreAssignments: Boolean)
                                    (implicit hc: HeaderCarrier): Future[CheckEnrolmentAllocationResponse] = {
    enrolmentStoreProxyConnector.getAllocatedEnrolments(enrolmentKey, ignoreAssignments) map {
      case Right(EnrolmentStoreProxyHttpParser.EnrolmentNotAllocated) =>
        Right(EnrolmentNotAllocated)
      case Right(EnrolmentStoreProxyHttpParser.EnrolmentAlreadyAllocated(groupId)) =>
        Left(EnrolmentAlreadyAllocated(groupId))
      case Left(EnrolmentStoreProxyHttpParser.EnrolmentStoreProxyFailure(status)) =>
        Left(UnexpectedEnrolmentStoreProxyFailure(status))
      case unexpectedError =>
        throw new InternalServerException(s"[CheckEnrolmentAllocationService][getGroupIdForEnrolment] Unexpected error calling get allocated enrolments: $unexpectedError")
    }
  }

  def getGroupIdForMtdVatEnrolment(vatNumber: String, ignoreAssignments: Boolean)
                                  (implicit hc: HeaderCarrier): Future[CheckEnrolmentAllocationResponse] = {
    val enrolmentKey = mtdVatEnrolmentKey(vatNumber)

    getGroupIdForEnrolment(enrolmentKey, ignoreAssignments)
  }

  def getGroupIdForLegacyVatEnrolment(vatNumber: String)
                                     (implicit hc: HeaderCarrier): Future[CheckEnrolmentAllocationResponse] = {
    val enrolmentKey = legacyVatEnrolmentKey(vatNumber)

    getGroupIdForEnrolment(enrolmentKey, ignoreAssignments = false)
  }
}

object CheckEnrolmentAllocationService {
  type CheckEnrolmentAllocationResponse = Either[CheckEnrolmentAllocationFailure, EnrolmentNotAllocated.type]

  case object EnrolmentNotAllocated

  sealed trait CheckEnrolmentAllocationFailure

  case class EnrolmentAlreadyAllocated(groupId: String) extends CheckEnrolmentAllocationFailure

  case class UnexpectedEnrolmentStoreProxyFailure(status: Int) extends CheckEnrolmentAllocationFailure

}
