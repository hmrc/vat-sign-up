/*
 * Copyright 2021 HM Revenue & Customs
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

package uk.gov.hmrc.vatsignup.connectors.utils

object EtmpEntityKeys {

  val SoleTraderKey = "soleTrader"
  val LimitedCompanyKey = "company"
  val GeneralPartnershipKey = "ordinaryPartnership"
  val LimitedPartnershipKey = "limitedPartnership"
  val LimitedLiabilityPartnershipKey = "limitedLiabilityPartnership"
  val ScottishLimitedPartnershipKey = "scottishLimitedPartnership"
  val VatGroupKey = "vatGroup"
  val AdministrativeDivisionKey = "division"
  val UnincorporatedAssociationKey = "unincorporatedAssociation"
  val TrustKey = "trust"
  val RegisteredSocietyKey = "registeredSociety"
  val CharityKey = "charitableIncorporatedOrganisation"
  val GovernmentOrganisationKey = "publicBody"
  val OverseasKey = "nonUKCompanyNoUKEstablishment"
  val OverseasWithUkEstablishmentKey = "nonUKCompanyWithUKEstablishment"

}
