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

import java.util.UUID

import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.vatsignup.helpers.IntegrationTestConstants._
import uk.gov.hmrc.vatsignup.models._

import scala.concurrent.ExecutionContext.Implicits.global

class SubscriptionRequestRepositoryISpec extends UnitSpec with GuiceOneAppPerSuite with BeforeAndAfterEach {
  lazy val repo: SubscriptionRequestRepository = app.injector.instanceOf[SubscriptionRequestRepository]

  private val testSubscriptionRequest = SubscriptionRequest(
    vatNumber = testVatNumber,
    businessEntity = Some(LimitedCompany(testCompanyNumber))
  )

  override def beforeEach: Unit = {
    super.beforeEach()
    await(repo.drop)
  }

  "insert" should {
    "successfully insert and retrieve a SubscriptionRequest model" in {
      val res = for {
        _ <- repo.insert(testSubscriptionRequest)
        model <- repo.findById(testVatNumber)
      } yield model

      await(res) should contain(testSubscriptionRequest)
    }
  }

  "insertVatNumber" should {
    val testSubscriptionRequest = SubscriptionRequest(
      vatNumber = testVatNumber
    )

    "insert the subscription request where there is not already one" when {
      "isMigratable is true" in {
        val res = for {
          _ <- repo.upsertVatNumber(testVatNumber, isMigratable = true)
          model <- repo.findById(testVatNumber)
        } yield model

        await(res) should contain(testSubscriptionRequest)
      }
      "isMigratable is false" in {
        val res = for {
          _ <- repo.upsertVatNumber(testVatNumber, isMigratable = false)
          model <- repo.findById(testVatNumber)
        } yield model

        await(res) should contain(testSubscriptionRequest.copy(isMigratable = false))
      }
    }

    "replace the previous data when one already exists" in {
      val res = for {
        _ <- repo.insert(
          SubscriptionRequest(
            vatNumber = testVatNumber,
            ctReference = Some(testCtReference),
            businessEntity = Some(SoleTrader(testNino)),
            ninoSource = Some(UserEntered),
            email = Some(testEmail),
            identityVerified = true
          )
        )
        _ <- repo.upsertVatNumber(testVatNumber, isMigratable = true)
        model <- repo.findById(testVatNumber)
      } yield model

      await(res) should contain(testSubscriptionRequest)

    }
  }

  "upsertEmail" should {
    val testSubscriptionRequest = SubscriptionRequest(
      vatNumber = testVatNumber,
      email = Some(testEmail)
    )

    "throw NoSuchElementException where the vat number doesn't exist" in {
      val res = for {
        _ <- repo.upsertEmail(testVatNumber, testEmail)
        model <- repo.findById(testVatNumber)
      } yield model

      intercept[NoSuchElementException] {
        await(res)
      }
    }

    "update the subscription request where there isn't already an email stored" in {
      val res = for {
        _ <- repo.upsertVatNumber(testVatNumber, isMigratable = true)
        _ <- repo.upsertEmail(testVatNumber, testEmail)
        model <- repo.findById(testVatNumber)
      } yield model

      await(res) should contain(testSubscriptionRequest)
    }

    "replace an existing stored email" in {
      val newEmail = UUID.randomUUID().toString
      val res = for {
        _ <- repo.insert(testSubscriptionRequest)
        _ <- repo.upsertEmail(testVatNumber, newEmail)
        model <- repo.findById(testVatNumber)
      } yield model

      await(res) should contain(SubscriptionRequest(testVatNumber, email = Some(newEmail)))
    }
  }

  "upsertTransactionEmail" should {
    val testSubscriptionRequest = SubscriptionRequest(
      vatNumber = testVatNumber,
      transactionEmail = Some(testEmail)
    )

    "throw NoSuchElementException where the vat number doesn't exist" in {
      val res = for {
        _ <- repo.upsertTransactionEmail(testVatNumber, testEmail)
        model <- repo.findById(testVatNumber)
      } yield model

      intercept[NoSuchElementException] {
        await(res)
      }
    }

    "update the subscription request where there isn't already a transaction email stored" in {
      val res = for {
        _ <- repo.upsertVatNumber(testVatNumber, isMigratable = true)
        _ <- repo.upsertTransactionEmail(testVatNumber, testEmail)
        model <- repo.findById(testVatNumber)
      } yield model

      await(res) should contain(testSubscriptionRequest)
    }

    "replace an existing stored transaction email" in {
      val newEmail = UUID.randomUUID().toString
      val res = for {
        _ <- repo.insert(testSubscriptionRequest)
        _ <- repo.upsertTransactionEmail(testVatNumber, newEmail)
        model <- repo.findById(testVatNumber)
      } yield model

      await(res) should contain(SubscriptionRequest(testVatNumber, transactionEmail = Some(newEmail)))
    }
  }

  "upsertIdentityVerified" should {
    "throw NoSuchElementException where the vat number doesn't exist" in {
      val res = for {
        _ <- repo.upsertIdentityVerified(testVatNumber)
        model <- repo.findById(testVatNumber)
      } yield model

      intercept[NoSuchElementException] {
        await(res)
      }
    }

    "update the subscription request with IdentityVerified" in {
      val res = for {
        _ <- repo.upsertVatNumber(testVatNumber, isMigratable = true)
        _ <- repo.upsertIdentityVerified(testVatNumber)
        model <- repo.findById(testVatNumber)
      } yield model

      await(res) should contain(SubscriptionRequest(
        vatNumber = testVatNumber,
        identityVerified = true
      ))
    }
  }

