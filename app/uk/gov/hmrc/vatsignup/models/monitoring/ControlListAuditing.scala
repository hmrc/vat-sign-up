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

package uk.gov.hmrc.vatsignup.models.monitoring

import uk.gov.hmrc.vatsignup.httpparsers.KnownFactsAndControlListInformationHttpParser._
import uk.gov.hmrc.vatsignup.models.controllist.ControlListInformation._
import uk.gov.hmrc.vatsignup.models.controllist._
import uk.gov.hmrc.vatsignup.services.monitoring.AuditModel

object ControlListAuditing {
  val controlListTransactionName = "VATControlListRequest"
  val controlListAuditType = "mtdVatControlList"

  val invalidVatNumber = "Invalid VAT number"
  val vatNumberNotFound = "VAT number not found"
  val unexpectedError = "Unexpected error"

  val directDebitMigrationRestrictionMessage = "Sign up restricted by Direct Debit migration timeframe"

  val filingDateMigrationRestrictionMessage = "Sign up restricted by Filing Date migration timeframe"

  case class ControlListAuditModel(vatNumber: String,
                                   isSuccess: Boolean,
                                   failureReasons: Seq[String] = Nil,
                                   nonMigratableReasons: Seq[String] = Nil
                                  ) extends AuditModel {
    override val transactionName: String = controlListTransactionName
    override val detail: Map[String, String] = Map(
      "vatNumber" -> vatNumber,
      "isSuccess" -> s"$isSuccess"
    ) ++ (failureReasons match {
      case Nil => None
      case reasons => Some("failureReasons" -> reasons.mkString(", "))
    }) ++ (nonMigratableReasons match {
      case Nil => None
      case reasons => Some("nonMigratableReasons" -> reasons.mkString(", "))
    })

    override val auditType: String = controlListAuditType
  }

  object ControlListAuditModel {
    def fromFailure(vatNumber: String, failure: KnownFactsAndControlListInformationFailure): ControlListAuditModel = {
      val failureMessage = failure match {
        case KnownFactsInvalidVatNumber => invalidVatNumber
        case ControlListInformationVatNumberNotFound => vatNumberNotFound
        case UnexpectedKnownFactsAndControlListInformationFailure(status, body) => s"""$unexpectedError {"status":"$status","body":"$body"}"""
      }

      ControlListAuditModel(
        vatNumber = vatNumber,
        isSuccess = false,
        failureReasons = Seq(failureMessage)
      )
    }

    def fromEligibilityState(vatNumber: String, controlListEligibility: ControlListInformation.Eligible): ControlListAuditModel = {
      controlListEligibility match {
        case Migratable =>
          ControlListAuditModel(vatNumber, isSuccess = true)
        case NonMigratable(nonMigratableReasons) =>
          ControlListAuditModel(vatNumber, isSuccess = true, nonMigratableReasons = nonMigratableReasons map (_.toString))
      }
    }

    def directDebitMigrationRestriction(vatNumber: String): ControlListAuditModel = {
      ControlListAuditModel(vatNumber, isSuccess = false, failureReasons = Seq(directDebitMigrationRestrictionMessage))
    }
    def filingDateMigrationRestriction(vatNumber: String): ControlListAuditModel = {
      ControlListAuditModel(vatNumber, isSuccess = false, failureReasons = Seq(filingDateMigrationRestrictionMessage))
    }

    def fromEligibilityState(vatNumber: String, controlListEligibility: ControlListInformation.Ineligible): ControlListAuditModel = {
      ControlListAuditModel(vatNumber, isSuccess = false, failureReasons = controlListEligibility.reasons map (_.toString))
    }
  }

}
