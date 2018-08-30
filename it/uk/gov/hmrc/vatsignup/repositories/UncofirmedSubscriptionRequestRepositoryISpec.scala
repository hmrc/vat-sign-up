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
import uk.gov.hmrc.vatsignup.models.UnconfirmedSubscriptionRequest.credentialIdKey
import uk.gov.hmrc.vatsignup.models.{UnconfirmedSubscriptionRequest, UserEntered}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class UncofirmedSubscriptionRequestRepositoryISpec extends UnitSpec with GuiceOneAppPerSuite with BeforeAndAfterEach {
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

    def findSubscriptionRequest(credentialId: String
                               )(implicit format: OFormat[UnconfirmedSubscriptionRequest]): Future[Option[UnconfirmedSubscriptionRequest]] =
      repo.collection.find(selector = Json.obj(credentialIdKey -> credentialId)).one[UnconfirmedSubscriptionRequest]

    "create a new record and return the requestID if the record does not already exist," +
      " but if it does then return without replacement of the existing record" in {
      val (findBeforeInsert, someRequestId, findAfterInsert, someRequestIdForExistingRecord) =
        await(
          for {
            findBeforeInsert <- findSubscriptionRequest(testCredentialId)
            someRequestId <- repo.getRequestIdByCredential(testCredentialId)
            findAfterInsert <- findSubscriptionRequest(testCredentialId)
            someRequestIdForExistingRecord <- repo.getRequestIdByCredential(testCredentialId)
          } yield (findBeforeInsert, someRequestId, findAfterInsert, someRequestIdForExistingRecord)
        )

      findBeforeInsert shouldBe None
      someRequestId shouldBe defined
      someRequestId should not be empty
      findAfterInsert shouldBe Some(UnconfirmedSubscriptionRequest(someRequestId.get, Some(testCredentialId)))
      someRequestId shouldBe someRequestIdForExistingRecord
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
      vatNumber = Some(testVatNumber)
    )

    "insert the unconfirmed subscription request where there is not already one" in {
      val res = for {
        _ <- repo.upsertVatNumber(testRequestId, testVatNumber)
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
            Some(testVatNumber),
            Some(testCompanyNumber),
            Some(testCtReference),
            Some(testNino),
            Some(UserEntered),
            Some(testEmail),
            identityVerified = Some(true)
          )
        )
        _ <- repo.upsertVatNumber(testRequestId, testVatNumber)
        model <- repo.findById(testRequestId)
      } yield model

      await(res) should contain(testUnconfirmedSubscriptionRequest)

    }
  }

  "upsertCompanyNumber" should {

    val testUnconfirmedSubscriptionRequest = UnconfirmedSubscriptionRequest(
      requestId = testRequestId,
      companyNumber = Some(testCompanyNumber)
    )

    "insert the unconfirmed subscription request where there isn't already a company number stored" in {
      val res = for {
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

    "insert the unconfirmed subscription request where there isn't one" in {
      val res = for {
        _ <- repo.upsertEmail(testRequestId, testEmail)
        model <- repo.findById(testRequestId)
      } yield model

      await(res) should contain(testUnconfirmedSubscriptionRequest)

    }
    "update the unconfirmed subscription request where there isn't already an email stored" in {
      val res = for {
        _ <- repo.upsertVatNumber(testRequestId, testVatNumber)
        _ <- repo.upsertEmail(testRequestId, testEmail)
        model <- repo.findById(testRequestId)
      } yield model

      await(res) should contain(UnconfirmedSubscriptionRequest(
        requestId = testRequestId,
        vatNumber = Some(testVatNumber),
        email = Some(testEmail)
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

    "insert the unconfirmed subscription request where there isn't one" in {
      val res = for {
        _ <- repo.upsertTransactionEmail(testRequestId, testEmail)
        model <- repo.findById(testRequestId)
      } yield model

      await(res) should contain(testUnconfirmedSubscriptionRequest)
    }

    "update the unconfirmed subscription request where there isn't already a transaction email stored" in {
      val res = for {
        _ <- repo.upsertVatNumber(testRequestId, testVatNumber)
        _ <- repo.upsertTransactionEmail(testRequestId, testEmail)
        model <- repo.findById(testRequestId)
      } yield model

      await(res) should contain(UnconfirmedSubscriptionRequest(
        requestId = testRequestId, vatNumber = Some(testVatNumber), transactionEmail = Some(testEmail)
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

    "insert the unconfirmed subscription request where there isn't one" in {
      val res = for {
        _ <- repo.upsertNino(testRequestId, testNino, UserEntered)
        model <- repo.findById(testRequestId)
      } yield model

      await(res) should contain(testUnconfirmedSubscriptionRequest)
    }

    "update the unconfirmed subscription request where there isn't already a nino stored" in {
      val res = for {
        _ <- repo.upsertVatNumber(testRequestId, testVatNumber)
        _ <- repo.upsertNino(testRequestId, testNino, UserEntered)
        model <- repo.findById(testRequestId)
      } yield model

      await(res) should contain(testUnconfirmedSubscriptionRequest.copy(vatNumber = Some(testVatNumber)))
    }

    "set identity verification to false" in {
      val res = for {
        _ <- repo.upsertIdentityVerified(testRequestId)
        identityVerified <- repo.findById(testRequestId)
        _ <- repo.upsertNino(testRequestId, testNino, UserEntered)
        identityNotVerified <- repo.findById(testRequestId)
      } yield (identityVerified, identityNotVerified)

      val (identityVerified, identityNotVerified) = await(res)

      identityVerified should contain(testUnconfirmedSubscriptionRequest.copy(
        identityVerified = Some(true),
        nino = None,
        ninoSource = None
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

    "insert the unconfirmed subscription request where there isn't one" in {
      val res = for {
        _ <- repo.upsertIdentityVerified(testRequestId)
        model <- repo.findById(testRequestId)
      } yield model

      await(res) should contain(testUnconfirmedSubscriptionRequest.copy(identityVerified = Some(true)))
    }

    "update the unconfirmed subscription request with IdentityVerified" in {
      val res = for {
        _ <- repo.upsertVatNumber(testRequestId, testVatNumber)
        _ <- repo.upsertIdentityVerified(testRequestId)
        model <- repo.findById(testRequestId)
      } yield model

      await(res) should contain(testUnconfirmedSubscriptionRequest.copy(
        vatNumber = Some(testVatNumber),
        identityVerified = Some(true)
      ))
    }
  }

  "upsertCtReference" should {
    val testUnconfirmedSubscriptionRequest = UnconfirmedSubscriptionRequest(
      requestId = testRequestId,
      ctReference = Some(testCtReference)
    )

    "insert the unconfirmed subscription request where there isn't one" in {
      val res = for {
        _ <- repo.upsertCtReference(testRequestId, testCtReference)
        model <- repo.findById(testRequestId)
      } yield model

      await(res) should contain(testUnconfirmedSubscriptionRequest)
    }

    "update the unconfirmed subscription request with CtReference" in {
      val res = for {
        _ <- repo.upsertVatNumber(testRequestId, testVatNumber)
        _ <- repo.upsertCtReference(testRequestId, testCtReference)
        model <- repo.findById(testRequestId)
      } yield model

      await(res) should contain(testUnconfirmedSubscriptionRequest.copy(vatNumber = Some(testVatNumber)))
    }
  }

  "deleteRecord" should {
    "delete the entry stored against the vrn" in {
      val res = for {
        _ <- repo.upsertVatNumber(testRequestId, testVatNumber)
        inserted <- repo.findById(testRequestId)
        _ <- repo.deleteRecord(testRequestId)
        postDelete <- repo.findById(testRequestId)
      } yield (inserted, postDelete)

      val (inserted, postDelete) = await(res)
      inserted should contain(UnconfirmedSubscriptionRequest(
        requestId = testRequestId,
        vatNumber = Some(testVatNumber)
      ))
      postDelete shouldBe None
    }
  }
}
