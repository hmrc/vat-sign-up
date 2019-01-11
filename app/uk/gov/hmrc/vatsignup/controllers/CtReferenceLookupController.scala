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
import play.api.libs.json.JsPath
import play.api.mvc.Action
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisedFunctions}
import uk.gov.hmrc.play.bootstrap.controller.BaseController
import uk.gov.hmrc.vatsignup.models.SubscriptionRequest.companyNumberKey
import uk.gov.hmrc.vatsignup.services.CtReferenceLookupService
import uk.gov.hmrc.vatsignup.services.CtReferenceLookupService.{CtReferenceIsFound, CtReferenceNotFound}

import scala.concurrent.ExecutionContext

@Singleton
class CtReferenceLookupController @Inject()(val authConnector: AuthConnector,
                                            checkCtReferenceExistsService: CtReferenceLookupService
                                           )(implicit ec: ExecutionContext) extends BaseController with AuthorisedFunctions {

  val checkCtReferenceExists: Action[String] =
    Action.async(parse.json((JsPath \ companyNumberKey).read[String])) { implicit req =>
      authorised() {
        val companyNumber = req.body
        checkCtReferenceExistsService.checkCtReferenceExists(companyNumber) map {
          case Right(CtReferenceIsFound) => Ok
          case Left(CtReferenceNotFound) => NotFound
          case Left(_) => InternalServerError
        }
      }
    }

}
