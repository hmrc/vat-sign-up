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

package uk.gov.hmrc.vatsignup.services

import javax.inject.{Inject, Singleton}
import play.api.mvc.Request
import uk.gov.hmrc.auth.core.Enrolments
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.vatsignup.utils.EnrolmentUtils._
import uk.gov.hmrc.vatsignup.repositories.SubscriptionRequestRepository
import uk.gov.hmrc.vatsignup.services.MigratedSubmissionService.SubmissionSuccess
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class MigratedSubmissionService @Inject()(signUpRequestService: MigratedSignUpRequestService,
                                          registrationService: MigratedRegistrationService,
                                          migratedSignUpService: MigratedSignUpService,
                                          migratedEnrolmentService: MigratedEnrolmentService,
                                          subscriptionRequestRepository: SubscriptionRequestRepository)
                                         (implicit ec: ExecutionContext) {

  def submit(vatNumber: String, enrolments: Enrolments)
            (implicit hc: HeaderCarrier, request: Request[_]): Future[SubmissionSuccess.type] = {

    val optArn = enrolments.agentReferenceNumber

    for {
      signUpRequest <- signUpRequestService.getSignUpRequest(vatNumber, enrolments)
      safeId <- registrationService.registerBusinessEntity(vatNumber, signUpRequest.businessEntity, optArn)
      isPartialMigration = !signUpRequest.isMigratable
      _ <- migratedSignUpService.signUp(safeId, vatNumber, isPartialMigration, optArn)
      _ <- migratedEnrolmentService.enrolForMtd(vatNumber, safeId)
      _ <- signUpRequestService.deleteSignUpRequest(vatNumber)
    } yield SubmissionSuccess
  }

}

object MigratedSubmissionService {

  case object SubmissionSuccess

}
