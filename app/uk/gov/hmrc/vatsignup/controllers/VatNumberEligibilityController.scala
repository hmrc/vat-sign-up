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
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisedFunctions}
import uk.gov.hmrc.play.bootstrap.controller.BackendController
import uk.gov.hmrc.vatsignup.config.Constants.ControlList.OverseasKey
import uk.gov.hmrc.vatsignup.services.ControlListEligibilityService._
import uk.gov.hmrc.vatsignup.services._

import scala.concurrent.ExecutionContext

@Singleton
class VatNumberEligibilityController @Inject()(val authConnector: AuthConnector,
                                               controlListEligibilityService: ControlListEligibilityService,
                                               cc: ControllerComponents
                                              )(implicit ec: ExecutionContext)
  extends BackendController(cc) with AuthorisedFunctions {

  def checkVatNumberEligibility(vatNumber: String): Action[AnyContent] = Action.async {
    implicit request =>
      authorised() {
        controlListEligibilityService.getEligibilityStatus(vatNumber) map {
          case Right(success) =>
            Ok(Json.obj(OverseasKey -> success.isOverseas))
          case Left(ControlListEligibilityService.IneligibleVatNumber(migratableDates)) =>
            BadRequest(Json.toJson(migratableDates))
          case Left(VatNumberNotFound | InvalidVatNumber) =>
            NotFound
          case _ =>
            BadGateway
        }
      }
  }
}
