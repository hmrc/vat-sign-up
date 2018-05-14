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

package uk.gov.hmrc.vatsignup.models

import play.api.libs.json._
import reactivemongo.play.json.ValidationError

sealed trait SubscriptionState

object SubscriptionState {

  case object Success extends SubscriptionState {
    val jsonName = "SUCCESS"
  }

  case object Failure extends SubscriptionState {
    val jsonName = "FAILURE"
  }

  implicit object SubscriptionStateJsonReads extends Reads[SubscriptionState] {
    override def reads(json: JsValue): JsResult[SubscriptionState] = {
      json.validate[String].flatMap {
        case Success.jsonName => JsSuccess(Success)
        case Failure.jsonName => JsSuccess(Failure)
        case invalidState => JsError(ValidationError(Seq(s"$invalidState is an invalid state"), Nil))
      }
    }
  }

}

