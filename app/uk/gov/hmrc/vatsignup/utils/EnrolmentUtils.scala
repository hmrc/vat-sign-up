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

package uk.gov.hmrc.vatsignup.utils

import uk.gov.hmrc.auth.core.Enrolments
import uk.gov.hmrc.vatsignup.config.Constants.Des._
import uk.gov.hmrc.vatsignup.config.Constants.TaxEnrolments._
import uk.gov.hmrc.vatsignup.config.Constants._

object EnrolmentUtils {

  implicit class EnrolmentUtils(enrolments: Enrolments) {
    def vatNumber: Option[String] = {
      val vatDecEnrolment = enrolments getEnrolment VatDecEnrolmentKey flatMap {
        _.getIdentifier(VatReferenceKey)
      }
      val mtdEnrolment = enrolments getEnrolment MtdEnrolmentKey flatMap {
        _.getIdentifier(VrnKey)
      }
      val result = mtdEnrolment ++ vatDecEnrolment map (_.value)
      result.headOption
    }

    val agentReferenceNumber: Option[String] =
      enrolments getEnrolment AgentEnrolmentKey flatMap { agentEnrolment =>
        agentEnrolment getIdentifier AgentReferenceNumberKey map (_.value)
      }

    val partnershipUtr: Option[String] =
      enrolments getEnrolment PartnershipIrsaEnrolmentKey flatMap {
        partnershipEnrolment =>
          partnershipEnrolment
            .getIdentifier(PartnershipIrsaReferenceNumberKey)
            .map(_.value)
      }

    def isPrincipal: Boolean = agentReferenceNumber.isEmpty

    def isDelegated: Boolean = agentReferenceNumber.isDefined
  }

  def mtdVatEnrolmentKey(vatNumber: String): String = s"HMRC-MTD-VAT~VRN~$vatNumber"

  def legacyVatEnrolmentKey(vatNumber: String): String = s"HMCE-VATDEC-ORG~VATRegNo~$vatNumber"

}
