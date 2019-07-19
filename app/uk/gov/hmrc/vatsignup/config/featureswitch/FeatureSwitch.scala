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

package uk.gov.hmrc.vatsignup.config.featureswitch

import FeatureSwitch._

sealed trait FeatureSwitch {
  val name: String
  val displayName: String
}

object FeatureSwitch {
  val prefix = "feature-switch"

  val switches: Set[FeatureSwitch] = Set(
    StubDESFeature,
    StubAgentServicesFeature,
    AdditionalKnownFacts,
    SkipPartnershipKnownFactsMismatch,
    AutoClaimEnrolment
  )

  def apply(str: String): FeatureSwitch =
    switches find (_.name == str) match {
      case Some(switch) => switch
      case None => throw new IllegalArgumentException("Invalid feature switch: " + str)
    }

  def apply(setting: FeatureSwitchSetting): FeatureSwitch =
    switches find (_.displayName == setting.feature) match {
      case Some(switch) => switch
      case None => throw new IllegalArgumentException("Invalid feature switch: " + setting.feature)
    }
}

case object StubDESFeature extends FeatureSwitch {
  val displayName = s"Use stub for DES connection"
  val name = s"$prefix.stub-des"
}

case object StubAgentServicesFeature extends FeatureSwitch {
  val displayName = s"Use stub for Agent Services connection"
  val name = s"$prefix.stub-agent-services"
}

case object AdditionalKnownFacts extends FeatureSwitch {
  val displayName: String = "Enable additional known facts check (Box 5 and filing month)"
  val name: String = s"$prefix.additional-known-facts"
}

case object SkipPartnershipKnownFactsMismatch extends FeatureSwitch {
  val displayName: String = "Skip known facts mismatches for partnerships"
  val name: String = s"$prefix.skip-partnership-kf-mismatch"
}

case object AutoClaimEnrolment extends FeatureSwitch {
  val displayName: String = "Enable the auto claim enrolment"
  val name: String = s"$prefix.auto-claim-enrolment"
}
