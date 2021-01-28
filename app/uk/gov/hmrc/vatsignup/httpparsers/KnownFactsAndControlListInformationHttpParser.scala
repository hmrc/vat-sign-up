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

package uk.gov.hmrc.vatsignup.httpparsers

import play.api.http.Status._
import uk.gov.hmrc.http.{HttpReads, HttpResponse, InternalServerException}
import uk.gov.hmrc.vatsignup.config.featureswitch.FeatureSwitching
import uk.gov.hmrc.vatsignup.models.{KnownFactsAndControlListInformation, VatKnownFacts}
import uk.gov.hmrc.vatsignup.utils.controllist.ControlListInformationParser

object KnownFactsAndControlListInformationHttpParser extends FeatureSwitching {
  type KnownFactsAndControlListInformationHttpParserResponse = Either[KnownFactsAndControlListInformationFailure, KnownFactsAndControlListInformation]

  val postcodeKey = "postcode"
  val registrationDateKey = "dateOfReg"
  val lastReturnMonthPeriodKey = "lastReturnMonthPeriod"
  val lastNetDueKey = "lastNetDue"
  val controlListInformationKey = "controlListInformation"

  val invalidJsonResponseMessage = "Invalid JSON response"

  val lastReturnMonthDefault: String = "N/A"
  val lastNetDueDefault: String = "0.00"

  implicit object KnownFactsAndControlListInformationHttpReads extends HttpReads[KnownFactsAndControlListInformationHttpParserResponse] {
    override def read(method: String, url: String, response: HttpResponse): KnownFactsAndControlListInformationHttpParserResponse = {
      response.status match {
        case OK =>
          ControlListInformationParser.tryParse(getControlList(response)) match {
            case Right(validControlList) =>
              val lastMonthReturnPeriod = getLastReturnMonthPeriod(response) filterNot (_ == lastReturnMonthDefault) map VatKnownFacts.fromMMM
              val lastNetDue = getLastNetDue(response).map { x => "%.2f".format(x) } filterNot (_ == lastNetDueDefault && lastMonthReturnPeriod.isEmpty)
              Right(KnownFactsAndControlListInformation(
                VatKnownFacts(
                  businessPostcode = getBusinessPostcode(response),
                  vatRegistrationDate = getVatRegistrationDate(response),
                  lastReturnMonthPeriod = lastMonthReturnPeriod,
                  lastNetDue = lastNetDue
                ),
                controlListInformation = validControlList
              ))
            case Left(parsingError) =>
              Left(UnexpectedKnownFactsAndControlListInformationFailure(OK, parsingError.toString))
          }

        case BAD_REQUEST =>
          Left(KnownFactsInvalidVatNumber)
        case NOT_FOUND =>
          Left(ControlListInformationVatNumberNotFound)
        case status =>
          Left(UnexpectedKnownFactsAndControlListInformationFailure(status, response.body))
      }
    }

    private def getBusinessPostcode(response: HttpResponse): Option[String] =
      (response.json \ postcodeKey).validateOpt[String]
        .getOrElse(throw new InternalServerException(s"$invalidJsonResponseMessage: postcodeKey is missing in the json response"))

    private def getVatRegistrationDate(response: HttpResponse): String =
      (response.json \ registrationDateKey).validate[String]
        .getOrElse(throw new InternalServerException(s"$invalidJsonResponseMessage: registrationDateKey is missing in the json response"))

    private def getLastReturnMonthPeriod(response: HttpResponse): Option[String] =
      (response.json \ lastReturnMonthPeriodKey).validateOpt[String]
        .getOrElse(throw new InternalServerException(s"$invalidJsonResponseMessage: lastReturnMonthPeriodKey is missing in the json response"))

    private def getLastNetDue(response: HttpResponse): Option[Double] =
      (response.json \ lastNetDueKey).validateOpt[Double]
        .getOrElse(throw new InternalServerException(s"$invalidJsonResponseMessage: lastNetDue is missing in the json response"))

    private def getControlList(response: HttpResponse): String =
      (response.json \ controlListInformationKey).validate[String]
        .getOrElse(throw new InternalServerException(s"$invalidJsonResponseMessage: controlList is missing in the json response"))
  }

  sealed trait KnownFactsAndControlListInformationFailure

  case object KnownFactsInvalidVatNumber extends KnownFactsAndControlListInformationFailure

  case object ControlListInformationVatNumberNotFound extends KnownFactsAndControlListInformationFailure

  case class UnexpectedKnownFactsAndControlListInformationFailure(status: Int, body: String) extends KnownFactsAndControlListInformationFailure

}
