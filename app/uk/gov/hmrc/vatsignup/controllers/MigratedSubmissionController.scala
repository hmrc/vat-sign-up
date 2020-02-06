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
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.auth.core.retrieve.Retrievals
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisedFunctions}
import uk.gov.hmrc.play.bootstrap.controller.BackendController
import uk.gov.hmrc.vatsignup.services.MigratedSubmissionService
import uk.gov.hmrc.vatsignup.services.MigratedSubmissionService.SubmissionSuccess

import scala.concurrent.ExecutionContext

@Singleton
class MigratedSubmissionController @Inject()(val authConnector: AuthConnector,
                                             migratedSubmissionService: MigratedSubmissionService,
                                             cc: ControllerComponents
                                            )(implicit ec: ExecutionContext) extends BackendController(cc) with AuthorisedFunctions {

  def submit(vatNumber: String): Action[AnyContent] = Action.async {
    implicit request =>
      authorised().retrieve(Retrievals.allEnrolments) {
        enrolments =>
          migratedSubmissionService.submit(vatNumber, enrolments) map {
            case SubmissionSuccess => NoContent
            case _ => BadGateway
          }
      }
  }

}
