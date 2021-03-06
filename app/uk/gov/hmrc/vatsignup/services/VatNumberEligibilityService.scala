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

package uk.gov.hmrc.vatsignup.services

import javax.inject.{Inject, Singleton}
import play.api.mvc.Request
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}
import uk.gov.hmrc.vatsignup.connectors.{MandationStatusConnector, VatCustomerDetailsConnector}
import uk.gov.hmrc.vatsignup.httpparsers.{GetMandationStatusHttpParser, VatCustomerDetailsHttpParser}
import uk.gov.hmrc.vatsignup.models._
import uk.gov.hmrc.vatsignup.services.ControlListEligibilityService.{EligibilitySuccess, IneligibleVatNumber}
import uk.gov.hmrc.vatsignup.services.VatNumberEligibilityService._

import java.time.LocalDate
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class VatNumberEligibilityService @Inject()(mandationStatusConnector: MandationStatusConnector,
                                            controlListEligibilityService: ControlListEligibilityService,
                                            vatCustomerDetailsConnector: VatCustomerDetailsConnector
                                           )(implicit ec: ExecutionContext) {
  //noinspection ScalaStyle
  def getMtdStatus(vatNumber: String)(implicit hc: HeaderCarrier, req: Request[_]): Future[MtdState] =
    mandationStatusConnector.getMandationStatus(vatNumber) flatMap {
      case Right(mandationStatus) =>
        vatCustomerDetailsConnector.getVatCustomerDetails(vatNumber) map {
          case Left(VatCustomerDetailsHttpParser.Deregistered) =>
            Deregistered
          case Right(VatCustomerDetails(knownFacts, isOverseas)) =>
            if (List(NonMTDfB, NonDigital, MTDfBExempt).contains(mandationStatus))
              Eligible(
                migrated = true,
                overseas = isOverseas,
                isNew = LocalDate.parse(knownFacts.vatRegistrationDate).isAfter(LocalDate.now().minusDays(7))
              )
            else
              AlreadySubscribed(isOverseas)
        }
      case Left(GetMandationStatusHttpParser.VatNumberNotFound) =>
        controlListEligibilityService.getEligibilityStatus(vatNumber) map {
          case Right(eligibilitySuccess: EligibilitySuccess) =>
            Eligible(
              migrated = false,
              overseas = eligibilitySuccess.isOverseas,
              isNew = LocalDate.parse(eligibilitySuccess.vatKnownFacts.vatRegistrationDate).isAfter(LocalDate.now().minusDays(7))
            )
          case Left(IneligibleVatNumber(MigratableDates.empty)) =>
            Ineligible
          case Left(IneligibleVatNumber(migratableDates)) =>
            Inhibited(migratableDates)
          case Left(ControlListEligibilityService.Deregistered) =>
            Deregistered
          case Left(ControlListEligibilityService.VatNumberNotFound) =>
            VatNumberNotFound
          case Left(error) =>
            throw new InternalServerException(s"Could not retrieve control list for VRN $vatNumber: $error")
        }
      case Left(GetMandationStatusHttpParser.MigrationInProgress) =>
        Future.successful(MigrationInProgress)
      case Left(error) =>
        throw new InternalServerException(s"Could not retrieve mandation status: $error")
    }
}

object VatNumberEligibilityService {

  sealed trait MtdState

  case class AlreadySubscribed(isOverseas: Boolean) extends MtdState

  case class Eligible(migrated: Boolean, overseas: Boolean, isNew: Boolean) extends MtdState

  case object Ineligible extends MtdState

  case class Inhibited(migratableDates: MigratableDates) extends MtdState

  case object MigrationInProgress extends MtdState

  case object VatNumberNotFound extends MtdState

  case object Deregistered extends MtdState

}
