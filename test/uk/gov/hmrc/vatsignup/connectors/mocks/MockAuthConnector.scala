/*
 * Copyright 2020 HM Revenue & Customs
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

package uk.gov.hmrc.vatsignup.connectors.mocks

import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatest.{BeforeAndAfterEach, Suite}
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.auth.core.authorise.{EmptyPredicate, Predicate}
import uk.gov.hmrc.auth.core.retrieve._
import uk.gov.hmrc.auth.core.{AuthConnector, ConfidenceLevel, Enrolment, Enrolments}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.vatsignup.helpers.TestConstants._

import scala.concurrent.{ExecutionContext, Future}

trait MockAuthConnector extends BeforeAndAfterEach with MockitoSugar {
  self: Suite =>

  val mockAuthConnector: AuthConnector = mock[AuthConnector]

  def mockAuthorise[T](predicate: Predicate = EmptyPredicate,
                       retrievals: Retrieval[T] = EmptyRetrieval
                      )(response: Future[T]): Unit = {
    when(
      mockAuthConnector.authorise(
        ArgumentMatchers.eq(predicate),
        ArgumentMatchers.eq(retrievals)
      )(
        ArgumentMatchers.any[HeaderCarrier],
        ArgumentMatchers.any[ExecutionContext])
    ) thenReturn response
  }

  def mockAuthRetrieveEnrolments(enrolments: Enrolment*): Unit =
    mockAuthorise(retrievals = Retrievals.allEnrolments)(Future.successful(Enrolments(enrolments.toSet)))

  def mockAuthRetrieveAgentEnrolment(): Unit =
    mockAuthorise(retrievals = Retrievals.allEnrolments)(Future.successful(Enrolments(Set(testAgentEnrolment))))

  def mockAuthRetrievePrincipalEnrolment(): Unit =
    mockAuthorise(retrievals = Retrievals.allEnrolments)(Future.successful(Enrolments(Set(testPrincipalEnrolment))))

  def mockAuthRetrieveConfidenceLevel(confidenceLevel: ConfidenceLevel): Unit =
    mockAuthorise(retrievals = Retrievals.confidenceLevel)(Future.successful(confidenceLevel))

  def mockAuthRetrieveCredentialAndGroupId(credentials: Credentials, groupId: Option[String]): Unit =
    mockAuthorise(EmptyPredicate, Retrievals.credentials and Retrievals.groupIdentifier)(Future.successful(
      new ~(credentials, groupId)
    ))

  def mockAuthRetrievePartnershipEnrolment(sautr: String = testUtr): Unit =
    mockAuthorise(
      retrievals = Retrievals.allEnrolments
    )(
      Future.successful(Enrolments(Set(testPartnershipEnrolment(sautr))))
    )

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockAuthConnector)
  }
}
