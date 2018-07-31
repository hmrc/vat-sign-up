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

package uk.gov.hmrc.vatsignup.models.controllist

import uk.gov.hmrc.vatsignup.config.EligibilityConfig


case class ControlListInformation(controlList: Set[ControlListParameter]) {

  import ControlListInformation._

  def validate(config: EligibilityConfig): Result = {
    config.ineligibleParameters.intersect(controlList).toSeq match {
      case Nil =>
        config.nonMigratableParameters.intersect(controlList).toSeq match {
          case Nil => Right(Migratable)
          case nonMigrationReasons => Right(NonMigratable(nonMigrationReasons))
        }
      case ineligibleReasons => Left(Ineligible(ineligibleReasons))
    }
  }

}

sealed trait Eligible

case object Migratable extends Eligible

case class NonMigratable(reasons: Seq[ControlListParameter]) extends Eligible

case class Ineligible(reasons: Seq[ControlListParameter])

object ControlListInformation {

  type Result = Either[Ineligible, Eligible]

  def apply(controlList: ControlListParameter*): ControlListInformation = new ControlListInformation(Set(controlList: _*))

}
