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

import cats.data.EitherT
import cats.implicits._
import javax.inject.{Inject, Singleton}
import play.api.mvc.Request
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.vatsignup.models.RegisteredSociety
import uk.gov.hmrc.vatsignup.repositories.SubscriptionRequestRepository
import uk.gov.hmrc.vatsignup.services.CompanyMatchService.GetCtReferenceFailure
import uk.gov.hmrc.vatsignup.services.StoreRegisteredSocietyService._

import scala.concurrent.{ExecutionContext, Future}


@Singleton
class StoreRegisteredSocietyService @Inject()(subscriptionRequestRepository: SubscriptionRequestRepository,
                                              companyMatchService: CompanyMatchService)
                                             (implicit ec: ExecutionContext) {

  def storeRegisteredSociety(vatNumber: String,
                             companyNumber: String,
                             ctReference: Option[String])
                            (implicit hc: HeaderCarrier, request: Request[_]): Future[StoreRegisteredSocietyResponse[StoreRegisteredSocietySuccess.type]] =

    ctReference match {
      case None =>
        upsertBE(vatNumber, companyNumber)
      case Some(ctutr) => {
        for {
          _ <- EitherT(companyMatchService.checkCompanyMatch(companyNumber, ctutr)) leftMap {
            case CompanyMatchService.CtReferenceMismatch => CtReferenceMismatch
            case GetCtReferenceFailure => MatchCtReferenceFailure
          }
          _ <- EitherT(upsertBE(vatNumber, companyNumber))
          _ <- EitherT(upsertCT(vatNumber, ctutr))
        } yield StoreRegisteredSocietySuccess
      }.value
    }

  private def upsertBE(vatNumber: String,
                       companyNumber: String): Future[StoreRegisteredSocietyResponse[StoreRegisteredSocietySuccess.type]] =
    subscriptionRequestRepository.upsertBusinessEntity(vatNumber, RegisteredSociety(companyNumber)) map {
      _ => Right(StoreRegisteredSocietySuccess)
    } recover {
      case e: NoSuchElementException => Left(DatabaseFailureNoVATNumber)
      case _ => Left(RegisteredSocietyDatabaseFailure)
    }

  private def upsertCT(vatNumber: String,
                       ctReference: String): Future[StoreRegisteredSocietyResponse[StoreCtReferenceSuccess.type]] =
    subscriptionRequestRepository.upsertCtReference(vatNumber, ctReference) map {
      _ => Right(StoreCtReferenceSuccess)
    } recover {
      case e: NoSuchElementException => Left(DatabaseFailureNoVATNumber)
      case _ => Left(CtReferenceDatabaseFailure)
    }
}

object StoreRegisteredSocietyService {

  type StoreRegisteredSocietyResponse[A] = Either[StoreRegisteredSocietyFailure, A]

  case object StoreRegisteredSocietySuccess

  case object StoreCtReferenceSuccess

  sealed trait StoreRegisteredSocietyFailure

  case object RegisteredSocietyDatabaseFailure extends StoreRegisteredSocietyFailure

  case object CtReferenceDatabaseFailure extends StoreRegisteredSocietyFailure

  case object DatabaseFailureNoVATNumber extends StoreRegisteredSocietyFailure

  case object CtReferenceMismatch extends StoreRegisteredSocietyFailure

  case object MatchCtReferenceFailure extends StoreRegisteredSocietyFailure

}
