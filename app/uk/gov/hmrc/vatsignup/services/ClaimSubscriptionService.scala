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

import cats.data.EitherT
import cats.implicits._
import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.auth.core.authorise.EmptyPredicate
import uk.gov.hmrc.auth.core.retrieve.Retrievals.{credentials, groupIdentifier}
import uk.gov.hmrc.auth.core.retrieve.{Credentials, ~}
import uk.gov.hmrc.http.{ForbiddenException, HeaderCarrier}
import uk.gov.hmrc.vatsignup.connectors.{KnownFactsConnector, TaxEnrolmentsConnector}
import uk.gov.hmrc.vatsignup.httpparsers.AllocateEnrolmentResponseHttpParser.EnrolSuccess
import uk.gov.hmrc.vatsignup.httpparsers.KnownFactsHttpParser
import uk.gov.hmrc.vatsignup.httpparsers.KnownFactsHttpParser.KnownFacts
import uk.gov.hmrc.vatsignup.services.ClaimSubscriptionService._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ClaimSubscriptionService @Inject()(authConnector: AuthConnector,
                                         knownFactsConnector: KnownFactsConnector,
                                         taxEnrolmentsConnector: TaxEnrolmentsConnector
                                        )(implicit ec: ExecutionContext) {

  def claimSubscription(vatNumber: String)(implicit hc: HeaderCarrier): Future[ClaimSubscriptionResponse] = {
    for {
      knownFacts <- getKnownFacts(vatNumber)
      _ <- allocateEnrolment(vatNumber, knownFacts)
    } yield SubscriptionClaimed
  }.value

  private def getKnownFacts(vatNumber: String)(implicit hc: HeaderCarrier): EitherT[Future, ClaimSubscriptionFailure, KnownFacts] =
    EitherT(knownFactsConnector.getKnownFacts(vatNumber)) leftMap {
      case KnownFactsHttpParser.InvalidVatNumber => InvalidVatNumber
      case KnownFactsHttpParser.VatNumberNotFound => VatNumberNotFound
      case err => KnownFactsFailure
    }

  private def allocateEnrolment(vatNumber: String,
                                knownFacts: KnownFacts)(implicit hc: HeaderCarrier): EitherT[Future, ClaimSubscriptionFailure, EnrolSuccess.type] =
    EitherT.right(authConnector.authorise(EmptyPredicate, credentials and groupIdentifier)) flatMap {
      case Credentials(credentialId, GGProviderId) ~ Some(groupId) =>
        EitherT(taxEnrolmentsConnector.allocateEnrolment(
          groupId = groupId,
          credentialId = credentialId,
          vatNumber = vatNumber,
          postcode = knownFacts.businessPostcode,
          vatRegistrationDate = knownFacts.vatRegistrationDate
        )) leftMap {
          _ => EnrolFailure
        }
      case _ =>
        EitherT.liftF(Future.failed(new ForbiddenException("Invalid auth credentials")))
    }
}

object ClaimSubscriptionService {
  val GGProviderId = "GovernmentGateway"

  type ClaimSubscriptionResponse = Either[ClaimSubscriptionFailure, SubscriptionClaimed.type]

  case object SubscriptionClaimed

  sealed trait ClaimSubscriptionFailure

  case object VatNumberNotFound extends ClaimSubscriptionFailure

  case object InvalidVatNumber extends ClaimSubscriptionFailure

  case object KnownFactsFailure extends ClaimSubscriptionFailure

  case object EnrolFailure extends ClaimSubscriptionFailure

}
