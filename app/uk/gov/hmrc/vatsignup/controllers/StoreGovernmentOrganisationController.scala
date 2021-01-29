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
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisedFunctions}
import uk.gov.hmrc.play.bootstrap.controller.BackendController
import uk.gov.hmrc.vatsignup.services.StoreGovernmentOrganisationService
import uk.gov.hmrc.vatsignup.services.StoreGovernmentOrganisationService._

import scala.concurrent.ExecutionContext

@Singleton
class StoreGovernmentOrganisationController @Inject()(val authConnector: AuthConnector,
                                                      storeGovernmentOrganisationService: StoreGovernmentOrganisationService,
                                                      cc: ControllerComponents
                                                     )(implicit ec: ExecutionContext) extends BackendController(cc) with AuthorisedFunctions {

  def storeGovernmentOrganisation(vatNumber: String): Action[AnyContent] = Action.async {
    implicit req =>
      authorised() {
        storeGovernmentOrganisationService.storeGovernmentOrganisation(vatNumber) map {
          case Right(_) => NoContent
          case Left(GovernmentOrganisationDatabaseFailureNoVATNumber) => NotFound
          case Left(_) => InternalServerError
        }
      }
  }

}
