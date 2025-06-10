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
import models.ServiceErrors.{Downstream_Error, Not_Allowed}
import models.{ApiErrorResponses, RequestData, ServiceErrors}
import play.api.Logging
import play.api.mvc.*
import services.SelfAssessmentService
import uk.gov.hmrc.auth.core.AffinityGroup.{Agent, Individual, Organisation}
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals.affinityGroup
import uk.gov.hmrc.auth.core.*
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import utils.constants.EnrolmentConstants.*

import javax.inject.Singleton
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AuthenticateRequestController(
    cc: ControllerComponents,
    selfAssessmentService: SelfAssessmentService,
    override val authConnector: AuthConnector
)(implicit appConfig: AppConfig, ec: ExecutionContext)
    extends BackendController(cc)
    with AuthorisedFunctions with Logging{

  def authorisedAction(
      utr: String
  )(block: RequestData[AnyContent] => Future[Result]): Action[AnyContent] = {
    Action.async(cc.parsers.anyContent) { implicit request =>
      implicit val headerCarrier: HeaderCarrier = hc(request)
      authorised(selfAssessmentEnrolments(utr)).retrieve(affinityGroup){
          case Some(Agent) if appConfig.agentsAllowed  =>  authoriseCaller(true, utr, block)
          case Some(Agent) if !appConfig.agentsAllowed  =>  Future.successful(Unauthorized(ApiErrorResponses(Not_Allowed.toString, "There were issues with the auth token provided").asJson))
          case _ => block(RequestData(utr, None, request))
          }.recoverWith {
          case _: NoActiveSession =>
            Future.successful(
              Unauthorized(ApiErrorResponses(Not_Allowed.toString, "There were issues with the auth token provided").asJson)
            )
          case _ : AuthorisationException if affinityGroup != Some(Agent)  => authoriseCaller(false, utr, block)
          case _ : AuthorisationException => Future.successful(Unauthorized(ApiErrorResponses(Not_Allowed.toString, "you shall not pass").asJson))
              }
        }
  }

  private def authoriseCaller(isAgent: Boolean, utr: String, block: RequestData[AnyContent] => Future[Result])(implicit hc: HeaderCarrier, request: Request[AnyContent]): Future[Result] = {
    selfAssessmentService.getMtdIdFromUtr(utr).flatMap { mtdId =>
      logger.info(s"mtd id $mtdId fetched")
      val authorisedFunction: AuthorisedFunction = if (isAgent) {
        authorised(agentDelegatedEnrolments(utr, mtdId))
      } else {
        authorised(checkForMtdEnrolment(mtdId))
      }
      authorisedFunction{
          block(RequestData(utr, Some(mtdId), request))
        }.recoverWith {
          case _: AuthorisationException => Future.successful(Unauthorized(ApiErrorResponses(Downstream_Error.toString, "Authorisation failed for the utr provided").asJson))
        }
      }
    }.recoverWith { case _: ServiceErrors => Future.successful(InternalServerError(ApiErrorResponses(Downstream_Error.toString, s"unexpected response when trying to fetch mtdID for utr $utr").asJson))}




  private def selfAssessmentEnrolments(utr: String): Predicate = {
    (Individual and Enrolment(IR_SA_Enrolment_Key).withIdentifier(IR_SA_Identifier, utr)) or
      (Organisation and Enrolment(IR_SA_Enrolment_Key).withIdentifier(IR_SA_Identifier, utr)) or
      (Agent and Enrolment(ASA_Enrolment_Key))
  }

  private def checkForMtdEnrolment(mtdId: String): Predicate = {
    (Individual and Enrolment(Mtd_Enrolment_Key).withIdentifier(Mtd_Identifier, mtdId)) or
      (Organisation and Enrolment(Mtd_Enrolment_Key).withIdentifier(Mtd_Identifier, mtdId))
  }

  private def agentDelegatedEnrolments(utr: String, mtdId: String): Predicate = {
    Enrolment(Mtd_Enrolment_Key)
      .withIdentifier(Mtd_Identifier, mtdId)
      .withDelegatedAuthRule(Mtd_Delegated_Auth_Rule) or
      Enrolment(IR_SA_Enrolment_Key)
        .withIdentifier(IR_SA_Identifier, utr)
        .withDelegatedAuthRule(IR_SA_Delegated_Auth_Rule)
  }
}
