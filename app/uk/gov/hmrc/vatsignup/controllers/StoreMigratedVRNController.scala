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

import play.api.mvc.Action
import uk.gov.hmrc.auth.core.retrieve.Retrievals
import uk.gov.hmrc.vatsignup.models.StoreVatNumberRequest
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisedFunctions}
import uk.gov.hmrc.play.bootstrap.controller.BaseController
import uk.gov.hmrc.vatsignup.services.StoreMigratedVRNService
import uk.gov.hmrc.vatsignup.services.StoreMigratedVRNService._

import scala.concurrent.ExecutionContext

@Singleton
class StoreMigratedVRNController @Inject()(val authConnector: AuthConnector,
                                           storeMigratedVRNService: StoreMigratedVRNService)
                                          (implicit ec: ExecutionContext) extends BaseController with AuthorisedFunctions {

  def storeVatNumber(): Action[StoreVatNumberRequest] = {
    Action.async(parse.json[StoreVatNumberRequest]) {
      implicit req =>
        val storeVatNumberRequest = req.body

        authorised().retrieve(Retrievals.allEnrolments) {
          enrolments =>
            storeMigratedVRNService.storeVatNumber(
              vatNumber = storeVatNumberRequest.vatNumber,
              enrolments = enrolments,
              optKnownFacts = storeVatNumberRequest.vatKnownFacts
            ) map {
              case Right(StoreMigratedVRNSuccess) => Ok
              case Left(NoVatEnrolment) => Forbidden
              case Left(DoesNotMatch) => Forbidden
              case _ => InternalServerError
            }
        }
    }
  }
}