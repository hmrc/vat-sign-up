/*
 * Copyright 2020 HM Revenue & Customs
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

package uk.gov.hmrc.vatsignup.config.mocks

import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, Suite}
import uk.gov.hmrc.vatsignup.config.EligibilityConfig
import uk.gov.hmrc.vatsignup.helpers.TestConstants
import uk.gov.hmrc.vatsignup.models.DateRange
import uk.gov.hmrc.vatsignup.models.controllist.{ControlListParameter, Stagger}

trait MockEligibilityConfig extends MockitoSugar with BeforeAndAfterEach {
  this: Suite =>
  override def beforeEach(): Unit = {
    reset(mockEligibilityConfig)

    when(mockEligibilityConfig.ineligibleParameters) thenReturn Set.empty[ControlListParameter]
    when(mockEligibilityConfig.nonMigratableParameters) thenReturn Set.empty[ControlListParameter]

    super.beforeEach()
  }

  val mockEligibilityConfig: EligibilityConfig = mock[EligibilityConfig]

  def mockIneligibleParameters(ineligibleParameters: Set[ControlListParameter]): Unit =
    when(mockEligibilityConfig.ineligibleParameters) thenReturn ineligibleParameters

  def mockNonMigratableParameters(nonMigratableParameters: Set[ControlListParameter]): Unit =
    when(mockEligibilityConfig.nonMigratableParameters) thenReturn nonMigratableParameters

  def mockDirectDebitStaggerParameters(config: Map[Stagger, Set[DateRange]] = TestConstants.testDDConfig): Unit =
    when(mockEligibilityConfig.directDebitStaggerParameters) thenReturn config

  def mockFilingDateStaggerParameters(config: Map[Stagger, Set[DateRange]] = TestConstants.testFilingDateConfig): Unit =
    when(mockEligibilityConfig.filingDateStaggerParameters) thenReturn config

}
