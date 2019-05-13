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

import org.scalatest.EitherValues
import play.api.http.Status._
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.vatsignup.httpparsers.AssignEnrolmentToUserHttpParser._
import uk.gov.hmrc.vatsignup.httpparsers.AssignEnrolmentToUserHttpParser.AssignEnrolmentToUserHttpReads.read

class AssignEnrolmentToUserRequestHttpParserSpec extends UnitSpec with EitherValues {
  "AssignEnrolmentToUserHttpReads#read" when {
    "the response status is OK" should {
      "return an EnrolmentAssigned" in {
        val httpResponse = HttpResponse(
          responseStatus = CREATED
        )

        read("", "", httpResponse).right.value shouldBe EnrolmentAssigned
      }
    }

    "the response status is anything else" should {
      "return an InvalidJsonResponse" in {
        val httpResponse = HttpResponse(
            responseStatus = INTERNAL_SERVER_ERROR
        )

        read("", "", httpResponse).left.value shouldBe EnrolmentAssignmentFailure(INTERNAL_SERVER_ERROR, httpResponse.body)
      }
    }

  }
}
