/*
 * Copyright 2019 HM Revenue & Customs
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
import uk.gov.hmrc.http.{HttpReads, HttpResponse}
import uk.gov.hmrc.vatsignup.config.featureswitch.{AdditionalKnownFacts, FeatureSwitching}
import uk.gov.hmrc.vatsignup.models.{KnownFactsAndControlListInformation, VatKnownFacts}
import uk.gov.hmrc.vatsignup.utils.controllist.ControlListInformationParser

object KnownFactsAndControlListInformationHttpParser  extends FeatureSwitching {
  type KnownFactsAndControlListInformationHttpParserResponse = Either[KnownFactsAndControlListInformationFailure, KnownFactsAndControlListInformation]

  val postcodeKey = "postcode"
  val registrationDateKey = "dateOfReg"
  val lastReturnMonthPeriodKey = "lastReturnMonthPeriod"
  val lastNetDueKey = "lastNetDue"
  val controlListInformationKey = "controlListInformation"

  val invalidJsonResponseMessage = "Invalid JSON response"

  val lastReturnMonthDefault: String = "N/A"
  val lastNetDueDefault:String = "0"

  implicit object KnownFactsAndControlListInformationHttpReads extends HttpReads[KnownFactsAndControlListInformationHttpParserResponse] {
    override def read(method: String, url: String, response: HttpResponse): KnownFactsAndControlListInformationHttpParserResponse = {
      response.status match {
        case OK  if isEnabled(AdditionalKnownFacts) =>
          (for {
            businessPostcode <- (response.json \ postcodeKey).validate[String]
            vatRegistrationDate <- (response.json \ registrationDateKey).validate[String]
            lastReturnMonthPeriod <- (response.json \ lastReturnMonthPeriodKey).validateOpt[String]
            lastNetDue <- (response.json \ lastNetDueKey).validateOpt[String]
            controlList <- (response.json \ controlListInformationKey).validate[String]
          } yield ControlListInformationParser.tryParse(controlList) match {
            case Right(validControlList) =>
              val lrmp = lastReturnMonthPeriod filterNot (_ == lastReturnMonthDefault)
              val lnd = lastNetDue filterNot (_ == lastNetDueDefault && lrmp.isEmpty)
              Right(KnownFactsAndControlListInformation(
                VatKnownFacts(
                  businessPostcode = businessPostcode,
                  vatRegistrationDate = vatRegistrationDate,
                  lastReturnMonthPeriod = lrmp,
                  lastNetDue = lnd
                ),
                controlListInformation = validControlList
              ))
            case Left(parsingError) =>
              Left(UnexpectedKnownFactsAndControlListInformationFailure(OK, parsingError.toString))
          }) getOrElse Left(
            UnexpectedKnownFactsAndControlListInformationFailure(OK, s"$invalidJsonResponseMessage ${response.body}")
          )

        case OK =>
          (for {
            businessPostcode <- (response.json \ postcodeKey).validate[String]
            vatRegistrationDate <- (response.json \ registrationDateKey).validate[String]
            controlList <- (response.json \ controlListInformationKey).validate[String]
          } yield ControlListInformationParser.tryParse(controlList) match {
            case Right(validControlList) =>
              Right(KnownFactsAndControlListInformation(
                VatKnownFacts(
                  businessPostcode = businessPostcode,
                  vatRegistrationDate = vatRegistrationDate,
                  lastReturnMonthPeriod = None,
                  lastNetDue = None
                ),
                controlListInformation = validControlList
              ))
            case Left(parsingError) =>
              Left(UnexpectedKnownFactsAndControlListInformationFailure(OK, parsingError.toString))
          }) getOrElse Left(
            UnexpectedKnownFactsAndControlListInformationFailure(OK, s"$invalidJsonResponseMessage ${response.body}")
          )

        case BAD_REQUEST =>
          Left(KnownFactsInvalidVatNumber)
        case NOT_FOUND =>
          Left(ControlListInformationVatNumberNotFound)
        case status =>
          Left(UnexpectedKnownFactsAndControlListInformationFailure(status, response.body))
      }
    }
  }


  sealed trait KnownFactsAndControlListInformationFailure

  case object KnownFactsInvalidVatNumber extends KnownFactsAndControlListInformationFailure

  case object ControlListInformationVatNumberNotFound extends KnownFactsAndControlListInformationFailure

  case class UnexpectedKnownFactsAndControlListInformationFailure(status: Int, body: String) extends KnownFactsAndControlListInformationFailure

}
