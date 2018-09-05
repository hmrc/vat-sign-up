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

package uk.gov.hmrc.vatsignup.config

import javax.inject.{Inject, Singleton}

import uk.gov.hmrc.vatsignup.models.controllist._


@Singleton
class EligibilityConfig @Inject()(appConfig: AppConfig) {

  lazy val ineligibleParameters: Set[ControlListParameter] =
    ControlListParameter.getParameterMap.values.filterNot(appConfig.loadIsEligibleConfig).toSet

  lazy val nonMigratableParameters: Set[ControlListParameter] =
    ControlListParameter.getParameterMap.values.filterNot(appConfig.loadIsMigratableConfig).toSet

}
