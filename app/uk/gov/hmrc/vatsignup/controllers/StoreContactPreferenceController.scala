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
import play.api.libs.json.{JsResult, JsValue, Reads}
import play.api.mvc.Action
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisedFunctions}
import uk.gov.hmrc.play.bootstrap.controller.BaseController
import uk.gov.hmrc.vatsignup.controllers.StoreContactPreferenceController.StoreContactPreferenceReader
import uk.gov.hmrc.vatsignup.models.ContactPreference
import uk.gov.hmrc.vatsignup.models.ContactPreference._
import uk.gov.hmrc.vatsignup.services.StoreContactPreferenceService
import uk.gov.hmrc.vatsignup.services.StoreContactPreferenceService._

import scala.concurrent.ExecutionContext

@Singleton
class StoreContactPreferenceController @Inject()(val authConnector: AuthConnector,
                                                 storeContactPreferenceService: StoreContactPreferenceService
                                                )(implicit ec: ExecutionContext) extends BaseController with AuthorisedFunctions {

  def storeContactPreference(vatNumber: String): Action[ContactPreference] =
    Action.async(parse.json(StoreContactPreferenceReader)) { implicit req =>
      authorised() {
        storeContactPreferenceService.storeContactPreference(vatNumber, req.body) map {
          case Right(_) => NoContent
          case Left(ContactPreferenceNoVatFound) => NotFound
          case Left(_) => InternalServerError
        }
      }
    }

}

object StoreContactPreferenceController {
  val contactPreferenceKey = "contactPreference"

  object StoreContactPreferenceReader extends Reads[ContactPreference] {
    override def reads(json: JsValue): JsResult[ContactPreference] = (json \ contactPreferenceKey).validate[ContactPreference]
  }

}
