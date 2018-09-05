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

package uk.gov.hmrc.vatsignup.services

import javax.inject.{Inject, Singleton}

import uk.gov.hmrc.vatsignup.repositories.UnconfirmedSubscriptionRequestRepository
import uk.gov.hmrc.vatsignup.services.RequestIdService.{RequestIdDatabaseFailure, RequestIdSuccess}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RequestIdService @Inject()(subscriptionRequestRepository: UnconfirmedSubscriptionRequestRepository
                                )(implicit ec: ExecutionContext) {

  def getRequestIdByCredential(credentialId: String): Future[Either[RequestIdDatabaseFailure.type, RequestIdSuccess]] =
    subscriptionRequestRepository.getRequestIdByCredential(credentialId)
      .map(requestId => Right(RequestIdSuccess(requestId)))
      .recover {
        case _ => Left(RequestIdDatabaseFailure)
      }

}

object RequestIdService {

  case class RequestIdSuccess(requestId: String)

  case object RequestIdDatabaseFailure

}
