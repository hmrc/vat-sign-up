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

package uk.gov.hmrc.vatsignup.service.mocks

import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.Request
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.vatsignup.models.controllist.Stagger
import uk.gov.hmrc.vatsignup.services.MigrationCheckService
import uk.gov.hmrc.vatsignup.services.MigrationCheckService.MigrationEligibility

trait MockMigrationCheckService extends MockitoSugar {
  val mockMigrationCheckService: MigrationCheckService = mock[MigrationCheckService]

  def mockCheckMigrationRestrictions(vatNumber: String,
                                     stagger: Stagger,
                                     isDirectDebit: Boolean,
                                     isMigratable: Boolean
                                    )(result: MigrationEligibility): Unit =
    when(mockMigrationCheckService.checkMigrationRestrictions(
      ArgumentMatchers.eq(vatNumber),
      ArgumentMatchers.eq(stagger),
      ArgumentMatchers.eq(isDirectDebit),
      ArgumentMatchers.eq(isMigratable)
    )(
      ArgumentMatchers.any[HeaderCarrier],
      ArgumentMatchers.any[Request[_]]
    )) thenReturn result

}
