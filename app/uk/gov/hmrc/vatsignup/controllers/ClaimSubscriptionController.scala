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
import play.api.mvc.Action
import uk.gov.hmrc.auth.core.retrieve.Retrievals
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisedFunctions}
import uk.gov.hmrc.http.ForbiddenException
import uk.gov.hmrc.play.bootstrap.controller.BaseController
import uk.gov.hmrc.vatsignup.models.ClaimSubscriptionRequest
import uk.gov.hmrc.vatsignup.services.ClaimSubscriptionService
import uk.gov.hmrc.vatsignup.services.ClaimSubscriptionService._
import uk.gov.hmrc.vatsignup.utils.EnrolmentUtils._

import scala.concurrent.ExecutionContext

@Singleton
class ClaimSubscriptionController @Inject()(val authConnector: AuthConnector,
                                            claimSubscriptionService: ClaimSubscriptionService
                                           )(implicit ec: ExecutionContext)
  extends BaseController with AuthorisedFunctions {

  def claimSubscription(vatNumber: String): Action[ClaimSubscriptionRequest] =
    Action.async(parse.json[ClaimSubscriptionRequest]) { implicit req =>
      authorised().retrieve(Retrievals.allEnrolments) {
        enrolments =>
          ((enrolments.vatNumber, req.body) match {
            case (Right(enrolmentVatNumber), ClaimSubscriptionRequest(_, _, isFromBta)) if enrolmentVatNumber == vatNumber =>
              claimSubscriptionService.claimSubscriptionWithEnrolment(vatNumber, isFromBta)
            case (Left(_), ClaimSubscriptionRequest(Some(postcode), Some(registrationDate), isFromBta)) =>
              claimSubscriptionService.claimSubscription(vatNumber, Some(postcode), Some(registrationDate), isFromBta)
            case _ =>
              throw new ForbiddenException("Either matching legacy enrolment or known facts must be provided to claim enrolment.")
          }) map {
            case Right(SubscriptionClaimed) => NoContent
            case Left(KnownFactsMismatch) => Forbidden
            case Left(VatNumberNotFound | InvalidVatNumber) => BadRequest
            case Left(KnownFactsFailure | EnrolFailure) => BadGateway
            case Left(EnrolmentAlreadyAllocated) => Conflict
          }
      }
    }
}

