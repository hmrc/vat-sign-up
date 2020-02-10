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
import play.api.libs.json.{JsPath, Json}
import play.api.mvc.{Action, ControllerComponents}
import uk.gov.hmrc.auth.core.retrieve.Retrievals
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisedFunctions}
import uk.gov.hmrc.play.bootstrap.controller.BackendController
import uk.gov.hmrc.vatsignup.config.Constants.EmailVerification.EmailVerifiedKey
import uk.gov.hmrc.vatsignup.models.SubscriptionRequest.transactionEmailKey
import uk.gov.hmrc.vatsignup.services.StoreEmailService.{EmailDatabaseFailure, EmailDatabaseFailureNoVATNumber, EmailVerificationFailure, StoreEmailSuccess}
import uk.gov.hmrc.vatsignup.services._

import scala.concurrent.ExecutionContext

@Singleton
class StoreTransactionEmailController @Inject()(val authConnector: AuthConnector,
                                                storeEmailService: StoreEmailService,
                                                cc: ControllerComponents
                                               )(implicit ec: ExecutionContext)
  extends BackendController(cc) with AuthorisedFunctions {

  def storeTransactionEmail(vatNumber: String): Action[String] =
    Action.async(parse.json((JsPath \ transactionEmailKey).read[String])) {
      implicit req =>
        authorised().retrieve(Retrievals.allEnrolments) {
          enrolments =>
          val transactionEmail = req.body
          storeEmailService.storeTransactionEmail(vatNumber, transactionEmail, enrolments) map {
            case Right(StoreEmailSuccess(emailVerified)) =>
              Ok(Json.obj(
                EmailVerifiedKey -> emailVerified
              ))
            case Left(EmailDatabaseFailureNoVATNumber) => NotFound
            case Left(EmailDatabaseFailure) => InternalServerError
            case Left(EmailVerificationFailure) => BadGateway
          }

        }
    }

}
