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

package uk.gov.hmrc.vatsignup.services

import javax.inject.{Inject, Singleton}
import play.api.mvc.Request
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}
import uk.gov.hmrc.vatsignup.connectors.MandationStatusConnector
import uk.gov.hmrc.vatsignup.httpparsers.GetMandationStatusHttpParser
import uk.gov.hmrc.vatsignup.httpparsers.GetMandationStatusHttpParser.VatNumberNotFound
import uk.gov.hmrc.vatsignup.models.{MTDfBMandated, MTDfBVoluntary, MigratableDates, NonDigital, NonMTDfB}
import uk.gov.hmrc.vatsignup.services.ControlListEligibilityService.{EligibilitySuccess, IneligibleVatNumber}
import uk.gov.hmrc.vatsignup.services.VatNumberEligibilityService._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class VatNumberEligibilityService @Inject()(mandationStatusConnector: MandationStatusConnector,
                                            controlListEligibilityService: ControlListEligibilityService
                                           )(implicit ec: ExecutionContext) {
  def getMtdStatus(vatNumber: String)(implicit hc: HeaderCarrier, req: Request[_]): Future[MtdState] =
    mandationStatusConnector.getMandationStatus(vatNumber) flatMap {
      case Right(MTDfBMandated | MTDfBVoluntary) =>
        Future.successful(AlreadySubscribed)
      case Right(NonMTDfB | NonDigital) =>
        Future.successful(
          Eligible(
            migrated = true,
            overseas = false //TODO Update when overseas story is played for opt-in
          )
        )
      case Left(VatNumberNotFound) =>
        controlListEligibilityService.getEligibilityStatus(vatNumber) map {
          case Right(eligibilitySuccess: EligibilitySuccess) =>
            Eligible(
              migrated = false,
              overseas = eligibilitySuccess.isOverseas
            )
          case Left(IneligibleVatNumber(MigratableDates.empty)) =>
            Ineligible
          case Left(IneligibleVatNumber(migratableDates)) =>
            Inhibited(migratableDates)
          case Left(error) =>
            throw new InternalServerException(s"Could not retrieve control list: $error")
        }
      case Left(GetMandationStatusHttpParser.MigrationInProgress) =>
        Future.successful(MigrationInProgress)
      case Left(error) =>
        throw new InternalServerException(s"Could not retrieve mandation status: $error")
    }
}

object VatNumberEligibilityService {

  sealed trait MtdState

  case object AlreadySubscribed extends MtdState

  case class Eligible(migrated: Boolean, overseas: Boolean) extends MtdState

  case object Ineligible extends MtdState

  case class Inhibited(migratableDates: MigratableDates) extends MtdState

  case object MigrationInProgress extends MtdState

}
