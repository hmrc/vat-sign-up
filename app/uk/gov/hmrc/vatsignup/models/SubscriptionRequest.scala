/*
 * Copyright 2018 HM Revenue & Customs
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

import java.time.Instant

import play.api.libs.json.{Json, OFormat}

import NinoSource._

case class SubscriptionRequest(vatNumber: String,
                               companyNumber: Option[String] = None,
                               ctReference: Option[String] = None,
                               nino: Option[String] = None,
                               ninoSource: Option[NinoSource] = None,
                               partnershipEntity: Option[PartnershipEntityType] = None,
                               partnershipUtr: Option[String] = None,
                               email: Option[String] = None,
                               transactionEmail: Option[String] = None,
                               identityVerified: Boolean = false,
                               isMigratable: Boolean = true
                              )

object SubscriptionRequest {

  val vatNumberKey = "vatNumber"
  val postCodeKey = "postCode"
  val registrationDateKey = "registrationDate"
  val idKey = "_id"
  val companyNumberKey = "companyNumber"
  val ctReferenceKey = "ctReference"
  val ninoKey = "nino"
  val ninoSourceKey = "ninoSource"
  val entityTypeKey = "entityType"
  val partnershipUtrKey = "sautr"
  val emailKey = "email"
  val transactionEmailKey = "transactionEmail"
  val identityVerifiedKey = "identityVerified"
  val creationTimestampKey = "creationTimestamp"
  val isMigratableKey = "isMigratable"

  val mongoFormat: OFormat[SubscriptionRequest] = OFormat(
    json =>
      for {
        vatNumber <- (json \ idKey).validate[String]
        companyNumber <- (json \ companyNumberKey).validateOpt[String]
        ctReference <- (json \ ctReferenceKey).validateOpt[String]
        nino <- (json \ ninoKey).validateOpt[String]
        ninoSource <- (json \ ninoSourceKey).validateOpt[NinoSource].map { source =>
          (nino, source) match {
            case (Some(_), None) => Some(UserEntered)
            case (Some(_), Some(_)) => source
            case (_, _) => None
          }
        }
        partnershipEntityType <- (json \ entityTypeKey).validateOpt[PartnershipEntityType]
        partnershipUtr <- (json \ partnershipUtrKey).validateOpt[String]
        email <- (json \ emailKey).validateOpt[String]
        transactionEmail <- (json \ transactionEmailKey).validateOpt[String]
        identityVerified <- (json \ identityVerifiedKey).validate[Boolean]
        isMigratable <- (json \ isMigratableKey).validate[Boolean]
      } yield SubscriptionRequest(
        vatNumber = vatNumber,
        companyNumber = companyNumber,
        ctReference = ctReference,
        nino = nino,
        ninoSource = ninoSource,
        partnershipEntity = partnershipEntityType,
        partnershipUtr = partnershipUtr,
        email = email,
        transactionEmail = transactionEmail,
        identityVerified = identityVerified,
        isMigratable = isMigratable
      ),
    subscriptionRequest =>
      Json.obj(
        idKey -> subscriptionRequest.vatNumber,
        companyNumberKey -> subscriptionRequest.companyNumber,
        ninoKey -> subscriptionRequest.nino,
        ninoSourceKey -> subscriptionRequest.ninoSource,
        entityTypeKey -> subscriptionRequest.partnershipEntity,
        partnershipUtrKey -> subscriptionRequest.partnershipUtr,
        emailKey -> subscriptionRequest.email,
        transactionEmailKey -> subscriptionRequest.transactionEmail,
        identityVerifiedKey -> subscriptionRequest.identityVerified,
        creationTimestampKey -> Json.obj("$date" -> Instant.now.toEpochMilli),
        isMigratableKey -> subscriptionRequest.isMigratable
      ).++(
        subscriptionRequest.ctReference match {
          case Some(ref) => Json.obj(ctReferenceKey -> ref)
          case _ => Json.obj()
        }
      )
  )

}
