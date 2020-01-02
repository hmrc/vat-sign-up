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
import play.api.libs.json.{JsSuccess, JsValue}
import play.api.mvc.Action
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisedFunctions}
import uk.gov.hmrc.play.bootstrap.controller.BaseController
import uk.gov.hmrc.vatsignup.models.BusinessEntity.NinoKey
import uk.gov.hmrc.vatsignup.services.StoreNinoService
import uk.gov.hmrc.vatsignup.services.StoreNinoService.{NinoDatabaseFailure, NinoDatabaseFailureNoVATNumber, StoreNinoSuccess}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class StoreNinoController @Inject()(val authConnector: AuthConnector, storeNinoService: StoreNinoService)
                                   (implicit ec: ExecutionContext) extends BaseController with AuthorisedFunctions {

  def storeNino(vatNumber: String): Action[JsValue] =
    Action.async(parse.json) { implicit req =>
      authorised() {
        (req.body \ NinoKey).validate[String] match {
          case JsSuccess(nino, _) =>
            storeNinoService.storeNino(vatNumber, nino) map {
              case Right(StoreNinoSuccess) => NoContent
              case Left(NinoDatabaseFailureNoVATNumber) => NotFound
              case Left(NinoDatabaseFailure) => InternalServerError
            }
          case _ => Future.successful(BadRequest)
        }
      }
    }
}
