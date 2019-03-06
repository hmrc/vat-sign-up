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

package uk.gov.hmrc.vatsignup.models

import java.time.Month
import java.time.format.DateTimeFormatter
import java.util.Locale

import uk.gov.hmrc.vatsignup.models.controllist.ControlListInformation

case class KnownFactsAndControlListInformation(vatKnownFacts: VatKnownFacts,
                                               controlListInformation: ControlListInformation)

case class VatKnownFacts(businessPostcode: Option[String],
                         vatRegistrationDate: String,
                         lastReturnMonthPeriod: Option[Month],
                         lastNetDue: Option[String])

object VatKnownFacts {
  def fromMMM(monthString: String): Month = {
    val temporalAccessor = DateTimeFormatter
      .ofPattern("MMM")
      .withLocale(Locale.ENGLISH)
      .parse(monthString.toLowerCase.capitalize)

    Month.from(temporalAccessor)
  }

  def fromDisplayName(monthString: String): Month = {
    val temporalAccessor = DateTimeFormatter
        .ofPattern("MMMM")
      .withLocale(Locale.ENGLISH)
      .parse(monthString)

    Month.from(temporalAccessor)
  }
}
