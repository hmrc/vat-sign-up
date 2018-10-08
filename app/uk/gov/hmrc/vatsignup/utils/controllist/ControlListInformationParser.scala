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

package uk.gov.hmrc.vatsignup.utils.controllist

import uk.gov.hmrc.vatsignup.models.controllist._

object ControlListInformationParser {

  private val CONTROL_LIST_TRUE = '0'
  private val CONTROL_LIST_FALSE = '1'

  def tryParse(controlList: String): Either[ControlListParseError, ControlListInformation] = {
    val parameters = ControlListParameter.getParameterMap
    if (controlList matches "[0,1]{32}") {
      val unsanitisedSet: Set[ControlListParameter] = (controlList.zipWithIndex flatMap {
        case (CONTROL_LIST_TRUE, index) => parameters.get(index)
        case (CONTROL_LIST_FALSE, _) => None
      }).toSet
      val sanitisedSet: Set[ControlListParameter] =
        if (unsanitisedSet.exists(x => x.isInstanceOf[Stagger] && !x.isInstanceOf[NonStandardTaxPeriod.type])) {
          unsanitisedSet - NonStandardTaxPeriod
        } else {
          unsanitisedSet
        }
      if ((sanitisedSet count (_.isInstanceOf[Stagger])) != 1) {
        Left(StaggerConflict)
      } else if ((sanitisedSet count (_.isInstanceOf[BusinessEntity])) != 1) {
        Left(EntityConflict)
      } else {
        Right(ControlListInformation(sanitisedSet))
      }
    } else {
      Left(InvalidFormat)
    }
  }

  sealed trait ControlListParseError

  case object InvalidFormat extends ControlListParseError

  case object EntityConflict extends ControlListParseError

  case object StaggerConflict extends ControlListParseError

}
