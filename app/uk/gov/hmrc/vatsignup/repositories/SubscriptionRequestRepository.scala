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

package uk.gov.hmrc.vatsignup.repositories

import javax.inject.{Inject, Singleton}
import play.api.libs.json.{Format, JsObject, Json}
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.api.commands.{UpdateWriteResult, WriteResult}
import reactivemongo.api.indexes.IndexType.Ascending
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.bson.BSONDocument
import reactivemongo.play.json.JSONSerializationPack.Writer
import reactivemongo.play.json._
import uk.gov.hmrc.mongo.ReactiveRepository
import uk.gov.hmrc.vatsignup.config.AppConfig
import uk.gov.hmrc.vatsignup.models.ContactPreference.contactPreferenceFormat
import uk.gov.hmrc.vatsignup.models.SubscriptionRequest._
import uk.gov.hmrc.vatsignup.models.{BusinessEntity, ContactPreference, SubscriptionRequest}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SubscriptionRequestRepository @Inject()(mongo: ReactiveMongoComponent,
                                              appConfig: AppConfig)(implicit val ec: ExecutionContext)
  extends ReactiveRepository[SubscriptionRequest, String](
    "subscriptionRequestRepository",
    mongo.mongoConnector.db,
    SubscriptionRequest.mongoFormat,
    implicitly[Format[String]]
  ) {

  private def upsert(vatNumber: String, elementKey: String, elementValue: String): Future[UpdateWriteResult] = {
    collection.update(
      selector = Json.obj(idKey -> vatNumber),
      update = Json.obj("$set" -> Json.obj(
        elementKey -> elementValue
      )),
      upsert = false
    ).filter(_.n == 1)
  }

  def upsertVatNumber(vatNumber: String, isMigratable: Boolean, isDirectDebit: Boolean): Future[UpdateWriteResult] = {
    collection.update(
      selector = Json.obj(idKey -> vatNumber),
      update = SubscriptionRequest(vatNumber, isMigratable = isMigratable, isDirectDebit = isDirectDebit, contactPreference = None),
      upsert = true
    )(implicitly[Writer[JsObject]], mongoFormat, implicitly[ExecutionContext])
  }

  def upsertBusinessEntity(vatNumber: String, businessEntity: BusinessEntity): Future[UpdateWriteResult] = {
    collection.update(
      selector = Json.obj(idKey -> vatNumber),
      update = Json.obj("$set" -> businessEntity),
      upsert = false
    ) filter (_.n == 1)
  }

  def upsertCtReference(vatNumber: String, ctReference: String): Future[UpdateWriteResult] =
    upsert(vatNumber, ctReferenceKey, ctReference)

  def upsertEmail(vatNumber: String, email: String): Future[UpdateWriteResult] =
    upsert(vatNumber, emailKey, email)

  def upsertTransactionEmail(vatNumber: String, transactionEmail: String): Future[UpdateWriteResult] =
    upsert(vatNumber, transactionEmailKey, transactionEmail)

  def upsertEmailVerificationStatus(vatNumber: String, emailVerified: Boolean): Future[UpdateWriteResult] =
    collection.update(
      selector = Json.obj(idKey -> vatNumber),
      update = Json.obj("$set" -> Json.obj(
        emailVerifiedKey -> emailVerified
      )),
      upsert = false
    ).filter(_.n == 1)

  def upsertContactPreference(vatNumber: String, contactPreference: ContactPreference): Future[WriteResult] =
    collection.update(
      selector = Json.obj(idKey -> vatNumber),
      update = Json.obj("$set" -> Json.obj(
        contactPreferenceKey -> contactPreference
      )),
      upsert = false
    ).filter(_.n == 1)

  def deleteRecord(vatNumber: String): Future[WriteResult] =
    collection.remove(selector = Json.obj(idKey -> vatNumber))

  private lazy val ttlIndex = Index(
    Seq((creationTimestampKey, IndexType(Ascending.value))),
    name = Some("subscriptionRequestExpires"),
    unique = false,
    background = false,
    dropDups = false,
    sparse = false,
    version = None,
    options = BSONDocument("expireAfterSeconds" -> appConfig.timeToLiveSeconds)
  )

  private def setIndex(): Unit = {
    collection.indexesManager.drop(ttlIndex.name.get) onComplete {
      _ => collection.indexesManager.ensure(ttlIndex)
    }
  }

  setIndex()

  override def drop(implicit ec: ExecutionContext): Future[Boolean] =
    collection.drop(failIfNotFound = false).map { r =>
      setIndex()
      r
    }

}
