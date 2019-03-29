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
import play.api.libs.json._
import play.api.mvc.Action
import uk.gov.hmrc.auth.core.retrieve.Retrievals
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisedFunctions}
import uk.gov.hmrc.http.InternalServerException
import uk.gov.hmrc.play.bootstrap.controller.BaseController
import uk.gov.hmrc.vatsignup.controllers.StorePartnershipInformationController._
import uk.gov.hmrc.vatsignup.models._
import uk.gov.hmrc.vatsignup.services.StorePartnershipInformationService
import uk.gov.hmrc.vatsignup.services.StorePartnershipInformationService._
import uk.gov.hmrc.vatsignup.utils.EnrolmentUtils._

import scala.concurrent.ExecutionContext

@Singleton
class StorePartnershipInformationController @Inject()(val authConnector: AuthConnector,
                                                      storePartnershipUtrService: StorePartnershipInformationService
                                                     )(implicit ec: ExecutionContext) extends BaseController with AuthorisedFunctions {

  def storePartnershipInformation(vatNumber: String): Action[StorePartnershipRequest] =
    Action.async(parse.json[StorePartnershipRequest](StorePartnershipRequest.reader)) { implicit request =>
      authorised().retrieve(Retrievals.allEnrolments) {

        val body = request.body

        enrolments =>
          (enrolments.partnershipUtr, body.postCode) match {
            case (Some(enrolmentUtr), _) =>
              storePartnershipUtrService.storePartnershipInformationWithEnrolment(vatNumber, body.partnership, enrolmentUtr) map {
                case Right(_) => NoContent
                case Left(EnrolmentMatchFailure) => Forbidden
                case Left(PartnershipInformationDatabaseFailureNoVATNumber) => PreconditionFailed
                case Left(_) => InternalServerError
              }
            case (None, Some(_)) =>
              storePartnershipUtrService.storePartnershipInformation(vatNumber, body.partnership, body.postCode) map {
                case Right(_) => NoContent
                case Left(KnownFactsMismatch) => Forbidden
                case Left(InsufficientData) => throw new InternalServerException("No postcodes returned for the partnership")
                case Left(InvalidSautr) => PreconditionFailed(Json.obj("statusCode" -> PRECONDITION_FAILED, "message" -> invalidSautrKey))
                case Left(PartnershipInformationDatabaseFailureNoVATNumber) => PreconditionFailed
                case Left(_) => InternalServerError
              }
            case (None, None) =>
              storePartnershipUtrService.storePartnershipInformation(vatNumber, body.partnership, None) map {
                case Right(_) => NoContent
                case Left(_) => InternalServerError
              }
          }
      }
    }

}

object StorePartnershipInformationController {
  val invalidSautrKey = "Invalid Sautr"
}