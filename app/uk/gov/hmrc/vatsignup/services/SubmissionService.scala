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

package uk.gov.hmrc.vatsignup.services

import cats.data._
import cats.implicits._
import javax.inject.{Inject, Singleton}
import play.api.mvc.Request
import uk.gov.hmrc.auth.core.Enrolments
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.vatsignup.config.AppConfig
import uk.gov.hmrc.vatsignup.config.featureswitch.HybridSolution
import uk.gov.hmrc.vatsignup.connectors.{CustomerSignUpConnector, RegistrationConnector, TaxEnrolmentsConnector}
import uk.gov.hmrc.vatsignup.httpparsers.RegisterWithMultipleIdentifiersHttpParser.RegisterWithMultipleIdsSuccess
import uk.gov.hmrc.vatsignup.httpparsers.TaxEnrolmentsHttpParser.SuccessfulTaxEnrolment
import uk.gov.hmrc.vatsignup.models._
import uk.gov.hmrc.vatsignup.models.monitoring.RegisterWithMultipleIDsAuditing.RegisterWithMultipleIDsAuditModel
import uk.gov.hmrc.vatsignup.models.monitoring.SignUpAuditing.SignUpAuditModel
import uk.gov.hmrc.vatsignup.repositories.SubscriptionRequestRepository
import uk.gov.hmrc.vatsignup.services.SubmissionService._
import uk.gov.hmrc.vatsignup.services.monitoring.AuditService
import uk.gov.hmrc.vatsignup.utils.EnrolmentUtils._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SubmissionService @Inject()(subscriptionRequestRepository: SubscriptionRequestRepository,
                                  customerSignUpConnector: CustomerSignUpConnector,
                                  registrationConnector: RegistrationConnector,
                                  taxEnrolmentsConnector: TaxEnrolmentsConnector,
                                  auditService: AuditService,
                                  appConfig: AppConfig
                                 )(implicit ec: ExecutionContext) {

  def submitSignUpRequest(signUpRequest: SignUpRequest,
                          enrolments: Enrolments
                         )(implicit hc: HeaderCarrier,
                           request: Request[_]): Future[SignUpRequestSubmissionResponse] = {

    val optAgentReferenceNumber = enrolments.agentReferenceNumber
    val email = signUpRequest.signUpEmail map {
      _.emailAddress
    }
    val transactionEmail = signUpRequest.transactionEmail.emailAddress
    val isSignUpVerified = signUpRequest.signUpEmail map {
      _.isVerified
    }
    val isPartialMigration = !signUpRequest.isMigratable

    val contactPreference = signUpRequest.contactPreference

    val result = for {
      safeId <- registerBusinessEntity(signUpRequest.vatNumber, signUpRequest.businessEntity, optAgentReferenceNumber)
      _ <- signUp(safeId, signUpRequest.vatNumber, email, transactionEmail, isSignUpVerified, optAgentReferenceNumber, isPartialMigration, contactPreference)
      _ <- registerEnrolment(signUpRequest.vatNumber, safeId)
    } yield SignUpRequestSubmitted

    result.value
  }

  private def registerBusinessEntity(vatNumber: String,
                                     businessEntity: BusinessEntity,
                                     agentReferenceNumber: Option[String]
                                    )(implicit hc: HeaderCarrier, request: Request[_]): EitherT[Future, SignUpRequestSubmissionFailure, String] =
    EitherT(registrationConnector.registerBusinessEntity(vatNumber, businessEntity)) bimap( { _ =>
      auditService.audit(RegisterWithMultipleIDsAuditModel(vatNumber, businessEntity, agentReferenceNumber, isSuccess = false))
      RegistrationFailure
    }, {
      case RegisterWithMultipleIdsSuccess(safeId) =>
        auditService.audit(RegisterWithMultipleIDsAuditModel(vatNumber, businessEntity, agentReferenceNumber, isSuccess = true))
        safeId
    })

  private def signUp(safeId: String,
                     vatNumber: String,
                     emailAddress: Option[String],
                     transactionEmail: String,
                     emailAddressVerified: Option[Boolean],
                     agentReferenceNumber: Option[String],
                     isPartialMigration: Boolean,
                     contactPreference: Option[ContactPreference]
                    )(implicit hc: HeaderCarrier, request: Request[_]): EitherT[Future, SignUpRequestSubmissionFailure, CustomerSignUpResponseSuccess.type] =
    EitherT(customerSignUpConnector.signUp(
      safeId,
      vatNumber,
      emailAddress,
      emailAddressVerified,
      optIsPartialMigration = if (appConfig.isEnabled(HybridSolution)) Some(isPartialMigration) else None,
      contactPreference
    )) bimap( {
      _ => {
        auditService.audit(SignUpAuditModel(
          safeId = safeId,
          vatNumber = vatNumber,
          emailAddress = emailAddress,
          transactionEmail = transactionEmail,
          emailAddressVerified = emailAddressVerified,
          agentReferenceNumber = agentReferenceNumber,
          isSuccess = false,
          contactPreference = contactPreference
        ))
        SignUpFailure
      }
    }, {
      customerSignUpSuccess => {
        auditService.audit(SignUpAuditModel(
          safeId = safeId,
          vatNumber = vatNumber,
          emailAddress = emailAddress,
          transactionEmail = transactionEmail,
          emailAddressVerified = emailAddressVerified,
          agentReferenceNumber = agentReferenceNumber,
          isSuccess = true,
          contactPreference = contactPreference
        ))
        customerSignUpSuccess
      }
    })

  private def registerEnrolment(
                                 vatNumber: String,
                                 safeId: String
                               )(implicit hc: HeaderCarrier): EitherT[Future, SignUpRequestSubmissionFailure, SuccessfulTaxEnrolment.type] = {
    EitherT(taxEnrolmentsConnector.registerEnrolment(vatNumber, safeId)) leftMap {
      _ => EnrolmentFailure
    }
  }

}

object SubmissionService {

  type SignUpRequestSubmissionResponse = Either[SignUpRequestSubmissionFailure, SignUpRequestSubmitted.type]

  case object SignUpRequestSubmitted

  sealed trait SignUpRequestSubmissionFailure

  case object SignUpFailure extends SignUpRequestSubmissionFailure

  case object RegistrationFailure extends SignUpRequestSubmissionFailure

  case object EnrolmentFailure extends SignUpRequestSubmissionFailure

}
