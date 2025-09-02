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

import models.ServiceErrors.Downstream_Error
import models.{ApiErrorResponses, RequestData}
import play.api.Logging
import play.api.mvc.*
import play.api.mvc.Results.*
import services.SelfAssessmentService
import uk.gov.hmrc.auth.core.*
import uk.gov.hmrc.auth.core.AffinityGroup.{Agent, Individual, Organisation}
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals.{affinityGroup, confidenceLevel}
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import utils.FutureConverter.FutureOps
import utils.SelfAssessmentEnrolments.*
import utils.UtrValidator
import utils.constants.ErrorMessageConstansts.*
import config.AppConfig
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AuthenticateRequestAction @Inject() (
    selfAssessmentService: SelfAssessmentService,
    authConnector: AuthConnector,
    bodyParsers: PlayBodyParsers
)(implicit ec: ExecutionContext, config: AppConfig)
    extends Logging {

  def apply(utr: String): ActionBuilder[RequestData, AnyContent] = {
    new AuthActionImpl(utr, selfAssessmentService, authConnector, bodyParsers)
  }

  private class AuthActionImpl(
      utr: String,
      selfAssessmentService: SelfAssessmentService,
      override val authConnector: AuthConnector,
      bodyParsers: PlayBodyParsers
  ) extends ActionBuilder[RequestData, AnyContent]
      with AuthorisedFunctions
      with Logging {

    override def parser: BodyParser[AnyContent] = bodyParsers.anyContent

    override protected def executionContext: ExecutionContext = ec

    override def invokeBlock[A](implicit
        request: Request[A],
        block: RequestData[A] => Future[Result]
    ): Future[Result] = {

      implicit val headerCarrier: HeaderCarrier = HeaderCarrierConverter.fromRequest(request)

      val isUtrValid = UtrValidator.isValidUtr(utr)
      if (isUtrValid) {
        authorised()
          .retrieve(affinityGroup and confidenceLevel) {
            case Some(Individual) ~ userConfidence =>
              dealWithSelfAssessmentIndividual(userConfidence, utr, request, block)
            case Some(Organisation) ~ _ =>
              dealWithNonAgentAffinity(utr, request, block)
            case Some(Agent) ~ _ =>
              authenticateAgent(utr, request, block)
          }
          .recover {
            case _: NoActiveSession =>
              BadRequest(ApiErrorResponses(BAD_REQUEST_RESPONSE).asJson)
            case _ =>
              ServiceUnavailable(ApiErrorResponses(SERVICE_UNAVAILABLE_RESPONSE).asJson)
          }
      } else {
        Future.successful(BadRequest(ApiErrorResponses(BAD_REQUEST_RESPONSE).asJson))
      }
    }

    private def dealWithSelfAssessmentIndividual[A](
        confidenceLevel: ConfidenceLevel,
        utr: String,
        request: Request[A],
        block: RequestData[A] => Future[Result]
    )(implicit hc: HeaderCarrier): Future[Result] = {
      if (confidenceLevel < config.confidenceLevel) {
        Unauthorized(ApiErrorResponses(UNAUTHORISED_RESPONSE).asJson).toFuture
      } else {
        dealWithNonAgentAffinity(utr, request, block)
      }
    }

    private def dealWithNonAgentAffinity[A](
        utr: String,
        request: Request[A],
        block: RequestData[A] => Future[Result]
    )(implicit hc: HeaderCarrier): Future[Result] = {
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
              Forbidden(ApiErrorResponses(FORBIDDEN_RESPONSE).asJson).toFuture
            case Downstream_Error =>
              InternalServerError(ApiErrorResponses(INTERNAL_ERROR_RESPONSE).asJson).toFuture
            case _ =>
              ServiceUnavailable(ApiErrorResponses(SERVICE_UNAVAILABLE_RESPONSE).asJson).toFuture
          }
      }
    }

    private def authenticateAgent[A](
        utr: String,
        request: Request[A],
        block: RequestData[A] => Future[Result]
    )(implicit hc: HeaderCarrier): Future[Result] = {
      authorised(principleAgentEnrolments) {
        authorised(delegatedLegacySaEnrolment(utr)) {
          block(RequestData(utr, None, request))
        }.recoverWith { case _: AuthorisationException =>
          selfAssessmentService
            .getMtdIdFromUtr(utr)
            .flatMap { mtdId =>
              authorised(delegatedMtdEnrolment(mtdId)) {
                block(RequestData(utr, None, request))
              }
            }
        }.recoverWith {
          case _: AuthorisationException =>
            Forbidden(ApiErrorResponses(FORBIDDEN_RESPONSE).asJson).toFuture
          case Downstream_Error =>
            InternalServerError(ApiErrorResponses(INTERNAL_ERROR_RESPONSE).asJson).toFuture
          case _ =>
            ServiceUnavailable(ApiErrorResponses(SERVICE_UNAVAILABLE_RESPONSE).asJson).toFuture
        }
      }
    }
  }
}
