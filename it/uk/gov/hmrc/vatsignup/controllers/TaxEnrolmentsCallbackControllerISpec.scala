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

package uk.gov.hmrc.vatsignup.controllers

import org.scalatest.BeforeAndAfterEach
import play.api.http.Status._
import play.api.libs.json.Json
import uk.gov.hmrc.vatsignup.config.featureswitch.EmailNotification
import uk.gov.hmrc.vatsignup.helpers.IntegrationTestConstants._
import uk.gov.hmrc.vatsignup.helpers.servicemocks.EmailStub
import uk.gov.hmrc.vatsignup.helpers.{ComponentSpecBase, CustomMatchers, TestEmailRequestRepository}
import uk.gov.hmrc.vatsignup.models.EmailRequest
import uk.gov.hmrc.vatsignup.services.SubscriptionNotificationService._

import scala.concurrent.ExecutionContext.Implicits.global

class TaxEnrolmentsCallbackControllerISpec extends ComponentSpecBase with BeforeAndAfterEach with CustomMatchers with TestEmailRequestRepository {

  "/subscription-request/vat-number/callback" when {
    "callback is successful" should {
      "return NO_CONTENT with the status" in {
        enable(EmailNotification)

        await(emailRequestRepo.insert(EmailRequest(testVatNumber, testEmail, isDelegated = false)))

        EmailStub.stubSendEmail(testEmail, principalSuccessEmailTemplate)(ACCEPTED)

        val res = post(s"/subscription-request/vat-number/$testVatNumber/callback")(Json.obj("state" -> "SUCCEEDED"))
        res should have(
          httpStatus(NO_CONTENT)
        )
      }

      "callback is unsuccessful" should {
        "return no Email" in {
          enable(EmailNotification)

          await(emailRequestRepo.insert(EmailRequest(testVatNumber, testEmail, isDelegated = false)))

          val res = post(s"/subscription-request/vat-number/$testVatNumber/callback")(Json.obj("state" -> "ERROR"))
          res should have(
            httpStatus(NO_CONTENT)
          )
        }
      }

    }
  }

}
