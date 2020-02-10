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

package uk.gov.hmrc.vatsignup.controllers

import javax.inject.{Inject, Singleton}
import play.api.Logger
import play.api.libs.json.JsValue
import play.api.mvc.{Action, ControllerComponents}
import uk.gov.hmrc.play.bootstrap.controller.BackendController
import uk.gov.hmrc.vatsignup.models.SubscriptionState
import uk.gov.hmrc.vatsignup.models.monitoring.TaxEnrolmentsCallbackAuditing.TaxEnrolmentsCallbackAuditModel
import uk.gov.hmrc.vatsignup.services.SubscriptionNotificationService
import uk.gov.hmrc.vatsignup.services.SubscriptionNotificationService.{EmailRequestDataNotFound, EmailServiceFailure}
import uk.gov.hmrc.vatsignup.services.monitoring.AuditService

import scala.concurrent.ExecutionContext

@Singleton
class TaxEnrolmentsCallbackController @Inject()(subscriptionNotificationService: SubscriptionNotificationService,
                                                auditService: AuditService,
                                                cc: ControllerComponents
                                               )(implicit ec: ExecutionContext)
  extends BackendController(cc) {

  val stateKey = "state"

  def taxEnrolmentsCallback(vatNumber: String): Action[JsValue] = Action.async(parse.json) {
    implicit req =>
      Logger.warn(s"taxEnrolmentsCallback($vatNumber)${req.body.toString()}")
      val state = (req.body \ stateKey).as[SubscriptionState]

      auditService.audit(TaxEnrolmentsCallbackAuditModel(vatNumber, req.body.toString()))

      subscriptionNotificationService.sendEmailNotification(vatNumber, state) map {
        case Right(_) => NoContent
        case Left(EmailRequestDataNotFound) => PreconditionFailed
        case Left(EmailServiceFailure) => BadGateway
      }
  }
}


