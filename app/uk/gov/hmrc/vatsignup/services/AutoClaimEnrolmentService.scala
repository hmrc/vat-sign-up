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

package uk.gov.hmrc.vatsignup.services

import cats.Functor
import cats.data.EitherT
import cats.implicits._

import javax.inject.{Inject, Singleton}
import play.api.Logger
import play.api.libs.json.Json
import play.api.mvc.Request
import uk.gov.hmrc.auth.core.Admin
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}
import uk.gov.hmrc.vatsignup.connectors.{EnrolmentStoreProxyConnector, UsersGroupsSearchConnector}
import uk.gov.hmrc.vatsignup.httpparsers.GetUsersForGroupHttpParser.UsersFound
import uk.gov.hmrc.vatsignup.httpparsers.{AllocateEnrolmentResponseHttpParser, _}
import uk.gov.hmrc.vatsignup.models.monitoring.AutoClaimEnrolementAuditing.AutoClaimEnrolementAuditingModel
import uk.gov.hmrc.vatsignup.services.AutoClaimEnrolmentService._
import uk.gov.hmrc.vatsignup.services.monitoring.AuditService

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AutoClaimEnrolmentService @Inject()(enrolmentStoreProxyConnector: EnrolmentStoreProxyConnector,
                                          checkEnrolmentAllocationService: CheckEnrolmentAllocationService,
                                          assignEnrolmentToUserService: AssignEnrolmentToUserService,
                                          usersGroupsSearchConnector: UsersGroupsSearchConnector,
                                          auditService: AuditService
                                         )(implicit ec: ExecutionContext) {

  def autoClaimEnrolment(vatNumber: String,
                         triggerPoint: String
                        )(implicit hc: HeaderCarrier,
                          request: Request[_]): Future[AutoClaimEnrolmentResponse] = {
    for {
      _ <- getMtdvatEnrolmentAllocation(vatNumber)
        .logLeft(vatNumber, triggerPoint, call = getMtdvatEnrolmentAllocationCall)
      legacyVatGroupId <- getLegacyEnrolmentAllocation(vatNumber)
        .logLeft(vatNumber, triggerPoint, call = getLegacyEnrolmentAllocationCall)
      legacyVatUserIds <- getLegacyEnrolmentUserIDs(vatNumber)
        .logLeft(vatNumber, triggerPoint, call = getLegacyEnrolmentUserIDsCall, Some(legacyVatGroupId))
      adminUserId <- EitherT(getAdminUserId(legacyVatGroupId, legacyVatUserIds))
        .logLeft(vatNumber, triggerPoint, call = getAdminUserIdCall, Some(legacyVatGroupId), legacyVatUserIds)
      _ <- allocateEnrolmentWithoutKnownFacts(vatNumber, legacyVatGroupId, adminUserId)
        .logLeft(vatNumber, triggerPoint, call = allocateEnrolmentWithoutKnownFactsCall, Some(legacyVatGroupId), legacyVatUserIds)
      _ <- assignEnrolmentToUser(legacyVatUserIds filterNot (_ == adminUserId), vatNumber)
        .logLeft(vatNumber, triggerPoint, call = assignEnrolmentToUserCall, Some(legacyVatGroupId), legacyVatUserIds)
      _ = audit(vatNumber, triggerPoint, isSuccess = true, None, Some(legacyVatGroupId), legacyVatUserIds, None)
      _ = log(vatNumber, triggerPoint, isSuccess = true, None, groupIdFound = true, legacyVatUserIds.size)
    } yield EnrolmentAssigned
  }.value

  private implicit class LeftWithLogging[F[_], A, B](value: EitherT[F, A, B]) {
    def logLeft(vatNumber: String,
                triggerPoint: String,
                call: String,
                groupId: Option[String] = None,
                userIds: Set[String] = Set.empty
               )(implicit F: Functor[F],
                 hc: HeaderCarrier,
                 request: Request[_]): EitherT[F, A, B] =
      value.leftMap {
        error =>
          val isSuccess = if (error == EnrolmentNotAllocated || error == NoUsersFound) true else false

          audit(vatNumber, triggerPoint, isSuccess, Some(call), groupId, userIds, Some(error.toString))

          log(vatNumber, triggerPoint, isSuccess, Some(call), groupId.isDefined, userIds.size)

          error
      }
  }

  private def audit(vatNumber: String,
                    triggerPoint: String,
                    isSuccess: Boolean,
                    call: Option[String],
                    groupId: Option[String],
                    userIds: Set[String],
                    auditInformation: Option[String]
                   )(implicit headerCarrier: HeaderCarrier,
                     request: Request[_]): Unit = {

    auditService.audit(AutoClaimEnrolementAuditingModel(
      vatNumber,
      triggerPoint,
      isSuccess,
      call,
      groupId,
      userIds,
      auditInformation
    ))
  }

  private def log(vatNumber: String,
                  triggerPoint: String,
                  isSuccess: Boolean,
                  call: Option[String],
                  groupIdFound: Boolean,
                  numberOfIDs: Int): Unit = {

    val logString = Json.obj(
      "callBack" -> autoClaimEnrolmentService,
      "vatNumber" -> vatNumber,
      "triggerPoint" -> triggerPoint,
      "isSuccess" -> isSuccess,
      "call" -> call,
      "groupIdFound" -> groupIdFound,
      "numberOfIds" -> numberOfIDs
    ).toString()

    if (isSuccess) {
      Logger.info(logString)
    } else {
      Logger.error(logString)
    }
  }

  private def getLegacyEnrolmentAllocation(vatNumber: String)
                                          (implicit hc: HeaderCarrier): EitherT[Future, AutoClaimEnrolmentFailure, String] =
    EitherT(checkEnrolmentAllocationService.getGroupIdForLegacyVatEnrolment(vatNumber)).transform {
      case Right(_) =>
        Left(EnrolmentNotAllocated)
      case Left(CheckEnrolmentAllocationService.EnrolmentAlreadyAllocated(groupId)) =>
        Right(groupId)
      case Left(CheckEnrolmentAllocationService.UnexpectedEnrolmentStoreProxyFailure(status)) =>
        Left(EnrolmentStoreProxyFailure(status))
    }

  private def getMtdvatEnrolmentAllocation(vatNumber: String)
                                          (implicit hc: HeaderCarrier): EitherT[Future, AutoClaimEnrolmentFailure, CheckEnrolmentAllocationService.EnrolmentNotAllocated.type] =
    EitherT(checkEnrolmentAllocationService.getGroupIdForMtdVatEnrolment(vatNumber, ignoreAssignments = true)).leftMap {
      case CheckEnrolmentAllocationService.EnrolmentAlreadyAllocated(_) =>
        EnrolmentAlreadyAllocated
      case CheckEnrolmentAllocationService.UnexpectedEnrolmentStoreProxyFailure(status) =>
        EnrolmentStoreProxyFailure(status)
    }

  private def getLegacyEnrolmentUserIDs(vatNumber: String)(implicit hc: HeaderCarrier): EitherT[Future, AutoClaimEnrolmentFailure, Set[String]] =
    EitherT(enrolmentStoreProxyConnector.getUserIds(vatNumber)).transform {
      case Right(QueryUsersHttpParser.UsersFound(retrievedUserIds)) if retrievedUserIds.nonEmpty =>
        Right(retrievedUserIds)
      case Right(QueryUsersHttpParser.NoUsersFound) =>
        Left(NoUsersFound)
      case _ =>
        Left(EnrolmentStoreProxyConnectionFailure)
    }

  private def getAdminUserId(groupId: String, legacyVatUserIds: Set[String])(implicit hc: HeaderCarrier): Future[Either[AutoClaimEnrolmentFailure, String]] =
    usersGroupsSearchConnector.getUsersForGroup(groupId).map {
      case Right(UsersFound(userIds)) =>
        userIds.collectFirst {
          case (userId, Admin) if legacyVatUserIds.contains(userId) => userId
        } match {
          case Some(userId) => Right(userId)
          case None => Left(NoAdminUsers)
        }
      case Left(_) =>
        Left(UsersGroupsSearchFailure)
    }

  private def allocateEnrolmentWithoutKnownFacts(vatNumber: String,
                                                 groupId: String,
                                                 credentialId: String
                                                )(implicit hc: HeaderCarrier): EitherT[Future, AutoClaimEnrolmentFailure, AutoClaimEnrolmentSuccess] =
    EitherT(enrolmentStoreProxyConnector.allocateEnrolmentWithoutKnownFacts(
      groupId = groupId,
      credentialId = credentialId,
      vatNumber = vatNumber
    )).transform {
      case Right(AllocateEnrolmentResponseHttpParser.EnrolSuccess) =>
        Right(AutoClaimEnrolmentService.EnrolSuccess)
      case Left(AllocateEnrolmentResponseHttpParser.MultipleEnrolmentsInvalid) =>
        Left(AutoClaimEnrolmentService.MultipleEnrolmentInvalid)
      case Left(AllocateEnrolmentResponseHttpParser.UnexpectedEnrolFailure(message)) =>
        throw new InternalServerException(s"Failed to enrol user with CredentialId: $credentialId, due to $message")
    }

  private def assignEnrolmentToUser(credentialIds: Set[String], vatNumber: String)
                                   (implicit hc: HeaderCarrier): EitherT[Future, AutoClaimEnrolmentFailure, AutoClaimEnrolmentSuccess] =
    EitherT(assignEnrolmentToUserService.assignEnrolment(
      userIds = credentialIds,
      vatNumber = vatNumber
    )).transform {
      case Right(AssignEnrolmentToUserService.EnrolmentAssignedToUsers) =>
        Right(AutoClaimEnrolmentService.EnrolmentAssigned)
      case Left(AssignEnrolmentToUserService.EnrolmentAssignmentFailed(failedIds)) =>
        Left(AutoClaimEnrolmentService.EnrolmentAssignmentFailureForIds(failedIds))
    }

}

