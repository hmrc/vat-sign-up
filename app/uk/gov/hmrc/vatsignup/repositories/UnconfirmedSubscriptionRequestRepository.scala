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
import uk.gov.hmrc.vatsignup.models.UnconfirmedSubscriptionRequest._
import uk.gov.hmrc.vatsignup.models.{NinoSource, UnconfirmedSubscriptionRequest}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UnconfirmedSubscriptionRequestRepository @Inject()(mongo: ReactiveMongoComponent,
                                                         appConfig: AppConfig)(implicit ec: ExecutionContext)
  extends ReactiveRepository[UnconfirmedSubscriptionRequest, String](
    "unconfirmedSubscriptionRequestRepository",
    mongo.mongoConnector.db,
    UnconfirmedSubscriptionRequest.mongoFormat,
    implicitly[Format[String]]
  ) {

  def getRequestIdByCredential(credentialId: String): Future[Option[String]] = {
    collection.findAndUpdate(
      selector = Json.obj(credentialIdKey -> credentialId),
      update = Json.obj("$setOnInsert" -> Json.obj(
        idKey -> UUID.randomUUID().toString
      )),
      upsert = true,
      fetchNewObject = true
    ).map(_.result[UnconfirmedSubscriptionRequest].map(_.requestId))
  }


  private def upsert(requestId: String, elementKey: String, elementValue: String): Future[UpdateWriteResult] = {
    collection.update(
      selector = Json.obj(idKey -> requestId),
      update = Json.obj("$set" -> Json.obj(
        elementKey -> elementValue
      )),
      upsert = true
    ).filter(_.n == 1)
  }

  def upsertVatNumber(requestId: String, vatNumber: String): Future[UpdateWriteResult] = {
    collection.update(
      selector = Json.obj(
        idKey -> requestId
      ),
      update = UnconfirmedSubscriptionRequest(requestId = requestId, vatNumber = Some(vatNumber)),
      upsert = true
    )(implicitly[Writer[JsObject]], mongoFormat, implicitly[ExecutionContext])
  }

  def upsertCompanyNumber(requestId: String, companyNumber: String): Future[UpdateWriteResult] =
    collection.update(
      selector = Json.obj(idKey -> requestId),
      update = Json.obj("$set" -> Json.obj(
        companyNumberKey -> companyNumber
      ), "$unset" -> Json.obj(
        ninoKey -> "",
        ninoSourceKey -> ""
      )),
      upsert = true
    ).filter(_.n == 1)

  def upsertCtReference(requestId: String, ctReference: String): Future[UpdateWriteResult] =
    upsert(requestId = requestId, elementKey = ctReferenceKey, elementValue = ctReference)

  def upsertEmail(requestId: String, email: String): Future[UpdateWriteResult] =
    upsert(requestId = requestId, elementKey = emailKey, elementValue = email)

  def upsertTransactionEmail(requestId: String, transactionEmail: String): Future[UpdateWriteResult] =
    upsert(requestId = requestId, elementKey = transactionEmailKey, elementValue = transactionEmail)

  def upsertNino(requestId: String, nino: String, ninoSource: NinoSource): Future[UpdateWriteResult] =
    collection.update(
      selector = Json.obj(idKey -> requestId),
      update = Json.obj("$set" -> Json.obj(
        ninoKey -> nino,
        ninoSourceKey -> ninoSource,
        identityVerifiedKey -> false
      ), "$unset" -> Json.obj(
        companyNumberKey -> ""
      )),
      upsert = true
    ).filter(_.n == 1)

  def upsertIdentityVerified(requestId: String): Future[UpdateWriteResult] =
    collection.update(
      selector = Json.obj(idKey -> requestId),
      update = Json.obj("$set" -> Json.obj(
        identityVerifiedKey -> true
      )),
      upsert = true
    ).filter(_.n == 1)

  def deleteRecord(requestId: String): Future[WriteResult] =
    collection.remove(selector = Json.obj(idKey -> requestId))

  private lazy val ttlIndex = Index(
    Seq((creationTimestampKey, IndexType(Ascending.value))),
    name = Some("unconfirmedSubscriptionRequestExpires"),
    unique = false,
    background = false,
    dropDups = false,
    sparse = false,
    version = None,
    options = BSONDocument("expireAfterSeconds" -> appConfig.timeToLiveSeconds)
  )

  private lazy val credentialIdIndex = Index(
    Seq((credentialIdKey, IndexType(Ascending.value))),
    name = Some("credentialIdIndex"),
    unique = true,
    background = false,
    dropDups = false,
    sparse = true,
    version = None
  )

  private def setIndex(): Future[Unit] = {
    for {
      _ <- collection.indexesManager.create(ttlIndex)
      _ <- collection.indexesManager.create(credentialIdIndex)
    } yield Unit
  }

  setIndex()

  override def drop(implicit ec: ExecutionContext): Future[Boolean] =
    for {
      r <- collection.drop(failIfNotFound = false)
      _  <- setIndex()
    } yield r

}