  "upsertBusinessEntity" should {
    "store a Sole Trader" in {
      val res = for {
        _ <- repo.upsertVatNumber(testVatNumber, isMigratable = true)
        _ <- repo.upsertBusinessEntity(testVatNumber, SoleTrader(testNino))
        model <- repo.findById(testVatNumber)
      } yield model

      await(res) shouldBe Some(SubscriptionRequest(
        vatNumber = testVatNumber,
        isMigratable = true,
        ninoSource = Some(UserEntered),
        businessEntity = Some(SoleTrader(testNino))
      ))
    }
    "store a Limited Company" in {
      val res = for {
        _ <- repo.upsertVatNumber(testVatNumber, isMigratable = true)
        _ <- repo.upsertBusinessEntity(testVatNumber, LimitedCompany(testCompanyNumber))
        model <- repo.findById(testVatNumber)
      } yield model

      await(res) shouldBe Some(SubscriptionRequest(
        vatNumber = testVatNumber,
        isMigratable = true,
        businessEntity = Some(LimitedCompany(testCompanyNumber))
      ))
    }
    "store a General Partnership" in {
      val res = for {
        _ <- repo.upsertVatNumber(testVatNumber, isMigratable = true)
        _ <- repo.upsertBusinessEntity(testVatNumber, GeneralPartnership(testUtr))
        model <- repo.findById(testVatNumber)
      } yield model

      await(res) shouldBe Some(SubscriptionRequest(
        vatNumber = testVatNumber,
        isMigratable = true,
        businessEntity = Some(GeneralPartnership(testUtr))
      ))
    }
    "store a Limited Partnership" in {
      val res = for {
        _ <- repo.upsertVatNumber(testVatNumber, isMigratable = true)
        _ <- repo.upsertBusinessEntity(testVatNumber, LimitedPartnership(testUtr, testCompanyNumber))
        model <- repo.findById(testVatNumber)
      } yield model

      await(res) shouldBe Some(SubscriptionRequest(
        vatNumber = testVatNumber,
        isMigratable = true,
        businessEntity = Some(LimitedPartnership(testUtr, testCompanyNumber))
      ))
    }
    "store a Limited Liabiility Partnership" in {
      val res = for {
        _ <- repo.upsertVatNumber(testVatNumber, isMigratable = true)
        _ <- repo.upsertBusinessEntity(testVatNumber, LimitedLiabilityPartnership(testUtr, testCompanyNumber))
        model <- repo.findById(testVatNumber)
      } yield model

      await(res) shouldBe Some(SubscriptionRequest(
        vatNumber = testVatNumber,
        isMigratable = true,
        businessEntity = Some(LimitedLiabilityPartnership(testUtr, testCompanyNumber))
      ))
    }
    "store a Scottish Limited Partnership" in {
      val res = for {
        _ <- repo.upsertVatNumber(testVatNumber, isMigratable = true)
        _ <- repo.upsertBusinessEntity(testVatNumber, ScottishLimitedPartnership(testUtr, testCompanyNumber))
        model <- repo.findById(testVatNumber)
      } yield model

      await(res) shouldBe Some(SubscriptionRequest(
        vatNumber = testVatNumber,
        isMigratable = true,
        businessEntity = Some(ScottishLimitedPartnership(testUtr, testCompanyNumber))
      ))
    }
    "store a VAT Group" in {
      val res = for {
        _ <- repo.upsertVatNumber(testVatNumber, isMigratable = true)
        _ <- repo.upsertBusinessEntity(testVatNumber, VatGroup)
        model <- repo.findById(testVatNumber)
      } yield model

      await(res) shouldBe Some(SubscriptionRequest(
        vatNumber = testVatNumber,
        isMigratable = true,
        businessEntity = Some(VatGroup)
      ))
    }

    "store a Registered Society" in {
      val res = for {
        _ <- repo.upsertVatNumber(testVatNumber, isMigratable = true)
        _ <- repo.upsertBusinessEntity(testVatNumber, RegisteredSociety(testCompanyNumber))
        model <- repo.findById(testVatNumber)
      } yield model

      await(res) shouldBe Some(SubscriptionRequest(
        vatNumber = testVatNumber,
        isMigratable = true,
        businessEntity = Some(RegisteredSociety(testCompanyNumber))
      ))
    }
  }

  "upsertCtReference" should {
    "throw NoSuchElementException where the vat number doesn't exist" in {
      val res = for {
        _ <- repo.upsertCtReference(testVatNumber, testCtReference)
        model <- repo.findById(testVatNumber)
      } yield model

      intercept[NoSuchElementException] {
        await(res)
      }
    }

    "update the subscription request with CtReference" in {
      val res = for {
        _ <- repo.upsertVatNumber(testVatNumber, isMigratable = true)
        _ <- repo.upsertCtReference(testVatNumber, testCtReference)
        model <- repo.findById(testVatNumber)
      } yield model

      await(res) should contain(SubscriptionRequest(
        vatNumber = testVatNumber,
        ctReference = Some(testCtReference)
      ))
    }
  }

  "deleteRecord" should {
    "delete the entry stored against the vrn" in {
      val res = for {
        _ <- repo.upsertVatNumber(testVatNumber, isMigratable = true)
        inserted <- repo.findById(testVatNumber)
        _ <- repo.deleteRecord(testVatNumber)
        postDelete <- repo.findById(testVatNumber)
      } yield (inserted, postDelete)

      val (inserted, postDelete) = await(res)
      inserted should contain(SubscriptionRequest(testVatNumber))
      postDelete shouldBe None
    }
  }

}
