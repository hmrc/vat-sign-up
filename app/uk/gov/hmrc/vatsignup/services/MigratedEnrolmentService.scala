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
import uk.gov.hmrc.vatsignup.httpparsers.TaxEnrolmentsHttpParser.SuccessfulTaxEnrolment
import uk.gov.hmrc.vatsignup.services.MigratedEnrolmentService.{EnrolmentFailure, EnrolmentSuccess, MigratedEnrolmentResponse}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class MigratedEnrolmentService @Inject()(taxEnrolmentsConnector: TaxEnrolmentsConnector)
                                        (implicit ec: ExecutionContext) {

  def enrolForMtd(vatNumber: String, safeId: String)
                 (implicit hc: HeaderCarrier): Future[MigratedEnrolmentResponse] =

    taxEnrolmentsConnector.registerEnrolment(vatNumber, safeId) map {
      case Right(SuccessfulTaxEnrolment) => Right(EnrolmentSuccess)
      case Left(failure) => Left(EnrolmentFailure(failure.status))
    }

}

object MigratedEnrolmentService {
  type MigratedEnrolmentResponse = Either[EnrolmentFailure, EnrolmentSuccess.type]

  case object EnrolmentSuccess
  case class EnrolmentFailure(status: Int)
}
