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

package uk.gov.hmrc.vatsignup.controllers

import javax.inject.{Inject, Singleton}
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisedFunctions}
import uk.gov.hmrc.play.bootstrap.controller.BaseController
import uk.gov.hmrc.vatsignup.services.VatNumberEligibilityService
import uk.gov.hmrc.vatsignup.services.VatNumberEligibilityService._
import uk.gov.hmrc.vatsignup.controllers.NewVatEligibillityController._

import scala.concurrent.ExecutionContext

@Singleton
class NewVatEligibillityController @Inject()(val authConnector: AuthConnector,
                                             vatNumberEligibilityService: VatNumberEligibilityService)
                                            (implicit ec: ExecutionContext) extends BaseController with AuthorisedFunctions {

  def checkVatNumberEligibillity(vatNumber: String): Action[AnyContent] = Action.async {
    implicit request =>
      authorised() {
        vatNumberEligibilityService.getMtdStatus(vatNumber) map {
          case AlreadySubscribed => Ok(Json.obj(MtdStatusKey -> AlreadySubscribedValue))
          case Eligible(isMigrated, isOverseas) =>
            Ok(Json.obj(
              MtdStatusKey -> EligibleValue,
              EligiblityDetailsKey -> Json.obj(
                IsMigratedKey -> isMigrated,
                IsOverseasKey -> isOverseas)))
          case Ineligible => Ok(Json.obj(MtdStatusKey -> IneligibleValue))
          case Inhibited(migratableDates) => Ok(Json.obj(MtdStatusKey -> InhibitedValue, MigratableDatesKey -> Json.toJson(migratableDates)))
          case MigrationInProgress => Ok(Json.obj(MtdStatusKey -> MigraitonInProgressValue))
        }
      }

  }

}

object NewVatEligibillityController {

  val MtdStatusKey = "mtdStatus"
  val AlreadySubscribedValue = "AlreadySubscribed"
  val EligibleValue = "Eligible"
  val EligiblityDetailsKey = "eligibilityDetails"
  val IsMigratedKey = "isMigrated"
  val IsOverseasKey = "isOverseas"
  val IneligibleValue = "Ineligible"
  val InhibitedValue = "Inhibited"
  val MigratableDatesKey = "migratableDates"
  val MigraitonInProgressValue = "MigrationInProgress"

}