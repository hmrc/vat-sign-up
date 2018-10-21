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
import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.vatsignup.helpers.IntegrationTestConstants._
import uk.gov.hmrc.vatsignup.models.ExplicitEntityType.{GeneralPartnership, LimitedPartnership}
import uk.gov.hmrc.vatsignup.models.UnconfirmedSubscriptionRequest.credentialIdKey
import uk.gov.hmrc.vatsignup.models.{UnconfirmedSubscriptionRequest, UserEntered}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class UnconfirmedSubscriptionRequestRepositoryISpec extends UnitSpec with GuiceOneAppPerSuite with BeforeAndAfterEach {
  val repo: UnconfirmedSubscriptionRequestRepository = app.injector.instanceOf[UnconfirmedSubscriptionRequestRepository]

  private val testUnconfirmedSubscriptionRequest = UnconfirmedSubscriptionRequest(
    requestId = testRequestId,
    companyNumber = Some(testCompanyNumber)
  )

  override def beforeEach: Unit = {
    super.beforeEach()
    await(repo.drop)
  }


  "getRequestIdByCredential" should {
    import reactivemongo.play.json._
    implicit val format = UnconfirmedSubscriptionRequest.mongoFormat

    def findSubscriptionRequest(
                                 credentialId: String
                               )(implicit format: OFormat[UnconfirmedSubscriptionRequest]): Future[Option[UnconfirmedSubscriptionRequest]] =
      repo.collection.find(selector = Json.obj(
        credentialIdKey -> credentialId
      )).one[UnconfirmedSubscriptionRequest]

    "create a new record or return the existing record if it already exist," +
      " but if it does then return without replacement of the existing record" in {
      val (findBeforeInsert, newlyCreatedRequest, findAfterInsert, requestForExistingRecord) =
        await(
          for {
            findBeforeInsert <- findSubscriptionRequest(testCredentialId)
            newlyCreatedRequest <- repo.getRequestIdByCredential(testCredentialId)
            findAfterInsert <- findSubscriptionRequest(testCredentialId)
            requestForExistingRecord <- repo.getRequestIdByCredential(testCredentialId)
          } yield (findBeforeInsert, newlyCreatedRequest, findAfterInsert, requestForExistingRecord)
        )

      findBeforeInsert shouldBe None
      findAfterInsert shouldBe Some(newlyCreatedRequest)
      newlyCreatedRequest shouldBe requestForExistingRecord
    }
  }

  "insert" should {
    "successfully insert and retrieve a UnconfirmedSubscriptionRequest model" in {
      val res = for {
        _ <- repo.insert(testUnconfirmedSubscriptionRequest)
        model <- repo.findById(testRequestId)
      } yield model

      await(res) should contain(testUnconfirmedSubscriptionRequest)
    }
  }

  "insertVatNumber" should {
    val testUnconfirmedSubscriptionRequest = UnconfirmedSubscriptionRequest(
      requestId = testRequestId,
      vatNumber = Some(testVatNumber),
      isMigratable = Some(true)
    )

    "throw NoSuchElementException where the vat number doesn't exist" in {
      val res = for {
        _ <- repo.upsertVatNumber(testRequestId, testVatNumber, isMigratable = true)
        model <- repo.findById(testRequestId)
      } yield model

      intercept[NoSuchElementException] {
        await(res)
      }
    }

    "insert the unconfirmed subscription request where there is not already one" in {
      val res = for {
        _ <- repo.insert(testUnconfirmedSubscriptionRequest)
        _ <- repo.upsertVatNumber(testRequestId, testVatNumber, isMigratable = true)
        model <- repo.findById(testRequestId)
      } yield model

      await(res) should contain(testUnconfirmedSubscriptionRequest)
    }

    "replace the previous data when one already exists" in {
      val res = for {
        _ <- repo.insert(
          UnconfirmedSubscriptionRequest(
            testRequestId,
            credentialId = None,
            vatNumber = Some(testVatNumber),
            companyNumber = Some(testCompanyNumber),
            ctReference = Some(testCtReference),
            nino = Some(testNino),
            ninoSource = Some(UserEntered),
            partnershipEntity = Some(GeneralPartnership),
            partnershipUtr = Some(testUtr),
            email = Some(testEmail),
            identityVerified = Some(true)
          )
        )
        _ <- repo.upsertVatNumber(testRequestId, testVatNumber, isMigratable = true)
        model <- repo.findById(testRequestId)
      } yield model

      await(res) should contain(testUnconfirmedSubscriptionRequest)

    }
  }

  "upsertPartnership" should {
    val testUnconfirmedSubscriptionRequest = UnconfirmedSubscriptionRequest(
      requestId = testRequestId,
      partnershipEntity = Some(GeneralPartnership),
      partnershipUtr = Some(testUtr)
    )

    "throw NoSuchElementException where the request id doesn't exist" in {
      val res = for {
        _ <- repo.upsertPartnership(testRequestId, testUtr, GeneralPartnership)
        model <- repo.findById(testRequestId)
      } yield model

      intercept[NoSuchElementException] {
        await(res)
      }
    }

    "update the subscription request where there isn't already a partnership stored" in {
      val res = for {
        _ <- repo.insert(testUnconfirmedSubscriptionRequest)
        _ <- repo.upsertPartnership(testRequestId, testUtr, GeneralPartnership)
        model <- repo.findById(testRequestId)
      } yield model

      await(res) should contain(testUnconfirmedSubscriptionRequest)
    }

    "delete the nino if it already exists" in {
      val res = for {
        _ <- repo.insert(testUnconfirmedSubscriptionRequest)
        _ <- repo.upsertNino(testRequestId, testNino, UserEntered)
        withNino <- repo.findById(testRequestId)
        _ <- repo.upsertPartnership(testRequestId, testUtr, GeneralPartnership)
        withoutNino <- repo.findById(testRequestId)
      } yield (withNino, withoutNino)

      val (withNino, withoutNino) = await(res)

      withNino should contain(
        testUnconfirmedSubscriptionRequest.copy(
          nino = Some(testNino),
          ninoSource = Some(UserEntered),
          partnershipEntity = None,
          partnershipUtr = None,
          identityVerified = Some(false)
        ))
      withoutNino should contain(testUnconfirmedSubscriptionRequest.copy(identityVerified = Some(false)))
    }

    "delete the company number if it already exists" in {
      val res = for {
        _ <- repo.insert(testUnconfirmedSubscriptionRequest)
        _ <- repo.upsertCompanyNumber(testRequestId, testCompanyNumber)
        withCompanyNumber <- repo.findById(testRequestId)
        _ <- repo.upsertPartnership(testRequestId, testUtr, GeneralPartnership)
        withoutCompanyNumber <- repo.findById(testRequestId)
      } yield (withCompanyNumber, withoutCompanyNumber)

      val (withCompanyNumber, withoutCompanyNumber) = await(res)

      withCompanyNumber should contain(testUnconfirmedSubscriptionRequest.copy(
        companyNumber = Some(testCompanyNumber),
        partnershipEntity = None,
        partnershipUtr = None
      ))
      withoutCompanyNumber should contain(testUnconfirmedSubscriptionRequest)
    }

    "replace an existing stored partnership record" in {
      val newUtr = UUID.randomUUID().toString
      val res = for {
        _ <- repo.insert(testUnconfirmedSubscriptionRequest)
        _ <- repo.upsertPartnership(testRequestId, newUtr, GeneralPartnership)
        model <- repo.findById(testRequestId)
      } yield model

      await(res) should contain(UnconfirmedSubscriptionRequest(
        requestId = testRequestId,
        partnershipEntity = Some(GeneralPartnership),
        partnershipUtr = Some(newUtr)
      ))
    }

  }

  "upsertPartnershipLimited" should {
    val testSubscriptionRequest = UnconfirmedSubscriptionRequest(
      requestId = testRequestId,
      partnershipEntity = Some(LimitedPartnership),
      partnershipUtr = Some(testUtr),
      companyNumber = Some(testCompanyNumber)
    )

    "throw NoSuchElementException where the vat number doesn't exist" in {
      val res = for {
        _ <- repo.upsertPartnershipLimited(testRequestId, testUtr, testCompanyNumber, LimitedPartnership)
        model <- repo.findById(testRequestId)
      } yield model

      intercept[NoSuchElementException] {
        await(res)
      }
    }

    "update the subscription request where there isn't already a partnership stored" in {
      val res = for {
        _ <- repo.insert(testUnconfirmedSubscriptionRequest)
        _ <- repo.upsertPartnershipLimited(testRequestId, testUtr, testCompanyNumber, LimitedPartnership)
        model <- repo.findById(testRequestId)
      } yield model

      await(res) should contain(testSubscriptionRequest)
    }

    "delete the nino if it already exists" in {
      val res = for {
        _ <- repo.insert(testUnconfirmedSubscriptionRequest)
        _ <- repo.upsertNino(testRequestId, testNino, UserEntered)
        withNino <- repo.findById(testRequestId)
        _ <- repo.upsertPartnershipLimited(testRequestId, testUtr, testCompanyNumber, LimitedPartnership)
        withoutNino <- repo.findById(testRequestId)
      } yield (withNino, withoutNino)

      val (withNino, withoutNino) = await(res)

      withNino should contain(testSubscriptionRequest.copy(
        nino = Some(testNino),
        ninoSource = Some(UserEntered),
        partnershipEntity = None,
        partnershipUtr = None,
        companyNumber = None,
        identityVerified = Some(false)
      ))
      withoutNino should contain(testSubscriptionRequest.copy(identityVerified = Some(false)))
    }

    "replace an existing stored partnership record" in {
      val newUtr = UUID.randomUUID().toString
      val res = for {
        _ <- repo.insert(testSubscriptionRequest)
        _ <- repo.upsertPartnershipLimited(testRequestId, newUtr, testCompanyNumber, LimitedPartnership)
        model <- repo.findById(testRequestId)
      } yield model

      await(res) should contain(UnconfirmedSubscriptionRequest(
        requestId = testRequestId,
        partnershipEntity = Some(LimitedPartnership),
        partnershipUtr = Some(newUtr),
        companyNumber = Some(testCompanyNumber)
      ))
    }

  }

  "upsertCompanyNumber" should {

    val testUnconfirmedSubscriptionRequest = UnconfirmedSubscriptionRequest(
      requestId = testRequestId,
      companyNumber = Some(testCompanyNumber)
    )

    "throw NoSuchElementException where the request id doesn't exist" in {
      val res = for {
        _ <- repo.upsertCompanyNumber(testRequestId, testCompanyNumber)
        model <- repo.findById(testRequestId)
      } yield model

      intercept[NoSuchElementException] {
        await(res)
      }
    }

    "insert the unconfirmed subscription request where there isn't already a company number stored" in {
      val res = for {
        _ <- repo.insert(UnconfirmedSubscriptionRequest(testRequestId))
        _ <- repo.upsertCompanyNumber(testRequestId, testCompanyNumber)
        model <- repo.findById(testRequestId)
      } yield model

      await(res) should contain(testUnconfirmedSubscriptionRequest)

    }

    "replace an existing stored company number" in {
      val newCompanyNumber = UUID.randomUUID().toString
      val res = for {
        _ <- repo.insert(testUnconfirmedSubscriptionRequest)
        _ <- repo.upsertCompanyNumber(testRequestId, newCompanyNumber)
        model <- repo.findById(testRequestId)
      } yield model

      await(res) should contain(UnconfirmedSubscriptionRequest(
        requestId = testRequestId,
        companyNumber = Some(newCompanyNumber)
      ))
    }
  }

  "upsertEmail" should {
    val testUnconfirmedSubscriptionRequest = UnconfirmedSubscriptionRequest(
      requestId = testRequestId,
      email = Some(testEmail)
    )

    "throw NoSuchElementException where the request id doesn't exist" in {
      val res = for {
        _ <- repo.upsertEmail(testRequestId, testEmail)
        model <- repo.findById(testRequestId)
      } yield model

      intercept[NoSuchElementException] {
        await(res)
      }
    }

    "insert the unconfirmed subscription request where there isn't one" in {
      val res = for {
        _ <- repo.insert(UnconfirmedSubscriptionRequest(testRequestId))
        _ <- repo.upsertEmail(testRequestId, testEmail)
        model <- repo.findById(testRequestId)
      } yield model

      await(res) should contain(testUnconfirmedSubscriptionRequest)

    }
    "update the unconfirmed subscription request where there isn't already an email stored" in {
      val res = for {
        _ <- repo.insert(UnconfirmedSubscriptionRequest(testRequestId))
        _ <- repo.upsertVatNumber(testRequestId, testVatNumber, isMigratable = true)
        _ <- repo.upsertEmail(testRequestId, testEmail)
        model <- repo.findById(testRequestId)
      } yield model

      await(res) should contain(UnconfirmedSubscriptionRequest(
        requestId = testRequestId,
        vatNumber = Some(testVatNumber),
        email = Some(testEmail),
        isMigratable = Some(true)
      ))
    }

    "replace an existing stored email" in {
      val newEmail = UUID.randomUUID().toString
      val res = for {
        _ <- repo.insert(testUnconfirmedSubscriptionRequest)
        _ <- repo.upsertEmail(testRequestId, newEmail)
        model <- repo.findById(testRequestId)
      } yield model

      await(res) should contain(UnconfirmedSubscriptionRequest(testRequestId, email = Some(newEmail)))
    }
  }

  "upsertTransactionEmail" should {
    val testUnconfirmedSubscriptionRequest = UnconfirmedSubscriptionRequest(
      requestId = testRequestId,
      transactionEmail = Some(testEmail)
    )

    "throw NoSuchElementException where the request id doesn't exist" in {
      val res = for {
        _ <- repo.upsertTransactionEmail(testRequestId, testEmail)
        model <- repo.findById(testRequestId)
      } yield model

      intercept[NoSuchElementException] {
        await(res)
      }
    }

    "insert the unconfirmed subscription request where there isn't one" in {
      val res = for {
        _ <- repo.insert(testUnconfirmedSubscriptionRequest)
        _ <- repo.upsertTransactionEmail(testRequestId, testEmail)
        model <- repo.findById(testRequestId)
      } yield model

      await(res) should contain(testUnconfirmedSubscriptionRequest)
    }

    "update the unconfirmed subscription request where there isn't already a transaction email stored" in {
      val res = for {
        _ <- repo.insert(testUnconfirmedSubscriptionRequest)
        _ <- repo.upsertVatNumber(testRequestId, testVatNumber, isMigratable = true)
        _ <- repo.upsertTransactionEmail(testRequestId, testEmail)
        model <- repo.findById(testRequestId)
      } yield model

      await(res) should contain(UnconfirmedSubscriptionRequest(
        requestId = testRequestId,
        vatNumber = Some(testVatNumber),
        transactionEmail = Some(testEmail),
        isMigratable = Some(true)
      ))
    }

    "replace an existing stored transaction email" in {
      val newEmail = UUID.randomUUID().toString
      val res = for {
        _ <- repo.insert(testUnconfirmedSubscriptionRequest)
        _ <- repo.upsertTransactionEmail(testRequestId, newEmail)
        model <- repo.findById(testRequestId)
      } yield model

      await(res) should contain(UnconfirmedSubscriptionRequest(
        requestId = testRequestId, transactionEmail = Some(newEmail)
      ))
    }
  }

  "upsertNino" should {
    val testUnconfirmedSubscriptionRequest = UnconfirmedSubscriptionRequest(
      requestId = testRequestId,
      nino = Some(testNino),
      ninoSource = Some(UserEntered),
      identityVerified = Some(false)
    )

    "throw NoSuchElementException where the request id doesn't exist" in {
      val res = for {
        _ <- repo.upsertNino(testRequestId, testNino, UserEntered)
        model <- repo.findById(testRequestId)
      } yield model

      intercept[NoSuchElementException] {
        await(res)
      }
    }

    "insert the unconfirmed subscription request where there isn't one" in {
      val res = for {
        _ <- repo.insert(testUnconfirmedSubscriptionRequest)
        _ <- repo.upsertNino(testRequestId, testNino, UserEntered)
        model <- repo.findById(testRequestId)
      } yield model

      await(res) should contain(testUnconfirmedSubscriptionRequest)
    }

    "update the unconfirmed subscription request where there isn't already a nino stored" in {
      val res = for {
        _ <- repo.insert(testUnconfirmedSubscriptionRequest)
        _ <- repo.upsertVatNumber(testRequestId, testVatNumber, isMigratable = true)
        _ <- repo.upsertNino(testRequestId, testNino, UserEntered)
        model <- repo.findById(testRequestId)
      } yield model

      await(res) should contain(
        testUnconfirmedSubscriptionRequest.copy(vatNumber = Some(testVatNumber), isMigratable = Some(true))
      )
    }

    "set identity verification to false" in {
      val res = for {
        _ <- repo.insert(testUnconfirmedSubscriptionRequest)
        _ <- repo.upsertIdentityVerified(testRequestId)
        identityVerified <- repo.findById(testRequestId)
        _ <- repo.upsertNino(testRequestId, testNino, UserEntered)
        identityNotVerified <- repo.findById(testRequestId)
      } yield (identityVerified, identityNotVerified)

      val (identityVerified, identityNotVerified) = await(res)

      identityVerified should contain(testUnconfirmedSubscriptionRequest.copy(
        identityVerified = Some(true)
      ))
      identityNotVerified should contain(testUnconfirmedSubscriptionRequest)
    }


    "replace an existing stored nino" in {
      val newNino = UUID.randomUUID().toString
      val res = for {
        _ <- repo.insert(testUnconfirmedSubscriptionRequest)
        _ <- repo.upsertNino(testRequestId, newNino, UserEntered)
        model <- repo.findById(testRequestId)
      } yield model

      await(res) should contain(testUnconfirmedSubscriptionRequest.copy(nino = Some(newNino)))
    }
  }

  "upsertIdentityVerified" should {
    val testUnconfirmedSubscriptionRequest = UnconfirmedSubscriptionRequest(
      requestId = testRequestId,
      identityVerified = Some(false)
    )

    "throw NoSuchElementException where the request id doesn't exist" in {
      val res = for {
        _ <- repo.upsertIdentityVerified(testRequestId)
        model <- repo.findById(testRequestId)
      } yield model

      intercept[NoSuchElementException] {
        await(res)
      }
    }

    "insert the unconfirmed subscription request where there isn't one" in {
      val res = for {
        _ <- repo.insert(testUnconfirmedSubscriptionRequest)
        _ <- repo.upsertIdentityVerified(testRequestId)
        model <- repo.findById(testRequestId)
      } yield model

      await(res) should contain(testUnconfirmedSubscriptionRequest.copy(identityVerified = Some(true)))
    }

    "update the unconfirmed subscription request with IdentityVerified" in {
      val res = for {
        _ <- repo.insert(testUnconfirmedSubscriptionRequest)
        _ <- repo.upsertVatNumber(testRequestId, testVatNumber, isMigratable = true)
        _ <- repo.upsertIdentityVerified(testRequestId)
        model <- repo.findById(testRequestId)
      } yield model

      await(res) should contain(testUnconfirmedSubscriptionRequest.copy(
        vatNumber = Some(testVatNumber),
        identityVerified = Some(true),
        isMigratable = Some(true)
      ))
    }
  }

  "upsertCtReference" should {
    val testUnconfirmedSubscriptionRequest = UnconfirmedSubscriptionRequest(
      requestId = testRequestId,
      ctReference = Some(testCtReference)
    )

    "throw NoSuchElementException where the request id doesn't exist" in {
      val res = for {
        _ <- repo.upsertCtReference(testRequestId, testCtReference)
        model <- repo.findById(testRequestId)
      } yield model

      intercept[NoSuchElementException] {
        await(res)
      }
    }

    "insert the unconfirmed subscription request where there isn't one" in {
      val res = for {
        _ <- repo.insert(testUnconfirmedSubscriptionRequest)
        _ <- repo.upsertCtReference(testRequestId, testCtReference)
        model <- repo.findById(testRequestId)
      } yield model

      await(res) should contain(testUnconfirmedSubscriptionRequest)
    }

    "update the unconfirmed subscription request with CtReference" in {
      val res = for {
        _ <- repo.insert(testUnconfirmedSubscriptionRequest)
        _ <- repo.upsertVatNumber(testRequestId, testVatNumber, isMigratable = true)
        _ <- repo.upsertCtReference(testRequestId, testCtReference)
        model <- repo.findById(testRequestId)
      } yield model

      await(res) should contain(
        testUnconfirmedSubscriptionRequest.copy(vatNumber = Some(testVatNumber), isMigratable = Some(true))
      )
    }
  }

  "deleteRecord" should {
    "delete the entry stored against the vrn" in {
      val res = for {
        _ <- repo.insert(testUnconfirmedSubscriptionRequest)
        _ <- repo.upsertVatNumber(testRequestId, testVatNumber, isMigratable = true)
        inserted <- repo.findById(testRequestId)
        _ <- repo.deleteRecord(testRequestId)
        postDelete <- repo.findById(testRequestId)
      } yield (inserted, postDelete)

      val (inserted, postDelete) = await(res)
      inserted should contain(UnconfirmedSubscriptionRequest(
        requestId = testRequestId,
        vatNumber = Some(testVatNumber),
        isMigratable = Some(true)
      ))
      postDelete shouldBe None
    }
  }
}
