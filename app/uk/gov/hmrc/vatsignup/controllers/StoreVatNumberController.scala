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

package uk.gov.hmrc.vatsignup.controllers

import javax.inject.{Inject, Singleton}
import play.api.libs.json.Json
import play.api.mvc.{Action, ControllerComponents, Result}
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisedFunctions}
import uk.gov.hmrc.play.bootstrap.controller.BackendController
import uk.gov.hmrc.vatsignup.config.Constants.ControlList._
import uk.gov.hmrc.vatsignup.config.Constants._
import uk.gov.hmrc.vatsignup.controllers.StoreVatNumberController._
import uk.gov.hmrc.vatsignup.models.StoreVatNumberRequest
import uk.gov.hmrc.vatsignup.services.StoreVatNumberService._
import uk.gov.hmrc.vatsignup.services._

import scala.concurrent.ExecutionContext


@Singleton
class StoreVatNumberController @Inject()(val authConnector: AuthConnector,
                                         storeVatNumberService: StoreVatNumberService,
                                         cc: ControllerComponents
                                        )(implicit ec: ExecutionContext)
  extends BackendController(cc) with AuthorisedFunctions {

  def storeVatNumber: Action[StoreVatNumberRequest] =
    Action.async(parse.json[StoreVatNumberRequest]) {
      implicit req =>
        val storeVatNumberRequest = req.body

        authorised().retrieve(Retrievals.allEnrolments) {
          enrolments =>
            storeVatNumberService.storeVatNumber(
              vatNumber = storeVatNumberRequest.vatNumber,
              enrolments = enrolments,
              vatKnownFacts = storeVatNumberRequest.vatKnownFacts
            ) map {
              case Right(success) =>
                Ok(Json.obj(
                  OverseasKey -> success.isOverseas,
                  DirectDebitKey -> success.isDirectDebit
                ))
              case Left(failure) =>
                getErrorResponse(failure)
            }
        }
    }

  def getErrorResponse(failure: StoreVatNumberFailure): Result =
    failure match {
      case DoesNotMatchEnrolment =>
        Forbidden(Json.obj(HttpCodeKey -> "DoesNotMatchEnrolment"))
      case InsufficientEnrolments =>
        Forbidden(Json.obj(HttpCodeKey -> "InsufficientEnrolments"))
      case RelationshipNotFound =>
        Forbidden(Json.obj(HttpCodeKey -> NoRelationshipCode))
      case KnownFactsMismatch =>
        Forbidden(Json.obj(HttpCodeKey -> KnownFactsMismatchCode))
      case VatNotFound | VatInvalid =>
        PreconditionFailed
      case Ineligible(migratableDates) =>
        UnprocessableEntity(Json.toJson(migratableDates))
      case VatNumberDatabaseFailure =>
        InternalServerError
      case AgentServicesConnectionFailure | KnownFactsAndControlListInformationConnectionFailure =>
        BadGateway
    }

}

object StoreVatNumberController {
  val SubscriptionClaimedCode = "SUBSCRIPTION_CLAIMED"
  val NoRelationshipCode = "RELATIONSHIP_NOT_FOUND"
  val KnownFactsMismatchCode = "KNOWN_FACTS_MISMATCH"
}
