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

package uk.gov.hmrc.vatsignup.models

import play.api.libs.json._
import uk.gov.hmrc.vatsignup.utils.JsonUtils._

sealed trait BusinessEntity

case class LimitedCompany(companyNumber: String) extends BusinessEntity

case class SoleTrader(nino: String) extends BusinessEntity

sealed trait PartnershipBusinessEntity extends BusinessEntity {
  val sautr: Option[String]
}

object PartnershipBusinessEntity {

  def copyWithoutSautr(partnershipBusinessEntity: PartnershipBusinessEntity): PartnershipBusinessEntity = {
    partnershipBusinessEntity match {
      case GeneralPartnership(_) => GeneralPartnership(None)
      case LimitedPartnership(_, companyNumber) => LimitedPartnership(None, companyNumber)
      case LimitedLiabilityPartnership(_, companyNumber) => LimitedLiabilityPartnership(None, companyNumber)
      case ScottishLimitedPartnership(_, companyNumber) => ScottishLimitedPartnership(None, companyNumber)
    }
  }

}

case class GeneralPartnership(sautr: Option[String]) extends BusinessEntity with PartnershipBusinessEntity

case class LimitedPartnership(sautr: Option[String], companyNumber: String) extends BusinessEntity with PartnershipBusinessEntity

case class LimitedLiabilityPartnership(sautr: Option[String], companyNumber: String) extends BusinessEntity with PartnershipBusinessEntity

case class ScottishLimitedPartnership(sautr: Option[String], companyNumber: String) extends BusinessEntity with PartnershipBusinessEntity

case object VatGroup extends BusinessEntity

case object AdministrativeDivision extends BusinessEntity

case object UnincorporatedAssociation extends BusinessEntity

case object Trust extends BusinessEntity

case class RegisteredSociety(companyNumber: String) extends BusinessEntity

case object Charity extends BusinessEntity

case object GovernmentOrganisation extends BusinessEntity

case object Overseas extends BusinessEntity

case object JointVenture extends BusinessEntity

case class OverseasWithUkEstablishment(companyNumber: String) extends BusinessEntity

object BusinessEntity {
  val EntityTypeKey = "entityType"
  val LimitedCompanyKey = "limitedCompany"
  val SoleTraderKey = "soleTrader"
  val GeneralPartnershipKey = "generalPartnership"
  val JointVentureKey = "jointVenture"
  val LimitedPartnershipKey = "limitedPartnership"
  val LimitedLiabilityPartnershipKey = "limitedLiabilityPartnershipKey"
  val ScottishLimitedPartnershipKey = "scottishLimitedPartnershipKey"
  val VatGroupKey = "vatGroup"
  val AdministrativeDivisionKey = "administrativeDivision"
  val UnincorporatedAssociationKey = "unincorporatedAssociation"
  val TrustKey = "trust"
  val RegisteredSocietyKey = "registeredSociety"
  val CharityKey = "charity"
  val OverseasWithUkEstablishmentKey = "nonUKCompanyWithUKEstablishment"
  val OverseasKey = "nonUKCompanyNoUKEstablishment"
  val GovernmentOrganisationKey = "governmentOrganisation"

  val NinoKey = "nino"
  val CompanyNumberKey = "companyNumber"
  val SautrKey = "sautr"

  implicit object BusinessEntityFormat extends OFormat[BusinessEntity] {
    override def writes(businessEntity: BusinessEntity): JsObject = businessEntity match {
      case LimitedCompany(companyNumber) =>
        Json.obj(
          EntityTypeKey -> LimitedCompanyKey,
          CompanyNumberKey -> companyNumber
        )
      case SoleTrader(nino) =>
        Json.obj(
          EntityTypeKey -> SoleTraderKey,
          NinoKey -> nino
        )
      case GeneralPartnership(sautr) => (
        Json.obj(
          EntityTypeKey -> GeneralPartnershipKey
        )
          + (SautrKey -> sautr)
        )
      case JointVenture =>
        Json.obj(
          EntityTypeKey -> JointVentureKey
        )
      case LimitedPartnership(sautr, companyNumber) => (
        Json.obj(
          EntityTypeKey -> LimitedPartnershipKey,
          CompanyNumberKey -> companyNumber
        )
          + (SautrKey -> sautr)
        )
      case LimitedLiabilityPartnership(sautr, companyNumber) => (
        Json.obj(
          EntityTypeKey -> LimitedLiabilityPartnershipKey,
          CompanyNumberKey -> companyNumber
        )
          + (SautrKey -> sautr)
        )
      case ScottishLimitedPartnership(sautr, companyNumber) => (
        Json.obj(
          EntityTypeKey -> ScottishLimitedPartnershipKey,
          CompanyNumberKey -> companyNumber
        )
          + (SautrKey -> sautr)
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
      case OverseasWithUkEstablishment(companyNumber) =>
        Json.obj(
          EntityTypeKey -> OverseasWithUkEstablishmentKey,
          CompanyNumberKey -> companyNumber
        )
    }

    override def reads(json: JsValue): JsResult[BusinessEntity] =
      for {
        entityType <- (json \ EntityTypeKey).validate[String]
        businessEntity <- entityType match {
          case LimitedCompanyKey =>
            for {
              companyNumber <- (json \ CompanyNumberKey).validate[String]
            } yield LimitedCompany(companyNumber)
          case SoleTraderKey =>
            for {
              nino <- (json \ NinoKey).validate[String]
            } yield SoleTrader(nino)
          case GeneralPartnershipKey =>
            for {
              sautr <- (json \ SautrKey).validateOpt[String]
            } yield GeneralPartnership(sautr)
          case JointVentureKey =>
            JsSuccess(JointVenture)
          case LimitedPartnershipKey =>
            for {
              companyNumber <- (json \ CompanyNumberKey).validate[String]
              sautr <- (json \ SautrKey).validateOpt[String]
            } yield LimitedPartnership(sautr, companyNumber)
          case LimitedLiabilityPartnershipKey =>
            for {
              companyNumber <- (json \ CompanyNumberKey).validate[String]
              sautr <- (json \ SautrKey).validateOpt[String]
            } yield LimitedLiabilityPartnership(sautr, companyNumber)
          case ScottishLimitedPartnershipKey =>
            for {
              companyNumber <- (json \ CompanyNumberKey).validate[String]
              sautr <- (json \ SautrKey).validateOpt[String]
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
          case OverseasWithUkEstablishmentKey =>
            for {
              companyNumber <- (json \ CompanyNumberKey).validate[String]
            } yield OverseasWithUkEstablishment(companyNumber)
        }
      } yield businessEntity
  }

}
