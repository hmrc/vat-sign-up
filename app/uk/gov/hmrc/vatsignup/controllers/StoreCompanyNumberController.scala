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
import play.api.libs.json.{JsResult, JsValue, Json, Reads}
import play.api.mvc.Action
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisedFunctions}
import uk.gov.hmrc.play.bootstrap.controller.BaseController
import uk.gov.hmrc.vatsignup.config.Constants._
import uk.gov.hmrc.vatsignup.controllers.StoreCompanyNumberController._
import uk.gov.hmrc.vatsignup.models.SubscriptionRequest._
import uk.gov.hmrc.vatsignup.services.StoreCompanyNumberService
import uk.gov.hmrc.vatsignup.services.StoreCompanyNumberService._

import scala.concurrent.ExecutionContext

@Singleton
class StoreCompanyNumberController @Inject()(val authConnector: AuthConnector,
                                             storeCompanyNumberService: StoreCompanyNumberService
                                            )(implicit ec: ExecutionContext)
  extends BaseController with AuthorisedFunctions {

  def storeCompanyNumber(vatNumber: String): Action[(String, Option[String])] =
    Action.async(parse.json(StoreCompanyNumberReader)) {
      implicit req =>
        authorised() {
          (req.body match {
            case (companyNumber, None) =>
              storeCompanyNumberService.storeCompanyNumber(vatNumber, companyNumber)
            case (companyNumber, Some(ctReference)) =>
              storeCompanyNumberService.storeCompanyNumber(vatNumber, companyNumber, ctReference)
          }) map {
            case Right(StoreCompanyNumberSuccess) => NoContent
            case Left(CompanyNumberDatabaseFailureNoVATNumber) => NotFound
            case Left(CompanyNumberDatabaseFailure) => InternalServerError
            case Left(CtReferenceMismatch) => BadRequest(Json.obj(HttpCodeKey -> CtReferenceMismatchCode))
            case Left(MatchCtReferenceFailure) => BadGateway
          }

        }
    }

}

object StoreCompanyNumberController {
  val CtReferenceMismatchCode = "CtReferenceMismatch"

  object StoreCompanyNumberReader extends Reads[(String, Option[String])] {
    override def reads(json: JsValue): JsResult[(String, Option[String])] = for {
      companyNumber <- (json \ companyNumberKey).validate[String]
      optCtReference <- (json \ ctReferenceKey).validateOpt[String]
    } yield (companyNumber, optCtReference)
  }

}
