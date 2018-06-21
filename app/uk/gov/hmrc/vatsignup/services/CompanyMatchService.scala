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

import javax.inject.Inject
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.vatsignup.connectors.GetCtReferenceConnector
import uk.gov.hmrc.vatsignup.services.CompanyMatchService._

import scala.concurrent.{ExecutionContext, Future}

class CompanyMatchService @Inject()(getCtReferenceConnector: GetCtReferenceConnector
                                   )(implicit ec: ExecutionContext) {
  def checkCompanyMatch(companyNumber: String, ctReference: String)(implicit hc: HeaderCarrier): Future[CheckCompanyMatchResponse] = {
    getCtReferenceConnector.getCtReference(companyNumber) map {
      case Right(retrievedCtReference) if retrievedCtReference == ctReference =>
        Right(CompanyVerified)
      case Right(_) =>
        Left(CtReferenceMismatch)
      case Left(_) =>
        Left(GetCtReferenceFailure)
    }
  }

}

object CompanyMatchService {
  type CheckCompanyMatchResponse = Either[CheckCompanyMatchFailure, CompanyVerified.type]

  case object CompanyVerified
  sealed trait CheckCompanyMatchFailure
  case object CtReferenceMismatch extends CheckCompanyMatchFailure
  case object GetCtReferenceFailure extends CheckCompanyMatchFailure
}
