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
import play.api.mvc.Request
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}
import uk.gov.hmrc.vatsignup.connectors.MigratedCustomerSignUpConnector
import uk.gov.hmrc.vatsignup.models.CustomerSignUpResponseSuccess
import uk.gov.hmrc.vatsignup.models.monitoring.SignUpAuditing.MigratedSignUpAuditModel
import uk.gov.hmrc.vatsignup.services.MigratedSignUpService.MigratedSignUpSuccess
import uk.gov.hmrc.vatsignup.services.monitoring.AuditService

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class MigratedSignUpService @Inject()(signUpConnector: MigratedCustomerSignUpConnector,
                                      auditService: AuditService
                                     )(implicit ec: ExecutionContext) {

  def signUp(safeId: String,
             vatNumber: String,
             isMigratable: Boolean,
             optArn: Option[String])
            (implicit hc: HeaderCarrier,
             request: Request[_]
            ): Future[MigratedSignUpSuccess.type] =

    signUpConnector.signUp(
      safeId = safeId,
      vatNumber = vatNumber,
      isMigratable = isMigratable
    ) map {
      case Right(CustomerSignUpResponseSuccess) => {
        auditService.audit(MigratedSignUpAuditModel(
          safeId = safeId,
          vatNumber = vatNumber,
          agentReferenceNumber = optArn,
          isSuccess = true
        ))
        MigratedSignUpSuccess
      }
      case Left(failure) => {
        auditService.audit(MigratedSignUpAuditModel(
          safeId = safeId,
          vatNumber = vatNumber,
          agentReferenceNumber = optArn,
          isSuccess = false
        ))
        throw new InternalServerException(
          s"[MigratedSignUpService] Failed to sign up VAT number $vatNumber with reason: ${failure.response}"
        )
      }
    }

}

object MigratedSignUpService {

  case object MigratedSignUpSuccess

}
