/*
 * Copyright 2018 HM Revenue & Customs
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
import uk.gov.hmrc.auth.core.retrieve.Retrievals
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisedFunctions}
import uk.gov.hmrc.http.BadRequestException
import uk.gov.hmrc.play.bootstrap.controller.BaseController
import uk.gov.hmrc.vatsignup.controllers.StorePartnershipInformationController.PartnershipBusinessEntityReader
import uk.gov.hmrc.vatsignup.models._
import uk.gov.hmrc.vatsignup.services.StorePartnershipInformationService
import uk.gov.hmrc.vatsignup.services.StorePartnershipInformationService.{EnrolmentMatchFailure, PartnershipInformationDatabaseFailureNoVATNumber}
import uk.gov.hmrc.vatsignup.utils.EnrolmentUtils._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class StorePartnershipInformationController @Inject()(val authConnector: AuthConnector,
                                                      storePartnershipUtrService: StorePartnershipInformationService
                                                     )(implicit ec: ExecutionContext) extends BaseController with AuthorisedFunctions {

  def storePartnershipInformation(vatNumber: String): Action[PartnershipBusinessEntity] = {
    Action.async(parse.json[PartnershipBusinessEntity](PartnershipBusinessEntityReader)) { implicit req =>
      authorised().retrieve(Retrievals.allEnrolments) {
        enrolments =>
          enrolments.partnershipUtr match {
            case Some(enrolmentUtr) =>
              storePartnershipUtrService.storePartnershipInformation(vatNumber, req.body, enrolmentUtr) map {
                case Right(_) => NoContent
                case Left(EnrolmentMatchFailure) => Forbidden
                case Left(PartnershipInformationDatabaseFailureNoVATNumber) => NotFound
                case Left(_) => InternalServerError
              }
            case None =>
              // TODO covered by a future story
              Future.successful(NotImplemented)
          }
      }
    }
  }

}

object StorePartnershipInformationController {
  implicit object PartnershipBusinessEntityReader extends Reads[PartnershipBusinessEntity] {
    override def reads(json: JsValue): JsResult[PartnershipBusinessEntity] =
      for {
        sautr <- (json \ "sautr").validate[String]
        optCompanyNumber <- (json \ "crn").validateOpt[String]
        partnershipType <- (json \ "partnershipType").validate[String]
      } yield (partnershipType, optCompanyNumber) match {
        case ("generalPartnership", _) => GeneralPartnership(sautr)
        case ("limitedPartnership", Some(companyNumber)) => LimitedPartnership(sautr, companyNumber)
        case ("limitedLiabilityPartnership", Some(companyNumber)) => LimitedLiabilityPartnership(sautr, companyNumber)
        case ("scottishLimitedPartnership", Some(companyNumber)) => ScottishLimitedPartnership(sautr, companyNumber)
        case _ => throw new BadRequestException(s"Invalid Partnership Information ${json.toString()}")
      }
  }
}
