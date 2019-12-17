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

import uk.gov.hmrc.vatsignup.connectors.utils.EtmpEntityKeys._
import uk.gov.hmrc.vatsignup.models._
import uk.gov.hmrc.vatsignup.services.monitoring.AuditModel

object RegisterWithMultipleIDsAuditing {
  val registerWithMultipleIDsTransactionName = "VATRegisterWithMultipleIDs"
  val registerWithMultipleIDsAuditType = "mtdVatRegisterWithMultipleIDs"

  case class RegisterWithMultipleIDsAuditModel(vatNumber: String,
                                               companyNumber: Option[String] = None,
                                               nino: Option[String] = None,
                                               sautr: Option[String] = None,
                                               businessEntity: String,
                                               agentReferenceNumber: Option[String],
                                               isSuccess: Boolean) extends AuditModel {

    override val transactionName: String = registerWithMultipleIDsTransactionName
    override val detail: Map[String, String] = Map(
      "vatNumber" -> Some(vatNumber),
      "companyNumber" -> companyNumber,
      "nino" -> nino,
      "agentReferenceNumber" -> agentReferenceNumber,
      "sautr" -> sautr,
      "businessEntity" -> Some(businessEntity),
      "isSuccess" -> Some(s"$isSuccess")
    ).collect { case (key, Some(value)) => key -> value }

    override val auditType: String = registerWithMultipleIDsAuditType
  }

  object RegisterWithMultipleIDsAuditModel {
    def apply(vatNumber: String,
              businessEntity: BusinessEntity,
              agentReferenceNumber: Option[String],
              isSuccess: Boolean): RegisterWithMultipleIDsAuditModel = {
      businessEntity match {
        case SoleTrader(nino) =>
          RegisterWithMultipleIDsAuditModel(
            vatNumber = vatNumber,
            nino = Some(nino),
            businessEntity = SoleTraderKey,
            agentReferenceNumber = agentReferenceNumber,
            isSuccess = isSuccess
          )
        case LimitedCompany(companyNumber) =>
          RegisterWithMultipleIDsAuditModel(
            vatNumber = vatNumber,
            companyNumber = Some(companyNumber),
            businessEntity = LimitedCompanyKey,
            agentReferenceNumber = agentReferenceNumber,
            isSuccess = isSuccess
          )
        case GeneralPartnership(sautr) =>
          RegisterWithMultipleIDsAuditModel(
            vatNumber = vatNumber,
            sautr = sautr,
            businessEntity = GeneralPartnershipKey,
            agentReferenceNumber = agentReferenceNumber,
            isSuccess = isSuccess
          )
        case LimitedPartnership(sautr, companyNumber) =>
          RegisterWithMultipleIDsAuditModel(
            vatNumber = vatNumber,
            sautr = sautr,
            companyNumber = Some(companyNumber),
            businessEntity = LimitedPartnershipKey,
            agentReferenceNumber = agentReferenceNumber,
            isSuccess = isSuccess
          )
        case LimitedLiabilityPartnership(sautr, companyNumber) =>
          RegisterWithMultipleIDsAuditModel(
            vatNumber = vatNumber,
            sautr = sautr,
            companyNumber = Some(companyNumber),
            businessEntity = LimitedLiabilityPartnershipKey,
            agentReferenceNumber = agentReferenceNumber,
            isSuccess = isSuccess
          )
        case ScottishLimitedPartnership(sautr, companyNumber) =>
          RegisterWithMultipleIDsAuditModel(
            vatNumber = vatNumber,
            sautr = sautr,
            companyNumber = Some(companyNumber),
            businessEntity = ScottishLimitedPartnershipKey,
            agentReferenceNumber = agentReferenceNumber,
            isSuccess = isSuccess
          )
        case VatGroup =>
          RegisterWithMultipleIDsAuditModel(
            vatNumber = vatNumber,
            sautr = None,
            companyNumber = None,
            businessEntity = VatGroupKey,
            agentReferenceNumber = agentReferenceNumber,
            isSuccess = isSuccess
          )
        case AdministrativeDivision =>
          RegisterWithMultipleIDsAuditModel(
            vatNumber = vatNumber,
            sautr = None,
            companyNumber = None,
            businessEntity = AdministrativeDivisionKey,
            agentReferenceNumber = agentReferenceNumber,
            isSuccess = isSuccess
          )
        case UnincorporatedAssociation =>
          RegisterWithMultipleIDsAuditModel(
            vatNumber = vatNumber,
            sautr = None,
            companyNumber = None,
            businessEntity = UnincorporatedAssociationKey,
            agentReferenceNumber = agentReferenceNumber,
            isSuccess = isSuccess
          )
        case Trust =>
          RegisterWithMultipleIDsAuditModel(
            vatNumber = vatNumber,
            sautr = None,
            companyNumber = None,
            businessEntity = TrustKey,
            agentReferenceNumber = agentReferenceNumber,
            isSuccess = isSuccess
          )
        case Charity =>
          RegisterWithMultipleIDsAuditModel(
            vatNumber = vatNumber,
            sautr = None,
            companyNumber = None,
            businessEntity = CharityKey,
            agentReferenceNumber = agentReferenceNumber,
            isSuccess = isSuccess
          )
        case GovernmentOrganisation =>
          RegisterWithMultipleIDsAuditModel(
            vatNumber = vatNumber,
            sautr = None,
            companyNumber = None,
            businessEntity = GovernmentOrganisationKey,
            agentReferenceNumber = agentReferenceNumber,
            isSuccess = isSuccess
          )
        case Overseas =>
          RegisterWithMultipleIDsAuditModel(
            vatNumber = vatNumber,
            sautr = None,
            companyNumber = None,
            businessEntity = OverseasKey,
            agentReferenceNumber = agentReferenceNumber,
            isSuccess = isSuccess
          )
        case RegisteredSociety(companyNumber) =>
          RegisterWithMultipleIDsAuditModel(
            vatNumber = vatNumber,
            companyNumber = Some(companyNumber),
            businessEntity = RegisteredSocietyKey,
            agentReferenceNumber = agentReferenceNumber,
            isSuccess = isSuccess
          )
        case OverseasWithUkEstablishment(companyNumber) =>
          RegisterWithMultipleIDsAuditModel(
            vatNumber = vatNumber,
            companyNumber = Some(companyNumber),
            businessEntity = OverseasWithUkEstablishmentKey,
            agentReferenceNumber = agentReferenceNumber,
            isSuccess = isSuccess
          )
      }
    }
  }

}