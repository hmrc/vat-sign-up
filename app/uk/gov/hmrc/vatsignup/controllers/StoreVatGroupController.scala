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
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisedFunctions}
import uk.gov.hmrc.play.bootstrap.controller.BaseController
import uk.gov.hmrc.vatsignup.services.StoreVatGroupService
import uk.gov.hmrc.vatsignup.services.StoreVatGroupService._

import scala.concurrent.ExecutionContext

@Singleton
class StoreVatGroupController @Inject()(val authConnector: AuthConnector,
                                        storeVatGroupService: StoreVatGroupService
                                       )(implicit ec: ExecutionContext) extends BaseController with AuthorisedFunctions {

  def storeVatGroup(vatNumber: String): Action[AnyContent] = Action.async {
    implicit req => authorised() {
      storeVatGroupService.storeVatGroup(vatNumber) map {
        case Right(_) => NoContent
        case Left(VatGroupDatabaseFailureNoVATNumber) => NotFound
        case Left(_) => InternalServerError
      }
    }
  }

}
