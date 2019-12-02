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
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.http.InternalServerException
import uk.gov.hmrc.play.bootstrap.controller.BaseController
import uk.gov.hmrc.vatsignup.config.AppConfig
import uk.gov.hmrc.vatsignup.services.AutoClaimEnrolmentService
import uk.gov.hmrc.vatsignup.services.AutoClaimEnrolmentService._
import uk.gov.hmrc.vatsignup.utils.AuthUtils._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class BulkMigrationAutoClaimEnrolmentController @Inject()(autoClaimEnrolmentService: AutoClaimEnrolmentService,
                                                          appConfig: AppConfig)
                                                         (implicit ec: ExecutionContext) extends BaseController {
  def autoClaimEnrolment(vatNumber: String): Action[AnyContent] = Action.async {
    implicit request =>
      if (getBasicAuth(request).contains(appConfig.expectedAuth)) {
        autoClaimEnrolmentService.autoClaimEnrolment(vatNumber, bulkMigration).map {
          case Right(EnrolmentAssigned) => NoContent
          case Left(EnrolmentNotAllocated) | Left(NoUsersFound) => NoContent
          case reason => throw new InternalServerException(s"Unexpected failure when trying to automatically claim an enrolment due to $reason")
        }
      } else Future.successful(Unauthorized.withHeaders(WWW_AUTHENTICATE -> s"""Basic realm="${appConfig.authRealm}""""))
  }

}
