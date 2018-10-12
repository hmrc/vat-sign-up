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
import play.api.mvc.Request
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.vatsignup.connectors.GetCtReferenceConnector
import uk.gov.hmrc.vatsignup.models.monitoring.CtReferenceMatchAuditing.CtReferenceMatchAuditModel
import uk.gov.hmrc.vatsignup.services.CompanyMatchService._
import uk.gov.hmrc.vatsignup.services.monitoring.AuditService

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CompanyMatchService @Inject()(getCtReferenceConnector: GetCtReferenceConnector,
                                    auditService: AuditService
                                   )(implicit ec: ExecutionContext) {
  def checkCompanyMatch(companyNumber: String, ctReference: String)(implicit hc: HeaderCarrier, request: Request[_]): Future[CheckCompanyMatchResponse] = {
    getCtReferenceConnector.getCtReference(companyNumber) map {
      case Right(retrievedCtReference) =>
        val isMatch = retrievedCtReference == ctReference
        auditService.audit(CtReferenceMatchAuditModel(companyNumber, ctReference, retrievedCtReference, isMatch))

        if (isMatch) Right(CompanyVerified)
        else Left(CtReferenceMismatch)
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
