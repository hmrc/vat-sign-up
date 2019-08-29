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

package uk.gov.hmrc.vatsignup.utils.controllist

import uk.gov.hmrc.vatsignup.models.controllist._
import cats.implicits._

object ControlListInformationParser {

  private val CONTROL_LIST_TRUE = '0'
  private val CONTROL_LIST_FALSE = '1'

  def tryParse(controlList: String): Either[ControlListParseError, ControlListInformation] = {
    val parameters = ControlListParameter.getParameterMap
    if (controlList matches "[0,1]{33,34}") {
      val parameterSet: Set[ControlListParameter] = (controlList.zipWithIndex flatMap {
        case (CONTROL_LIST_TRUE, index) => parameters.get(index)
        case (CONTROL_LIST_FALSE, _) => None
      }).toSet
      for {
        staggerType <- getStaggerType(parameterSet)
        businessEntity <- getBusinessEntity(parameterSet)
      } yield ControlListInformation(parameterSet, staggerType, businessEntity)

    } else {
      Left(InvalidFormat)
    }
  }

  private def getStaggerType(unsanitisedControlList: Set[ControlListParameter]): Either[StaggerConflict.type, Stagger] = {
    val isNonStandardTaxPeriod = unsanitisedControlList contains NonStandardTaxPeriod
    val staggerSet = unsanitisedControlList collect {
      case staggerType: Stagger if staggerType != NonStandardTaxPeriod => staggerType
    }

    staggerSet.toList match {
      case singleStagger :: Nil =>
        Right(singleStagger)
      case Nil if isNonStandardTaxPeriod =>
        Right(NonStandardTaxPeriod)
      case _ =>
        Left(StaggerConflict)
    }
  }

  private def getBusinessEntity(unsanitisedControlList: Set[ControlListParameter]): Either[EntityConflict.type, BusinessEntity] = {
    val businessEntities = unsanitisedControlList collect {
      case businessEntity: BusinessEntity => businessEntity
    }

    businessEntities.toList match {
      case singleBusinessEntity :: Nil =>
        Right(singleBusinessEntity)
      case _ if businessEntities.equals(Set(Company, Group)) =>
        Right(Group)
      case _ =>
        Left(EntityConflict)
    }
  }

  sealed trait ControlListParseError

  case object InvalidFormat extends ControlListParseError

  case object EntityConflict extends ControlListParseError

  case object StaggerConflict extends ControlListParseError

}
