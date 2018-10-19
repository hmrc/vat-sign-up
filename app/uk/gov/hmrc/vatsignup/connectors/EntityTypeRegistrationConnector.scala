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

package uk.gov.hmrc.vatsignup.connectors

import javax.inject.{Inject, Singleton}

import play.api.libs.json.{JsObject, Json, Writes}
import uk.gov.hmrc.http.logging.Authorization
import uk.gov.hmrc.http.{HeaderCarrier, HttpReads}
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._
import uk.gov.hmrc.vatsignup.config.AppConfig
import uk.gov.hmrc.vatsignup.connectors.EntityTypeRegistrationConnector._
import uk.gov.hmrc.vatsignup.httpparsers.RegisterWithMultipleIdentifiersHttpParser._
import uk.gov.hmrc.vatsignup.models._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EntityTypeRegistrationConnector @Inject()(val http: HttpClient,
                                                val applicationConfig: AppConfig) {
  def registerBusinessEntity(vatNumber: String,
                             businessEntity: BusinessEntity
                            )(implicit hc: HeaderCarrier): Future[RegisterWithMultipleIdentifiersResponse] = {
    val headerCarrier = hc
      .withExtraHeaders(applicationConfig.desEnvironmentHeader)
      .copy(authorization = Some(Authorization(applicationConfig.desAuthorisationToken)))

    http.POST[JsObject, RegisterWithMultipleIdentifiersResponse](
      url = applicationConfig.registerWithMultipleIdentifiersUrl,
      body = toRegisterApiJson(businessEntity, vatNumber)
    )(
      implicitly[Writes[JsObject]],
      implicitly[HttpReads[RegisterWithMultipleIdentifiersResponse]],
      headerCarrier,
      implicitly[ExecutionContext]
    )
  }

}

object EntityTypeRegistrationConnector {
  val SoleTraderKey = "soleTrader"
  val LimitedCompanyKey = "company"
  val GeneralPartnershipKey = "ordinaryPartnership"
  val LimitedPartnershipKey = "limitedPartnership"
  val LimitedLiabilityPartnershipKey = "limitedLiabilityPartnership"
  val ScottishLimitedPartnershipKey = "scottishLimitedPartnership"

  val VrnKey = "vrn"
  val NinoKey = "nino"
  val CrnKey = "crn"
  val SautrKey = "sautr"

  def toRegisterApiJson(businessEntity: BusinessEntity, vatNumber: String): JsObject = businessEntity match {
    case SoleTrader(nino) =>
      Json.obj(
        SoleTraderKey -> Json.obj(
          VrnKey -> vatNumber,
          NinoKey -> nino
        )
      )
    case LimitedCompany(companyNumber) =>
      Json.obj(
        LimitedCompanyKey -> Json.obj(
          VrnKey -> vatNumber,
          CrnKey -> companyNumber
        )
      )
    case GeneralPartnership(sautr) =>
      Json.obj(
        GeneralPartnershipKey -> Json.obj(
          VrnKey -> vatNumber,
          SautrKey -> sautr
        )
      )
    case LimitedPartnership(sautr, companyNumber) =>
      Json.obj(
        LimitedPartnershipKey -> Json.obj(
          VrnKey -> vatNumber,
          SautrKey -> sautr,
          CrnKey -> companyNumber
        )
      )
    case LimitedLiabilityPartnership(sautr, companyNumber) =>
      Json.obj(
        LimitedLiabilityPartnershipKey -> Json.obj(
          VrnKey -> vatNumber,
          SautrKey -> sautr,
          CrnKey -> companyNumber
        )
      )
    case ScottishLimitedPartnership(sautr, companyNumber) =>
      Json.obj(
        ScottishLimitedPartnershipKey -> Json.obj(
          VrnKey -> vatNumber,
          SautrKey -> sautr,
          CrnKey -> companyNumber
        )
      )
  }

}
