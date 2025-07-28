/*
 * Copyright 2025 HM Revenue & Customs
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

package controllers

import models.ServiceErrors.{Downstream_Error, Service_Currently_Unavailable}
import models.{ApiErrorResponses, RequestData}
import play.api.Logging
import play.api.mvc.*
import services.SelfAssessmentService
import uk.gov.hmrc.auth.core.*
import uk.gov.hmrc.auth.core.AffinityGroup.{Agent, Individual, Organisation}
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals.{affinityGroup, confidenceLevel}
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import utils.FutureConverter.FutureOps
import utils.SelfAssessmentEnrolments.*
import utils.UtrValidator
import utils.constants.ErrorMessageConstansts.{
  badRequestMessage,
  forbiddenMessage,
  internalErrorMEssage,
  serviceUnavailableMessage,
  unauthorisedMessage
}

import javax.inject.Singleton
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AuthenticateRequestController(
    cc: ControllerComponents,
    selfAssessmentService: SelfAssessmentService,
    override val authConnector: AuthConnector
)(implicit ec: ExecutionContext)
    extends BackendController(cc)
    with AuthorisedFunctions
    with Logging {

  private val minimumConfidence = ConfidenceLevel.L250

  def authorisedAction(
      utr: String
  )(block: RequestData[AnyContent] => Future[Result]): Action[AnyContent] = {
    Action.async(cc.parsers.anyContent) { implicit request =>
      implicit val headerCarrier: HeaderCarrier = hc(request)
      val isUtrValid = UtrValidator.isValidUtr(utr)
      if (isUtrValid) {
        authorised()
          .retrieve(affinityGroup and confidenceLevel) {
            case Some(Individual) ~ userConfidence =>
              dealWithSelfAssessmentIndividual(userConfidence, utr)(request, block)
            case Some(Organisation) ~ _ => dealWithNonAgentAffinity(utr)(request, block)
            case Some(Agent) ~ _        => authenticateAgent(utr)(request, block)
          }
          .recover {
            case _: NoActiveSession =>
              BadRequest(ApiErrorResponses(badRequestMessage).asJson)
            case _ => ServiceUnavailable(ApiErrorResponses(serviceUnavailableMessage).asJson)
          }
      } else {
        Future.successful(BadRequest(ApiErrorResponses(badRequestMessage).asJson))
      }
    }

  }

  private def dealWithSelfAssessmentIndividual(confidenceLevel: ConfidenceLevel, utr: String)(
      implicit
      request: Request[AnyContent],
      block: RequestData[AnyContent] => Future[Result]
  ): Future[Result] = {
    if (confidenceLevel < minimumConfidence) {
      Unauthorized(ApiErrorResponses(unauthorisedMessage).asJson).toFuture
    } else {
      dealWithNonAgentAffinity(utr)
    }
  }

  private def dealWithNonAgentAffinity(utr: String)(implicit
      request: Request[AnyContent],
      block: RequestData[AnyContent] => Future[Result],
      hc: HeaderCarrier
  ): Future[Result] = {
    authorised(legacySaEnrolment(utr)) {
      block(RequestData(utr, None, request))
    }.recoverWith { case _: AuthorisationException =>
      selfAssessmentService
        .getMtdIdFromUtr(utr)
        .flatMap { mtdId =>
          authorised(mtdSaEnrolment(mtdId)) {
            block(RequestData(utr, None, request))
          }
        }
        .recoverWith {
          case _: AuthorisationException =>
            Forbidden(ApiErrorResponses(forbiddenMessage).asJson).toFuture
          case Downstream_Error =>
            InternalServerError(ApiErrorResponses(internalErrorMEssage).asJson).toFuture
          case _ =>
            ServiceUnavailable(ApiErrorResponses(serviceUnavailableMessage).asJson).toFuture
        }
    }
  }

  private def authenticateAgent(utr: String)(implicit
      request: Request[AnyContent],
      block: RequestData[AnyContent] => Future[Result],
      hc: HeaderCarrier
  ): Future[Result] = {
    authorised(principleAgentEnrolments) {
      selfAssessmentService
        .getMtdIdFromUtr(utr)
        .flatMap { mtdId =>
          authorised(delegatedEnrolments(utr, mtdId)) {
            block(RequestData(utr, None, request))
          }
        }
        .recoverWith {
          case _: AuthorisationException =>
            Forbidden(ApiErrorResponses(forbiddenMessage).asJson).toFuture
          case Downstream_Error =>
            InternalServerError(ApiErrorResponses(internalErrorMEssage).asJson).toFuture
          case _ =>
            ServiceUnavailable(ApiErrorResponses(serviceUnavailableMessage).asJson).toFuture
        }

    }.recoverWith { case _: AuthorisationException =>
      Forbidden(ApiErrorResponses(forbiddenMessage).asJson).toFuture
    }

  }

}
