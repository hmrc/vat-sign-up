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
import play.api.mvc.Request
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}
import uk.gov.hmrc.vatsignup.connectors.RegistrationConnector
import uk.gov.hmrc.vatsignup.httpparsers.RegisterWithMultipleIdentifiersHttpParser._
import uk.gov.hmrc.vatsignup.models.BusinessEntity
import uk.gov.hmrc.vatsignup.models.monitoring.RegisterWithMultipleIDsAuditing.RegisterWithMultipleIDsAuditModel
import uk.gov.hmrc.vatsignup.services.monitoring.AuditService

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class MigratedRegistrationService @Inject()(registrationConnector: RegistrationConnector,
                                            auditService: AuditService)
                                           (implicit ec: ExecutionContext) {

  def registerBusinessEntity(vatNumber: String, businessEntity: BusinessEntity, optArn: Option[String])
                            (implicit hc: HeaderCarrier, request: Request[_]): Future[String] =

    registrationConnector.registerBusinessEntity(vatNumber, businessEntity) map {
      case Right(RegisterWithMultipleIdsSuccess(safeId)) =>
        auditService.audit(RegisterWithMultipleIDsAuditModel(
          vatNumber = vatNumber,
          businessEntity = businessEntity,
          agentReferenceNumber = optArn,
          isSuccess = true
        ))
        safeId
      case Left(failure) =>
        auditService.audit(RegisterWithMultipleIDsAuditModel(
          vatNumber = vatNumber,
          businessEntity = businessEntity,
          agentReferenceNumber = optArn,
          isSuccess = false
        ))
        throw new InternalServerException(
          s"[MigratedRegistrationService] Failed to register business entity for VAT number $vatNumber with reason ${failure.body}"
        )
    }

}
