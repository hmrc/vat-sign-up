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

package uk.gov.hmrc.vatsignup.controllers

import javax.inject.{Inject, Singleton}
import play.api.libs.json.Json
import play.api.mvc.{Action, ControllerComponents}
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisedFunctions}
import uk.gov.hmrc.play.bootstrap.controller.BackendController
import uk.gov.hmrc.vatsignup.controllers.NewStoreTransactionEmailController.reasonKey
import uk.gov.hmrc.vatsignup.models.StoreTransactionEmailRequest
import uk.gov.hmrc.vatsignup.services.NewStoreEmailService._
import uk.gov.hmrc.vatsignup.services._

import scala.concurrent.ExecutionContext

@Singleton
class NewStoreTransactionEmailController @Inject()(val authConnector: AuthConnector,
                                                   emailService: NewStoreEmailService,
                                                   cc: ControllerComponents
                                                  )(implicit ec: ExecutionContext)
  extends BackendController(cc) with AuthorisedFunctions {

  def storeTransactionEmail(vatNumber: String): Action[StoreTransactionEmailRequest] =
    Action.async(parse.json[StoreTransactionEmailRequest]) {
      implicit request =>
        authorised() {
          val transactionEmailRequest = request.body
          emailService.storeTransactionEmail(vatNumber, transactionEmailRequest) map {
            case Right(StoreEmailSuccess) => Created
            case Left(EmailVerificationFailure(reason)) =>
              BadGateway(Json.obj(
                reasonKey -> reason
              ))
            case Left(EmailDatabaseFailureNoVATNumber) => NotFound
            case Left(EmailDatabaseFailure) => InternalServerError
          }

        }
    }
}

object NewStoreTransactionEmailController {
  val reasonKey = "reason"
}
