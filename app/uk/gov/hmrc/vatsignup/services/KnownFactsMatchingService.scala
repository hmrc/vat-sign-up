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

import javax.inject._
import play.api.mvc.Request
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.vatsignup.config.featureswitch.FeatureSwitching
import uk.gov.hmrc.vatsignup.models.VatKnownFacts
import uk.gov.hmrc.vatsignup.models.monitoring.KnownFactsAuditing.KnownFactsAuditModel
import uk.gov.hmrc.vatsignup.services.KnownFactsMatchingService._
import uk.gov.hmrc.vatsignup.services.monitoring.AuditService

import scala.concurrent.ExecutionContext


@Singleton
class KnownFactsMatchingService @Inject()(auditService: AuditService)(implicit ec: ExecutionContext) extends FeatureSwitching {

  implicit class AbsoluteValue(optValue: Option[String]) {
    def absoluteValue: Option[String] = optValue map { value =>
      if (value.isEmpty) value
      else Math.abs(value.toDouble).toString
    }
  }

  def checkKnownFactsMatch(vatNumber: String,
                           enteredKfs: VatKnownFacts,
                           retrievedKfs: VatKnownFacts,
                           isOverseas: Boolean)(implicit hc: HeaderCarrier, req: Request[_]): KnownFactsMatchingResponse = {
    val businessPostcodeMatch = (enteredKfs.businessPostcode, retrievedKfs.businessPostcode) match {
      case (Some(enteredPostcode), Some(retrievedPostcode))
        if (enteredPostcode filterNot (_.isWhitespace)) equalsIgnoreCase (retrievedPostcode filterNot (_.isWhitespace)) =>
        true
      case (None, _) if isOverseas =>
        true
      case _ =>
        false
    }

    val vatRegDateMatch = enteredKfs.vatRegistrationDate == retrievedKfs.vatRegistrationDate
    val lastNetDueMatch = enteredKfs.lastNetDue.absoluteValue == retrievedKfs.lastNetDue.absoluteValue
    val lastReturnMonthPeriodMatch = enteredKfs.lastReturnMonthPeriod == retrievedKfs.lastReturnMonthPeriod

    if (businessPostcodeMatch && vatRegDateMatch && lastNetDueMatch && lastReturnMonthPeriodMatch) {
      auditService.audit(KnownFactsAuditModel(vatNumber, enteredKfs, retrievedKfs, matched = true))
      Right(KnownFactsMatch)
    }
    else {
      auditService.audit(KnownFactsAuditModel(vatNumber, enteredKfs, retrievedKfs, matched = false))
      Left(KnownFactsMismatch)
    }
  }
}

object KnownFactsMatchingService {

  type KnownFactsMatchingResponse = Either[KnownFactsMismatch.type, KnownFactsMatch.type]

  case object KnownFactsMatch

  case object KnownFactsMismatch

}
