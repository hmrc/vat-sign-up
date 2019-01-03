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

import play.api.libs.json.{Json, OFormat}


case class PartialSignUpRequest(requestId: String,
                                vatNumber: Option[String] = None,
                                companyNumber: Option[String] = None,
                                ctReference: Option[String] = None,
                                nino: Option[String] = None,
                                email: Option[String] = None,
                                transactionEmail: Option[String] = None
                               )

object PartialSignUpRequest {

  implicit val format: OFormat[PartialSignUpRequest] = Json.format[PartialSignUpRequest]

  def apply(subscriptionRequest: UnconfirmedSubscriptionRequest): PartialSignUpRequest =
    PartialSignUpRequest(
      requestId = subscriptionRequest.requestId,
      vatNumber = subscriptionRequest.vatNumber,
      companyNumber = subscriptionRequest.companyNumber,
      ctReference = subscriptionRequest.ctReference,
      nino = subscriptionRequest.nino,
      email = subscriptionRequest.email,
      transactionEmail = subscriptionRequest.transactionEmail
    )

}
