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

package uk.gov.hmrc.vatsignup.services

import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.vatsignup.connectors.EnrolmentStoreProxyConnector
import uk.gov.hmrc.vatsignup.httpparsers.EnrolmentStoreProxyHttpParser
import uk.gov.hmrc.vatsignup.services.CheckEnrolmentAllocationService._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CheckEnrolmentAllocationService @Inject()(enrolmentStoreProxyConnector: EnrolmentStoreProxyConnector)
                                               (implicit ec: ExecutionContext) {
  def getEnrolmentAllocationStatus(vatNumber: String)
                                  (implicit hc: HeaderCarrier): Future[CheckEnrolmentAllocationResponse] = {
    enrolmentStoreProxyConnector.getAllocatedEnrolments(vatNumber) map {
      case Right(EnrolmentStoreProxyHttpParser.EnrolmentNotAllocated) => Right(EnrolmentNotAllocated)
      case Right(EnrolmentStoreProxyHttpParser.EnrolmentAlreadyAllocated) => Left(EnrolmentAlreadyAllocated)
      case Left(EnrolmentStoreProxyHttpParser.EnrolmentStoreProxyFailure(status)) => Left(UnexpectedEnrolmentStoreProxyFailure(status))
    }
  }

}

object CheckEnrolmentAllocationService {
  type CheckEnrolmentAllocationResponse = Either[CheckEnrolmentAllocationFailure, EnrolmentNotAllocated.type]

  case object EnrolmentNotAllocated

  sealed trait CheckEnrolmentAllocationFailure

  case object EnrolmentAlreadyAllocated extends CheckEnrolmentAllocationFailure

  case class UnexpectedEnrolmentStoreProxyFailure(status: Int) extends CheckEnrolmentAllocationFailure
}
