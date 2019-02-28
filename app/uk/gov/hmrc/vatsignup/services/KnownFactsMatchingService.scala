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

import javax.inject._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.vatsignup.config.featureswitch.{AdditionalKnownFacts, FeatureSwitching}
import uk.gov.hmrc.vatsignup.connectors.KnownFactsAndControlListInformationConnector
import uk.gov.hmrc.vatsignup.httpparsers.KnownFactsAndControlListInformationHttpParser._
import uk.gov.hmrc.vatsignup.models.{KnownFactsAndControlListInformation, VatKnownFacts}
import uk.gov.hmrc.vatsignup.services.KnownFactsMatchingService._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class KnownFactsMatchingService @Inject()(knownFactsAndControlListConnector: KnownFactsAndControlListInformationConnector
                                         )(implicit ec: ExecutionContext, hc: HeaderCarrier) extends FeatureSwitching {

  type KnownFactsMatchingResponse = Either[KnownFactsMatchingFailure, KnownFactsMatchingSuccess]

  def checkVatKnownFactsMatch(vatNumber: String,
                              vatRegistrationDate: String,
                              businessPostcode: String,
                              lastNetDue: Option[String],
                              lastReturnMonthPeriod: Option[String]
                             ): Future[KnownFactsMatchingResponse] = {

    val enteredKFs = VatKnownFacts(
      businessPostcode,
      vatRegistrationDate,
      lastReturnMonthPeriod,
      lastNetDue
    )

    knownFactsAndControlListConnector.getKnownFactsAndControlListInformation(vatNumber) map {
      case Right(retrievedKFs) if knownFactsMatch(enteredKFs, retrievedKFs) =>
        Right(KnownFactsMatch)
      case Right(_) =>
        Left(KnownFactsDoNotMatch)
      case Left(KnownFactsInvalidVatNumber) =>
        Left(InvalidVatNumber)
      case Left(ControlListInformationVatNumberNotFound) =>
        Left(VatNumberNotFound)
      case Left(UnexpectedKnownFactsAndControlListInformationFailure(status, body)) =>
        Left(UnexpectedError(status, body))
    }
  }

  private def knownFactsMatch(enteredKFs: VatKnownFacts,
                              retrievedKfs: KnownFactsAndControlListInformation): Boolean = {

    val baseKnownFactsValid = enteredKFs.vatRegistrationDate == retrievedKfs.vatKnownFacts.vatRegistrationDate &&
      enteredKFs.businessPostcode == retrievedKfs.vatKnownFacts.businessPostcode

    (enteredKFs.lastNetDue, enteredKFs.lastReturnMonthPeriod) match {
      case (Some(lastNetDue) ,Some(lastReturnMonthPeriod)) =>
        if (isEnabled(AdditionalKnownFacts)) {
          baseKnownFactsValid && retrievedKfs.vatKnownFacts.lastNetDue.contains(lastNetDue) &&
            retrievedKfs.vatKnownFacts.lastReturnMonthPeriod.contains(lastReturnMonthPeriod)
        }
        else baseKnownFactsValid
      case _ => baseKnownFactsValid
    }
  }

}

object KnownFactsMatchingService {
  sealed trait KnownFactsMatchingSuccess

  case object KnownFactsMatch extends KnownFactsMatchingSuccess

  sealed trait KnownFactsMatchingFailure

  case object KnownFactsDoNotMatch extends KnownFactsMatchingFailure

  case object InvalidVatNumber extends KnownFactsMatchingFailure

  case object VatNumberNotFound extends KnownFactsMatchingFailure

  case class UnexpectedError(status: Int, body: String) extends KnownFactsMatchingFailure
}


