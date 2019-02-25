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
import play.api.http.Status.BAD_REQUEST
import play.api.mvc.Request
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.vatsignup.config.AppConfig
import uk.gov.hmrc.vatsignup.config.featureswitch.AdditionalKnownFacts
import uk.gov.hmrc.vatsignup.connectors.KnownFactsAndControlListInformationConnector
import uk.gov.hmrc.vatsignup.httpparsers.{KnownFactsAndControlListInformationHttpParser => KfHttpParser}
import uk.gov.hmrc.vatsignup.models.VatKnownFacts
import uk.gov.hmrc.vatsignup.services.KnownFactsAndControllistInformationService._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class KnownFactsAndControllistInformationService @Inject()(appConfig: AppConfig,
                                                           knownFactsAndControlListInformationConnector: KnownFactsAndControlListInformationConnector)
                                                          (implicit ec: ExecutionContext, request: Request[_]) {

  def getKnownFactsAndControlListInformation(
    vatNumber: String,
    businessPostcode: String,
    vatRegistrationDate: String,
    lastReturnMonthPeriod: Option[String],
    lastNetDue: Option[String]
  )(implicit ec: HeaderCarrier, request: Request[_]): Future[KnownFactsAndControlListInformationResponse] = {
    (lastReturnMonthPeriod, lastNetDue) match {
      case (Some(_), Some(_)) => knownFactsAndControlListInformationConnector.getKnownFactsAndControlListInformation(
        vatNumber
      ) map {
        case Right(kf: VatKnownFacts) =>
          val matching: Boolean = (
            (businessPostcode filterNot (_.isWhitespace))
              .equalsIgnoreCase(kf.businessPostcode filterNot (_.isWhitespace))
            && (vatRegistrationDate == kf.vatRegistrationDate)
            && (lastReturnMonthPeriod == kf.lastReturnMonthPeriod)
            && (lastNetDue == kf.lastNetDue)
          )
          if (matching) Right(kf) else Left(KnownFactsMatchingFailure)
        case Left(KfHttpParser.KnownFactsInvalidVatNumber) =>
          Left(KnownFactsInvalidVatNumber)
        case Left(KfHttpParser.ControlListInformationVatNumberNotFound) =>
          Left(ControlListInformationVatNumberNotFound)
        case Left(KfHttpParser.UnexpectedKnownFactsAndControlListInformationFailure(status, body)) =>
          Left(UnexpectedKnownFactsAndControlListInformationFailure(status, body))
      }
      case (None, None) =>
        knownFactsAndControlListInformationConnector.getKnownFactsAndControlListInformation(vatNumber) map {
          case Right(kf: VatKnownFacts) if appConfig.isEnabled(AdditionalKnownFacts) =>
            val isFiled: Boolean = (kf.lastNetDue.isDefined || kf.lastReturnMonthPeriod.isDefined)
            if (isFiled) Left(KnownFactsMatchingFailure)
            else {
              val matching: Boolean = (businessPostcode filterNot (_.isWhitespace))
                .equalsIgnoreCase(kf.businessPostcode filterNot (_.isWhitespace)
                ) && (vatRegistrationDate == kf.vatRegistrationDate)
              if (matching) Right(kf)
              else Left(KnownFactsMatchingFailure)
            }
          case  Right(kf: VatKnownFacts) =>
            val matching: Boolean = (
              (businessPostcode filterNot (_.isWhitespace))
                .equalsIgnoreCase(kf.businessPostcode filterNot (_.isWhitespace))
              && (vatRegistrationDate == kf.vatRegistrationDate)
            )
            if (matching) Right(kf)
            else Left(KnownFactsMatchingFailure)
          case Left(KfHttpParser.KnownFactsInvalidVatNumber) =>
            Left(KnownFactsInvalidVatNumber)
          case Left(KfHttpParser.ControlListInformationVatNumberNotFound) =>
            Left(ControlListInformationVatNumberNotFound)
          case Left(KfHttpParser.UnexpectedKnownFactsAndControlListInformationFailure(status, body)) =>
            Left(UnexpectedKnownFactsAndControlListInformationFailure(status, body))
        }
      case _ => Future.successful(
        Left(UnexpectedKnownFactsAndControlListInformationFailure(BAD_REQUEST, "Invalid known facts combination"))
      )
    }
  }
}

object KnownFactsAndControllistInformationService {

  type KnownFactsAndControlListInformationResponse = Either[KnownFactsAndControlListInformationFailure, VatKnownFacts]

  sealed trait KnownFactsAndControlListInformationFailure

  case object KnownFactsInvalidVatNumber extends KnownFactsAndControlListInformationFailure

  case object ControlListInformationVatNumberNotFound extends KnownFactsAndControlListInformationFailure

  case object KnownFactsMatchingFailure extends KnownFactsAndControlListInformationFailure

  case class UnexpectedKnownFactsAndControlListInformationFailure(status: Int, body: String) extends
    KnownFactsAndControlListInformationFailure

}
