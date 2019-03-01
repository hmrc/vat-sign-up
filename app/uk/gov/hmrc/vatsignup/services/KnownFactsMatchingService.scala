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
import uk.gov.hmrc.vatsignup.config.featureswitch.{AdditionalKnownFacts, FeatureSwitching}
import uk.gov.hmrc.vatsignup.models.VatKnownFacts
import uk.gov.hmrc.vatsignup.services.KnownFactsMatchingService._


@Singleton
class KnownFactsMatchingService @Inject() extends FeatureSwitching {

  def checkKnownFactsMatch(enteredKfs: VatKnownFacts, retrievedKfs: VatKnownFacts): KnownFactsMatchingResponse = {

    val businessPostCodeMatch = enteredKfs.businessPostcode == retrievedKfs.businessPostcode
    val vatRegDateMatch = enteredKfs.vatRegistrationDate == retrievedKfs.vatRegistrationDate
    val lastNetDueMatch = enteredKfs.lastNetDue == retrievedKfs.lastNetDue
    val lastReturnMonthPeriodMatch = enteredKfs.lastReturnMonthPeriod == retrievedKfs.lastReturnMonthPeriod

    lazy val fourKFMatch: Boolean = (
      businessPostCodeMatch
        && vatRegDateMatch
        && lastNetDueMatch
        && lastReturnMonthPeriodMatch
    )
    lazy val twoKFMatch: Boolean = (
      businessPostCodeMatch
        && vatRegDateMatch
        && enteredKfs.lastReturnMonthPeriod.isEmpty
        && enteredKfs.lastNetDue.isEmpty
    )

    if (isEnabled(AdditionalKnownFacts))
      if (fourKFMatch)
        Right(FourKnownFactsMatch)
      else
        Left(KnownFactsDoNotMatch)
    else
      if (twoKFMatch)
        Right(TwoKnownFactsMatch)
      else
        Left(KnownFactsDoNotMatch)
  }

}

object KnownFactsMatchingService {

  type KnownFactsMatchingResponse = Either[KnownFactsMatchingFailure, KnownFactsMatchingSuccess]

  sealed trait KnownFactsMatchingSuccess

  case object FourKnownFactsMatch extends KnownFactsMatchingSuccess

  case object TwoKnownFactsMatch extends KnownFactsMatchingSuccess

  sealed trait KnownFactsMatchingFailure

  case object KnownFactsDoNotMatch extends KnownFactsMatchingFailure

}
