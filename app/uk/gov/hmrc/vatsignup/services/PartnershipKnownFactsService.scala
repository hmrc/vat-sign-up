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
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.vatsignup.connectors.PartnershipKnownFactsConnector
import uk.gov.hmrc.vatsignup.httpparsers.GetPartnershipKnownFactsHttpParser.{PartnershipKnownFactsNotFound, UnexpectedGetPartnershipKnownFactsFailure}
import uk.gov.hmrc.vatsignup.services.PartnershipKnownFactsService._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PartnershipKnownFactsService @Inject()(partnershipKnownFactsConnector: PartnershipKnownFactsConnector)(
                                             implicit ec: ExecutionContext
                                            ) {
  def getMatchedKnownFacts(saUtr: String, postCode: String)(
                           implicit hc: HeaderCarrier
                          ): Future[PartnershipPostCodeResponse] =
    partnershipKnownFactsConnector.getPartnershipKnownFacts(saUtr) map {
      case Right(knownFacts) =>
        val sanitisedStoredPostCodes: List[String] = knownFacts.iterator.toList.collect {
          case postCode => (postCode filterNot { _.isWhitespace }).toUpperCase
        }
        val sanitisedPostCode = (postCode filterNot { _.isWhitespace }).toUpperCase
        if(sanitisedStoredPostCodes.contains(sanitisedPostCode))
          Right(PartnershipPostCodeMatched)
        else if(sanitisedStoredPostCodes.isEmpty)
          Left(NoPostCodesReturned)
        else
          Left(PostCodeDoesNotMatch)
      case Left(PartnershipKnownFactsNotFound) =>
        Left(InvalidSautr)
      case Left(UnexpectedGetPartnershipKnownFactsFailure(status, body)) =>
        Left(GetPartnershipKnownFactsFailure(status, body))
    }

}

object PartnershipKnownFactsService {

  type PartnershipPostCodeResponse = Either[PartnershipPostCodeMatchFailure, PartnershipPostCodeMatched.type]

  case object PartnershipPostCodeMatched

  sealed trait PartnershipPostCodeMatchFailure

  case object PostCodeDoesNotMatch extends PartnershipPostCodeMatchFailure

  case object NoPostCodesReturned extends PartnershipPostCodeMatchFailure

  case object InvalidSautr extends PartnershipPostCodeMatchFailure

  case class GetPartnershipKnownFactsFailure(status: Int, body: String) extends PartnershipPostCodeMatchFailure

}
