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

import play.api.libs.json.{JsSuccess, JsValue}
import play.api.mvc.Action
import uk.gov.hmrc.auth.core.retrieve.Retrievals
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisedFunctions}
import uk.gov.hmrc.play.bootstrap.controller.BaseController
import uk.gov.hmrc.vatsignup.models.NinoSource.ninoSourceFrontEndKey
import uk.gov.hmrc.vatsignup.models.{NinoSource, UserDetailsModel, UserEntered}
import uk.gov.hmrc.vatsignup.services.StoreNinoService
import uk.gov.hmrc.vatsignup.services.StoreNinoService._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class StoreNinoWithMatchingController @Inject()(val authConnector: AuthConnector,
                                                storeNinoService: StoreNinoService
                                   )(implicit ec: ExecutionContext)
  extends BaseController with AuthorisedFunctions {

  def storeNinoWithMatching(vatNumber: String): Action[JsValue] =
    Action.async(parse.json) {
      implicit req =>
        // TODO spike 1) handle cases where first name last name and dob may not be returned from cid (in future)
        // TODO spike 2) in the IR-SA flow, orchestrate or verify the nino using IR-SA
        authorised().retrieve(Retrievals.allEnrolments) {
          enrolments =>
            (req.body.validate[UserDetailsModel], (req.body \ ninoSourceFrontEndKey).validate[NinoSource]) match {
              case (JsSuccess(userDetails, _), JsSuccess(ninoSource, _)) =>
                storeNinoService.storeNinoWithMatching(vatNumber, userDetails, enrolments, ninoSource) map {
                  case Right(StoreNinoSuccess) => NoContent
                  case Left(AuthenticatorFailure) => InternalServerError("calls to authenticator failed")
                  case Left(NoMatchFoundFailure) => Forbidden
                  case Left(NinoDatabaseFailureNoVATNumber) => NotFound
                  case Left(NinoDatabaseFailure) => InternalServerError("calls to mongo failed")
                }
              case _ => Future.successful(BadRequest(req.body))
            }
        }
    }
}
