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

package uk.gov.hmrc.vatsignup.repositories

import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import reactivemongo.core.errors.DatabaseException
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.vatsignup.helpers.IntegrationTestConstants._
import uk.gov.hmrc.vatsignup.models.{EmailRequest, SubscriptionRequest}

import scala.concurrent.ExecutionContext.Implicits.global

class EmailRequestRepositoryISpec extends UnitSpec with GuiceOneAppPerSuite with BeforeAndAfterEach {
  val repo: EmailRequestRepository = app.injector.instanceOf[EmailRequestRepository]

  private val testEmailRequest = EmailRequest(
    vatNumber = testVatNumber,
    email = testEmail,
    isDelegated = true
  )

  override def beforeEach: Unit = {
    super.beforeEach()
    await(repo.drop)
  }

  "upsertEmail" should {
    "insert the subscription request where there is not already one" in {
      val res = for {
        _ <- repo.upsertEmail(testVatNumber, testEmail, isDelegated = true)
        model <- repo.findById(testVatNumber)
      } yield model

      await(res) should contain(testEmailRequest)
    }

    "replace the previous data when one already exists" in {
      val res = for {
        _ <- repo.insert(EmailRequest(testVatNumber, testEmail, isDelegated = false))
        _ <- repo.upsertEmail(testVatNumber, testEmail, isDelegated = true)
        model <- repo.findById(testVatNumber)
      } yield model

      await(res) should contain(testEmailRequest)
    }
  }

}
