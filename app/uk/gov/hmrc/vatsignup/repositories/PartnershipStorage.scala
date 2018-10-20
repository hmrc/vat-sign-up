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

package uk.gov.hmrc.vatsignup.repositories

import play.api.libs.json.Json
import reactivemongo.api.commands.UpdateWriteResult
import reactivemongo.play.json._
import uk.gov.hmrc.vatsignup.models.SubscriptionRequest._
import uk.gov.hmrc.vatsignup.models.{PartnershipBusinessEntity, _}

import scala.concurrent.{ExecutionContext, Future}

trait PartnershipStorage {
  self: SubscriptionRequestRepository =>

  val GeneralPartnershipKey: String = "generalPartnership"
  val LimitedPartnershipKey: String = "limitedPartnership"
  val LimitedLiabilityPartnershipKey: String = "limitedLiabilityPartnership"
  val ScottishLimitedPartnershipKey: String = "scottishLimitedPartnership"

  private def getUpdateJson(partnership: PartnershipBusinessEntity) = partnership match {
    case GeneralPartnership(sautr) =>
      Json.obj(
        entityTypeKey -> GeneralPartnershipKey,
        partnershipUtrKey -> sautr
      )
    case LimitedPartnership(sautr, companyNumber) =>
      Json.obj(
        entityTypeKey -> LimitedPartnershipKey,
        partnershipUtrKey -> sautr,
        companyNumberKey -> companyNumber
      )
    case LimitedLiabilityPartnership(sautr, companyNumber) =>
      Json.obj(
        entityTypeKey -> LimitedLiabilityPartnershipKey,
        partnershipUtrKey -> sautr,
        companyNumberKey -> companyNumber
      )
    case ScottishLimitedPartnership(sautr, companyNumber) =>
      Json.obj(
        entityTypeKey -> ScottishLimitedPartnershipKey,
        partnershipUtrKey -> sautr,
        companyNumberKey -> companyNumber
      )
  }

  private def getUnsetJson(partnership: PartnershipBusinessEntity) = partnership match {
    case _: GeneralPartnership =>
      Json.obj(
        ninoKey -> "",
        ninoSourceKey -> "",
        companyNumberKey -> ""
      )
    case (_: LimitedPartnership) | (_: LimitedLiabilityPartnership) | (_: ScottishLimitedPartnership) =>
      Json.obj(
        ninoKey -> "",
        ninoSourceKey -> ""
      )
  }

  def upsertPartnership(vatNumber: String,
                        partnership: PartnershipBusinessEntity): Future[UpdateWriteResult] =
    collection.update(
      selector = Json.obj(idKey -> vatNumber),
      update = Json.obj(
        "$set" -> getUpdateJson(partnership),
        "$unset" -> getUnsetJson(partnership)
      ),
      upsert = false
    ) filter (_.n == 1)

}
