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
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.vatsignup.connectors.GetCtReferenceConnector
import uk.gov.hmrc.vatsignup.httpparsers.GetCtReferenceHttpParser
import uk.gov.hmrc.vatsignup.services.CtReferenceLookupService._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CtReferenceLookupService @Inject()(getCtReferenceConnector: GetCtReferenceConnector
                                        )(implicit ec: ExecutionContext) {

  def checkCtReferenceExists(companyNumber: String
                            )(implicit hc: HeaderCarrier, request: Request[_]): Future[CheckCtReferenceExistsServiceResponse] = {
    getCtReferenceConnector.getCtReference(companyNumber) map {
      case Right(_) => Right(CtReferenceIsFound)
      case Left(GetCtReferenceHttpParser.CtReferenceNotFound) => Left(CtReferenceNotFound)
      case _ => Left(CheckCtReferenceExistsServiceFailure)
    }
  }

}

object CtReferenceLookupService {
  type CheckCtReferenceExistsServiceResponse = Either[CheckCtReferenceExistsServiceFailure, CheckCtReferenceExistsServiceSuccess]

  sealed trait CheckCtReferenceExistsServiceSuccess

  case object CtReferenceIsFound extends CheckCtReferenceExistsServiceSuccess

  sealed trait CheckCtReferenceExistsServiceFailure

  case object CtReferenceNotFound extends CheckCtReferenceExistsServiceFailure

  case object CheckCtReferenceExistsServiceFailure extends CheckCtReferenceExistsServiceFailure

}
