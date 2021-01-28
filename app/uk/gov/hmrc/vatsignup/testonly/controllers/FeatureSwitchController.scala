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
import play.api.i18n.MessagesApi
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.play.bootstrap.controller.BackendController
import uk.gov.hmrc.vatsignup.config.featureswitch.FeatureSwitch.switches
import uk.gov.hmrc.vatsignup.config.featureswitch.{FeatureSwitch, FeatureSwitchSetting, FeatureSwitching}

@Singleton
class FeatureSwitchController @Inject()(override val messagesApi: MessagesApi, cc: ControllerComponents)
  extends BackendController(cc) with FeatureSwitching {

  private def returnCurrentSettings = {
    val featureSwitches = switches map (featureSwitch => FeatureSwitchSetting(featureSwitch, isEnabled(featureSwitch)))

    Ok(Json.toJson(featureSwitches))
  }

  lazy val get: Action[AnyContent] = Action {
    returnCurrentSettings
  }

  lazy val update: Action[List[FeatureSwitchSetting]] = Action(parse.json[List[FeatureSwitchSetting]]) { req =>
    req.body foreach { setting =>
      val featureSwitch = FeatureSwitch(setting)

      if (setting.enable) enable(featureSwitch)
      else disable(featureSwitch)
    }
    returnCurrentSettings
  }
}
