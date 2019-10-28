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

package uk.gov.hmrc.vatsignup.services

import javax.inject.{Inject, Singleton}
import play.api.mvc.Request
import uk.gov.hmrc.auth.core.Enrolments
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.vatsignup.models.VatKnownFacts
import uk.gov.hmrc.vatsignup.repositories.SubscriptionRequestRepository
import uk.gov.hmrc.vatsignup.services.StoreMigratedVRNService._
import uk.gov.hmrc.vatsignup.utils.EnrolmentUtils._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class StoreMigratedVRNService @Inject()(subscriptionRequestRepository: SubscriptionRequestRepository,
                                        migratedKnownFactsMatchingService: MigratedKnownFactsMatchingService
                                       )(implicit ec: ExecutionContext) {

  def storeVatNumber(vatNumber: String,
                     enrolments: Enrolments,
                     optKnownFacts: Option[VatKnownFacts] = None)
                    (implicit hc: HeaderCarrier,
                     request: Request[_]
                    ): Future[Either[StoreMigratedVRNFailure, StoreMigratedVRNSuccess.type]] = {

    def upsertVatNumber(vatNumber: String): Future[Either[StoreMigratedVRNFailure, StoreMigratedVRNSuccess.type]] =
      subscriptionRequestRepository.upsertVatNumber(vatNumber, isMigratable = true, isDirectDebit = false) map {
        case result if result.ok => Right(StoreMigratedVRNSuccess)
        case _ => Left(UpsertMigratedVRNFailure)
      }

    optKnownFacts match {
      case Some(knownFacts) =>
        migratedKnownFactsMatchingService.checkKnownFactsMatch(vatNumber, knownFacts).flatMap { knownFactsMatch =>
          if (knownFactsMatch) upsertVatNumber(vatNumber)
          else Future.successful(Left(DoesNotMatch))
        }

      case _ =>
        enrolments.vatNumber match {
          case Right(vrn) if (vatNumber == vrn) =>
            upsertVatNumber(vrn)
          case Right(_) =>
            Future.successful(Left(DoesNotMatch))
          case Left(VatNumberMismatch) =>
            Future.successful(Left(DoesNotMatch))
          case Left(NoEnrolment) =>
            Future.successful(Left(NoVatEnrolment))
        }
    }
  }
}

object StoreMigratedVRNService {

  case object StoreMigratedVRNSuccess

  trait StoreMigratedVRNFailure

  case object DoesNotMatch extends StoreMigratedVRNFailure

  case object NoVatEnrolment extends StoreMigratedVRNFailure

  case object UpsertMigratedVRNFailure extends StoreMigratedVRNFailure

}
