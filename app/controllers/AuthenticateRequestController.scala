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

import config.AppConfig
import models.{ApiErrorResponses, RequestData}
import play.api.mvc.*
import services.SelfAssessmentService
import uk.gov.hmrc.auth.core.AffinityGroup.{Agent, Individual, Organisation}
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals.{affinityGroup, confidenceLevel}
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.auth.core.*
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import utils.constants.EnrolmentConstants.*
import utils.constants.ErrorMessageConstants.*

import javax.inject.Singleton
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AuthenticateRequestController(
    cc: ControllerComponents,
    selfAssessmentService: SelfAssessmentService,
    override val authConnector: AuthConnector
)(implicit appConfig: AppConfig, ec: ExecutionContext)
    extends BackendController(cc)
    with AuthorisedFunctions {

  private val minimumConfidence = ConfidenceLevel.L250

  def authorisedAction(
      utr: String
  )(block: RequestData[AnyContent] => Future[Result]): Action[AnyContent] = {
    Action.async(cc.parsers.anyContent) { request =>
      implicit val headerCarrier: HeaderCarrier = hc(request)

      authorised(selfAssessmentEnrolments(utr))
        .retrieve(affinityGroup and confidenceLevel) {
          case Some(Individual) ~ userConfidence
              if userConfidence.level < minimumConfidence.level =>
            lowConfidenceResult
          case _ =>
            block(RequestData(utr, None, request))
        }
        .recoverWith {
          case _: MissingBearerToken =>
            Future.successful(
              Unauthorized(ApiErrorResponses(unauthorisedMessage).asJson)
            )
          case _: AuthorisationException =>
            selfAssessmentService
              .getMtdIdFromUtr(utr)
              .flatMap { mtdId =>
                authorised(checkForMtdEnrolment(mtdId))
                  .retrieve(affinityGroup and confidenceLevel) {
                    case Some(Individual) ~ userConfidence
                        if userConfidence.level < minimumConfidence.level =>
                      lowConfidenceResult
                    case Some(Individual) ~ _ =>
                      block(RequestData(utr, None, request))
                    case Some(Organisation) ~ _ =>
                      block(RequestData(utr, None, request))
                    case Some(Agent) ~ _ =>
                      if (appConfig.agentsAllowed) {
                        authorised(agentDelegatedEnrolments(utr, mtdId)) {
                          block(RequestData(utr, None, request))
                        }.recoverWith { case _: AuthorisationException =>
                          Future.successful(
                            InternalServerError(ApiErrorResponses(internalErrorMessage).asJson)
                          )
                        }
                      } else {
                        Future.successful(
                          Unauthorized(ApiErrorResponses(unauthorisedMessage).asJson)
                        )
                      }
                    case _ =>
                      Future.successful(
                        InternalServerError(ApiErrorResponses(internalErrorMessage).asJson)
                      )
                  }
                  .recoverWith { case _: AuthorisationException =>
                    Future.successful(
                      InternalServerError(ApiErrorResponses(internalErrorMessage).asJson)
                    )
                  }
              }
              .recoverWith { case error =>
                Future.successful(
                  InternalServerError(ApiErrorResponses(internalErrorMessage).asJson)
                )
              }
          case error =>
            Future.successful(
              InternalServerError(ApiErrorResponses(internalErrorMessage).asJson)
            )
        }
    }
  }

  private def selfAssessmentEnrolments(utr: String): Predicate = {
    (Individual and Enrolment(IR_SA_Enrolment_Key).withIdentifier(IR_SA_Identifier, utr)) or
      (Organisation and Enrolment(IR_SA_Enrolment_Key).withIdentifier(IR_SA_Identifier, utr))
  }

  private def checkForMtdEnrolment(mtdId: String): Predicate = {
    (Individual and Enrolment(Mtd_Enrolment_Key).withIdentifier(Mtd_Identifier, mtdId)) or
      (Organisation and Enrolment(Mtd_Enrolment_Key).withIdentifier(Mtd_Identifier, mtdId)) or
      (Agent and Enrolment(ASA_Enrolment_Key))
  }

  private def agentDelegatedEnrolments(utr: String, mtdId: String): Predicate = {
    Enrolment(Mtd_Enrolment_Key)
      .withIdentifier(Mtd_Identifier, mtdId)
      .withDelegatedAuthRule(Mtd_Delegated_Auth_Rule) or
      Enrolment(IR_SA_Enrolment_Key)
        .withIdentifier(IR_SA_Identifier, utr)
        .withDelegatedAuthRule(IR_SA_Delegated_Auth_Rule)
  }

  private val lowConfidenceResult: Future[Result] = {
    Future.successful(
      Unauthorized(ApiErrorResponses(unauthorisedMessage).asJson)
    )
  }
}
