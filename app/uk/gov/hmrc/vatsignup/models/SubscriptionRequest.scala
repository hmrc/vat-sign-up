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

package uk.gov.hmrc.vatsignup.models

import java.time.Instant

import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.vatsignup.models.BusinessEntity.BusinessEntityFormat

case class SubscriptionRequest(vatNumber: String,
                               ctReference: Option[String] = None,
                               businessEntity: Option[BusinessEntity] = None,
                               email: Option[String] = None,
                               transactionEmail: Option[String] = None,
                               emailVerified: Option[Boolean] = None,
                               isMigratable: Boolean = true,
                               isDirectDebit: Boolean,
                               contactPreference: Option[ContactPreference]
                              )

object SubscriptionRequest {

  val vatNumberKey = "vatNumber"
  val postCodeKey = "postCode"
  val registrationDateKey = "registrationDate"
  val idKey = "_id"
  val companyNumberKey = "companyNumber"
  val ctReferenceKey = "ctReference"
  val ninoKey = "nino"
  val entityTypeKey = "entityType"
  val partnershipUtrKey = "sautr"
  val emailKey = "email"
  val transactionEmailKey = "transactionEmail"
  val emailVerifiedKey = "emailVerified"
  val creationTimestampKey = "creationTimestamp"
  val isMigratableKey = "isMigratable"
  val isDirectDebitKey = "isDirectDebit"
  val businessEntityKey = "businessEntity"
  val contactPreferenceKey = "contactPreference"

  val mongoFormat: OFormat[SubscriptionRequest] = OFormat(
    json =>
      for {
        vatNumber <- (json \ idKey).validate[String]
        ctReference <- (json \ ctReferenceKey).validateOpt[String]
        //Need to manually recover as validateOpt does not return None in the case of the fields not being set
        businessEntity <- json.validateOpt[BusinessEntity] recover { case _ => None }
        email <- (json \ emailKey).validateOpt[String]
        transactionEmail <- (json \ transactionEmailKey).validateOpt[String]
        emailVerified <- (json \ emailVerifiedKey).validateOpt[Boolean]
        isMigratable <- (json \ isMigratableKey).validate[Boolean]
        isDirectDebit <- (json \ isDirectDebitKey).validate[Boolean]
        contactPreference <- (json \ contactPreferenceKey).validateOpt[ContactPreference]
      } yield SubscriptionRequest(
        vatNumber = vatNumber,
        ctReference = ctReference,
        businessEntity = businessEntity,
        email = email,
        transactionEmail = transactionEmail,
        emailVerified = emailVerified,
        isMigratable = isMigratable,
        isDirectDebit = isDirectDebit,
        contactPreference = contactPreference
      ),
    subscriptionRequest =>
      Json.obj(
        idKey -> subscriptionRequest.vatNumber,
        emailKey -> subscriptionRequest.email,
        transactionEmailKey -> subscriptionRequest.transactionEmail,
        emailVerifiedKey -> subscriptionRequest.emailVerified,
        creationTimestampKey -> Json.obj("$date" -> Instant.now.toEpochMilli),
        isMigratableKey -> subscriptionRequest.isMigratable,
        isDirectDebitKey -> subscriptionRequest.isDirectDebit,
        contactPreferenceKey -> subscriptionRequest.contactPreference
      ).++(
        subscriptionRequest.ctReference match {
          case Some(ref) => Json.obj(ctReferenceKey -> ref)
          case _ => Json.obj()
        }
      ) ++ (subscriptionRequest.businessEntity match {
        case Some(businessEntity) => BusinessEntityFormat.writes(businessEntity)
        case None => Json.obj()
      })
  )

}
