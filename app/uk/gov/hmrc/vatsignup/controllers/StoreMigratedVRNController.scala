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
import play.api.libs.json.Json
import play.api.mvc.{Action, ControllerComponents}
import uk.gov.hmrc.auth.core.retrieve.Retrievals
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisedFunctions}
import uk.gov.hmrc.play.bootstrap.controller.BackendController
import uk.gov.hmrc.vatsignup.config.Constants.HttpCodeKey
import uk.gov.hmrc.vatsignup.controllers.StoreMigratedVRNController._
import uk.gov.hmrc.vatsignup.models.StoreVatNumberRequest
import uk.gov.hmrc.vatsignup.services.StoreMigratedVRNService
import uk.gov.hmrc.vatsignup.services.StoreMigratedVRNService._

import scala.concurrent.ExecutionContext

@Singleton
class StoreMigratedVRNController @Inject()(val authConnector: AuthConnector,
                                           storeMigratedVRNService: StoreMigratedVRNService,
                                           cc: ControllerComponents)
                                          (implicit ec: ExecutionContext) extends BackendController(cc) with AuthorisedFunctions {

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
              case Left(NoVatEnrolment) => Forbidden(Json.obj(HttpCodeKey -> VatEnrolmentMissingCode))
              case Left(VatNumberDoesNotMatch) => Forbidden(Json.obj(HttpCodeKey -> VatNumberMismatchCode))
              case Left(KnownFactsMismatch) => Forbidden(Json.obj(HttpCodeKey -> KnownFactsMismatchCode))
              case Left(AgentClientRelationshipNotFound) => Forbidden(Json.obj(HttpCodeKey -> NoRelationshipCode))
              case Left(AgentClientRelationshipFailure) => InternalServerError(Json.obj(HttpCodeKey -> RelationshipCheckFailure))
              case Left(UpsertMigratedVRNFailure) => InternalServerError(Json.obj(HttpCodeKey -> StoreVrnFailure))
            }
        }
    }
  }
}

object StoreMigratedVRNController {
  val VatEnrolmentMissingCode = "NO_VAT_ENROLMENT"
  val NoRelationshipCode = "RELATIONSHIP_NOT_FOUND"
  val KnownFactsMismatchCode = "KNOWN_FACTS_MISMATCH"
  val VatNumberMismatchCode = "VAT_NUMBER_MISMATCH"
  val RelationshipCheckFailure = "RELATIONSHIP_CHECK_FAILURE"
  val StoreVrnFailure = "STORE_VRN_FAILURE"
}
