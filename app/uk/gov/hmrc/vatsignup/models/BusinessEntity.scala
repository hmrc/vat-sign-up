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

import play.api.libs.json._

sealed trait BusinessEntity

case class LimitedCompany(companyNumber: String) extends BusinessEntity

case class SoleTrader(nino: String) extends BusinessEntity

sealed trait PartnershipBusinessEntity extends BusinessEntity {
  val sautr: String
}

case class GeneralPartnership(sautr: String) extends BusinessEntity with PartnershipBusinessEntity

case class LimitedPartnership(sautr: String, companyNumber: String) extends BusinessEntity with PartnershipBusinessEntity

case class LimitedLiabilityPartnership(sautr: String, companyNumber: String) extends BusinessEntity with PartnershipBusinessEntity

case class ScottishLimitedPartnership(sautr: String, companyNumber: String) extends BusinessEntity with PartnershipBusinessEntity

case object VatGroup extends BusinessEntity

case object AdministrativeDivision extends BusinessEntity

case object UnincorporatedAssociation extends BusinessEntity

case object Trust extends BusinessEntity

case class RegisteredSociety(companyNumber: String) extends BusinessEntity

case object Charity extends BusinessEntity

case object GovernmentOrganisation extends BusinessEntity

case object Overseas extends BusinessEntity

case object JointVenture extends BusinessEntity

object BusinessEntity {
  val EntityTypeKey = "entityType"
  val LimitedCompanyKey = "limitedCompany"
  val SoleTraderKey = "soleTrader"
  val GeneralPartnershipKey = "generalPartnership"
  val LimitedPartnershipKey = "limitedPartnership"
  val LimitedLiabilityPartnershipKey = "limitedLiabilityPartnershipKey"
  val ScottishLimitedPartnershipKey = "scottishLimitedPartnershipKey"
  val VatGroupKey = "vatGroup"
  val AdministrativeDivisionKey = "administrativeDivision"
  val UnincorporatedAssociationKey = "unincorporatedAssociation"
  val TrustKey = "trust"
  val RegisteredSocietyKey = "registeredSociety"
  val CharityKey = "charity"
  val NonUkWithUKEstablishmentKey = "nonUKCompanyWithUKEstablishment"
  val OverseasKey = "nonUKCompanyNoUKEstablishment"
  val GovernmentOrganisationKey = "governmentOrganisation"

  val NinoKey = "nino"
  val CompanyNumberKey = "companyNumber"
  val SautrKey = "sautr"

  implicit object BusinessEntityFormat extends OFormat[BusinessEntity] {
    override def writes(businessEntity: BusinessEntity): JsObject = businessEntity match {
      case LimitedCompany(companyNumber) =>
        val companyBusinessEntity = limitedCompaniesBE(companyNumber).getOrElse(LimitedCompanyKey)
        Json.obj(
          EntityTypeKey -> companyBusinessEntity,
          CompanyNumberKey -> companyNumber
        )
      case SoleTrader(nino) =>
        Json.obj(
          EntityTypeKey -> SoleTraderKey,
          NinoKey -> nino
        )
      case GeneralPartnership(sautr) =>
        Json.obj(
          EntityTypeKey -> GeneralPartnershipKey,
          SautrKey -> sautr
        )
      case LimitedPartnership(sautr, companyNumber) =>
        Json.obj(
          EntityTypeKey -> LimitedPartnershipKey,
          SautrKey -> sautr,
          CompanyNumberKey -> companyNumber
        )
      case LimitedLiabilityPartnership(sautr, companyNumber) =>
        Json.obj(
          EntityTypeKey -> LimitedLiabilityPartnershipKey,
          SautrKey -> sautr,
          CompanyNumberKey -> companyNumber
        )
      case ScottishLimitedPartnership(sautr, companyNumber) =>
        Json.obj(
          EntityTypeKey -> ScottishLimitedPartnershipKey,
          SautrKey -> sautr,
          CompanyNumberKey -> companyNumber
        )
      case VatGroup =>
        Json.obj(
          EntityTypeKey -> VatGroupKey
        )
      case AdministrativeDivision =>
        Json.obj(
          EntityTypeKey -> AdministrativeDivisionKey
        )
      case UnincorporatedAssociation =>
        Json.obj(
          EntityTypeKey -> UnincorporatedAssociationKey
        )
      case Trust =>
        Json.obj(
          EntityTypeKey -> TrustKey
        )
      case RegisteredSociety(companyNumber) =>
        Json.obj(
          EntityTypeKey -> RegisteredSocietyKey,
          CompanyNumberKey -> companyNumber
        )
      case Charity =>
        Json.obj(
          EntityTypeKey -> CharityKey
        )
      case GovernmentOrganisation =>
        Json.obj(
          EntityTypeKey -> GovernmentOrganisationKey
        )
      case Overseas =>
        Json.obj(
          EntityTypeKey -> OverseasKey
        )
    }

    override def reads(json: JsValue): JsResult[BusinessEntity] =
      for {
        entityType <- (json \ EntityTypeKey).validate[String]
        businessEntity <- entityType match {
          case LimitedCompanyKey | NonUkWithUKEstablishmentKey =>
            for {
              companyNumber <- (json \ CompanyNumberKey).validate[String]
            } yield LimitedCompany(companyNumber)
          case SoleTraderKey =>
            for {
              nino <- (json \ NinoKey).validate[String]
            } yield SoleTrader(nino)
          case GeneralPartnershipKey =>
            for {
              sautr <- (json \ SautrKey).validate[String]
            } yield GeneralPartnership(sautr)
          case LimitedPartnershipKey =>
            for {
              companyNumber <- (json \ CompanyNumberKey).validate[String]
              sautr <- (json \ SautrKey).validate[String]
            } yield LimitedPartnership(sautr, companyNumber)
          case LimitedLiabilityPartnershipKey =>
            for {
              companyNumber <- (json \ CompanyNumberKey).validate[String]
              sautr <- (json \ SautrKey).validate[String]
            } yield LimitedLiabilityPartnership(sautr, companyNumber)
          case ScottishLimitedPartnershipKey =>
            for {
              companyNumber <- (json \ CompanyNumberKey).validate[String]
              sautr <- (json \ SautrKey).validate[String]
            } yield ScottishLimitedPartnership(sautr, companyNumber)
          case VatGroupKey =>
            JsSuccess(VatGroup)
          case AdministrativeDivisionKey =>
            JsSuccess(AdministrativeDivision)
          case UnincorporatedAssociationKey =>
            JsSuccess(UnincorporatedAssociation)
          case TrustKey =>
            JsSuccess(Trust)
          case RegisteredSocietyKey =>
            for {
              companyNumber <- (json \ CompanyNumberKey).validate[String]
            } yield RegisteredSociety(companyNumber)
          case CharityKey =>
            JsSuccess(Charity)
          case GovernmentOrganisationKey =>
            JsSuccess(GovernmentOrganisation)
          case OverseasKey =>
            JsSuccess(Overseas)
        }
      } yield businessEntity
  }

  def limitedCompaniesBE(companyNumber: String): Option[String] = {
    val isNonUkWithUkEstablishment = Seq("FC", "SF", "NF") filter {
      companyNumber.toUpperCase.startsWith(_)
    }
    if(isNonUkWithUkEstablishment.nonEmpty) Some(NonUkWithUKEstablishmentKey) else None
  }
}

