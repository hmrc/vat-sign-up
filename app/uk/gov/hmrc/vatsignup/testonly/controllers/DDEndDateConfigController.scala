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

package uk.gov.hmrc.vatsignup.testonly.controllers

import javax.inject.{Inject, Singleton}
import play.api.libs.json._
import play.api.mvc.ControllerComponents
import uk.gov.hmrc.play.bootstrap.controller.BackendController
import uk.gov.hmrc.vatsignup.config.EligibilityConfig
import uk.gov.hmrc.vatsignup.models.DateRange
import uk.gov.hmrc.vatsignup.models.controllist.Stagger

@Singleton
class DDEndDateConfigController @Inject()(eligibilityConfig: EligibilityConfig,
                                          cc: ControllerComponents
                                         ) extends BackendController(cc) {

  import DDEndDateConfigController._

  def getConfig = Action {
    request => Ok(Json.toJson(eligibilityConfig.directDebitStaggerParameters))
  }

}

object DDEndDateConfigController {

  implicit val writter: OWrites[Map[Stagger, Set[DateRange]]] =
    new OWrites[Map[Stagger, Set[DateRange]]] {
      override def writes(config: Map[Stagger, Set[DateRange]]): JsObject =
        Json.obj("config" ->
          config.map {
            case (stagger, set) =>
              Json.obj(
                stagger.getClass.getSimpleName.replace("$", "") ->
                  set.foldLeft(JsArray()) {
                    case (arr, el) => arr :+ Json.toJson(el)
                  }
              )
          }.foldLeft(JsArray.apply())(_ :+ _)
        )
    }

}

