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
import uk.gov.hmrc.auth.core.retrieve.Retrievals
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisedFunctions}
import uk.gov.hmrc.play.bootstrap.controller.BaseController
import uk.gov.hmrc.vatsignup.httpparsers.IdentityVerificationHttpParser.IdentityVerified
import uk.gov.hmrc.vatsignup.services.IdentityVerificationOrchestrationWithRequestIdService
import uk.gov.hmrc.vatsignup.services.IdentityVerificationOrchestrationWithRequestIdService._
import scala.concurrent.ExecutionContext

@Singleton
class StoreIdentityVerificationOutcomeWithRequestIdController @Inject()(val authConnector: AuthConnector,
                                                                        identityVerificationOrchestrationWithRequestIdService: IdentityVerificationOrchestrationWithRequestIdService)
                                                                       (implicit ec: ExecutionContext)
  extends BaseController with AuthorisedFunctions {

  private val journeyLinkKey = "journeyLink"

  def storeIdentityVerificationOutcome(requestId: String): Action[String] =
    Action.async(parse.json((JsPath \ journeyLinkKey).read[String])) {
      implicit req =>
        authorised().retrieve(Retrievals.confidenceLevel) {
          confidenceLevel =>
            val journeyLink = req.body
            identityVerificationOrchestrationWithRequestIdService.updateIdentityVerificationState(requestId, journeyLink, confidenceLevel) map {
              case Right(IdentityVerified) => NoContent
              case Left(IdentityNotVerified) => Forbidden
              case Left(IdentityVerificationDatabaseFailure) => InternalServerError
              case Left(IdentityVerificationConnectionFailure) => BadGateway
            }
        }
    }
}