object AutoClaimEnrolmentService {

  val getLegacyEnrolmentAllocationCall: String = "getLegacyEnrolmentAllocation"
  val getMtdvatEnrolmentAllocationCall: String = "getMtdvatEnrolmentAllocation"
  val getLegacyEnrolmentUserIDsCall: String = "getLegacyEnrolmentUserIDs"
  val getAdminUserIdCall: String = "getAdminUserId"
  val allocateEnrolmentWithoutKnownFactsCall: String = "allocateEnrolmentWithoutKnownFacts"
  val assignEnrolmentToUserCall: String = "assignEnrolmentToUser"

  val autoClaimEnrolmentService: String = "AutoClaimEnrolmentService"

  val agentLedSignUp: String = "Agent sign-up"
  val bulkMigration: String = "Bulk Migration"

  type AutoClaimEnrolmentResponse = Either[AutoClaimEnrolmentFailure, AutoClaimEnrolmentSuccess]

  sealed trait AutoClaimEnrolmentSuccess

  case object EnrolSuccess extends AutoClaimEnrolmentSuccess

  case object EnrolmentAssigned extends AutoClaimEnrolmentSuccess

  sealed trait AutoClaimEnrolmentFailure

  case object EnrolmentAlreadyAllocated extends AutoClaimEnrolmentFailure

  case object NoUsersFound extends AutoClaimEnrolmentFailure

  case object EnrolmentNotAllocated extends AutoClaimEnrolmentFailure

  case object InvalidVatNumber extends AutoClaimEnrolmentFailure

  case object KnownFactsFailure extends AutoClaimEnrolmentFailure

  case object VatNumberNotFound extends AutoClaimEnrolmentFailure

  case object MultipleEnrolmentInvalid extends AutoClaimEnrolmentFailure

  case class EnrolmentStoreProxyFailure(status: Int) extends AutoClaimEnrolmentFailure

  case object EnrolmentStoreProxyConnectionFailure extends AutoClaimEnrolmentFailure

  case class EnrolmentAssignmentFailureForIds(failedIds: Set[String]) extends AutoClaimEnrolmentFailure

  case object UsersGroupsSearchFailure extends AutoClaimEnrolmentFailure

  case object NoAdminUsers extends AutoClaimEnrolmentFailure

}