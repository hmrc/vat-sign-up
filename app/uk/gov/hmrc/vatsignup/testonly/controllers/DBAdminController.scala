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

//$COVERAGE-OFF$Disabling scoverage

package uk.gov.hmrc.vatsignup.testonly.controllers

import javax.inject.{Inject, Singleton}
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.play.bootstrap.controller.BackendController
import uk.gov.hmrc.vatsignup.testonly.services.{DatabaseAdminService, DatabaseCallSuccess, DeleteRecordFailure}

import scala.concurrent.ExecutionContext

@Singleton
class DBAdminController @Inject()(databaseAdminService: DatabaseAdminService,
                                  cc: ControllerComponents
                                 )(implicit ec: ExecutionContext)
  extends BackendController(cc) {

  def delete(vatNumber: String): Action[AnyContent] =
    Action.async {
      implicit req =>
        databaseAdminService.deleteRecord(vatNumber) map {
          case Right(DatabaseCallSuccess) => NoContent
          case Left(DeleteRecordFailure) => InternalServerError
        }
    }

}
// $COVERAGE-ON$
