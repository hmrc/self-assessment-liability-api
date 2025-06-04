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
import models.ServiceErrors.{Downstream_Error, More_Than_One_NINO_Found_For_SAUTR, Not_Allowed}
import models.{ApiErrorResponses, RequestData}
import play.api.mvc.*
import services.SelfAssessmentService
import uk.gov.hmrc.auth.core.AffinityGroup.{Agent, Individual, Organisation}
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals.affinityGroup
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisationException, AuthorisedFunctions, Enrolment, MissingBearerToken}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import utils.constants.EnrolmentConstants.*

import javax.inject.Singleton
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AuthenticateRequestController(
    cc: ControllerComponents,
    selfAssessmentService : SelfAssessmentService,
    override val authConnector: AuthConnector
)(implicit appConfig: AppConfig, ec: ExecutionContext)
    extends BackendController(cc)
    with AuthorisedFunctions {

  def authorisedAction(utr: String)(block: RequestData[AnyContent] => Future[Result]): Action[AnyContent]= {
    Action.async(cc.parsers.anyContent) { request =>
      implicit val headerCarrier: HeaderCarrier = hc(request)

        authorised(selfAssessmentEnrolments(utr)) {
         block(RequestData(utr, None, request))
        }
          .recoverWith {
            case _: MissingBearerToken =>Future.successful(Unauthorized(ApiErrorResponses(Not_Allowed.toString, "missing auth token").asJson))
            case _: AuthorisationException =>
            selfAssessmentService.getMtdIdFromUtr(utr).flatMap { mtdId =>
              authorised(checkForMtdEnrolment(mtdId)).retrieve(affinityGroup) {
                case Some(Individual) =>
                  block(RequestData(utr, None, request))
                case Some(Organisation) =>
                  block(RequestData(utr, None, request))
                case Some(Agent) =>
                  if(appConfig.agentsAllowed){
                    authorised(agentDelegatedEnrolments(utr, mtdId)) {
                      block(RequestData(utr, None, request))
                    }.recoverWith{
                      case _: AuthorisationException =>Future.successful(InternalServerError(ApiErrorResponses(Downstream_Error.toString, "agent/client handshake was not established").asJson))
                      case error => Future.successful(InternalServerError(ApiErrorResponses(Downstream_Error.toString, "auth returned an error of some kind").asJson))
                    }
                  } else{
                    Future.successful(Unauthorized(ApiErrorResponses(Not_Allowed.toString, "Agents are currently not supported by our service").asJson))
                  }

                case _ => Future.successful(InternalServerError(ApiErrorResponses(Not_Allowed.toString, "unsupported affinity group").asJson))
              }.recoverWith{
                case _: AuthorisationException =>Future.successful(InternalServerError(ApiErrorResponses(Downstream_Error.toString, "user didnt have any of the self assessment enrolments").asJson))
                case error => Future.successful(InternalServerError(ApiErrorResponses(Downstream_Error.toString, "auth returned an error of some kind").asJson))
              }
            }.recoverWith {
              case More_Than_One_NINO_Found_For_SAUTR => Future.successful(InternalServerError(ApiErrorResponses(More_Than_One_NINO_Found_For_SAUTR.toString, "more than one nino found").asJson))
            }
            case error => Future.successful(InternalServerError(ApiErrorResponses(Downstream_Error.toString, "auth returned an error of some kind").asJson))
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
          (Agent and Enrolment("HMRC-AS-AGENT"))
      }

      private def agentDelegatedEnrolments(utr: String, mtdId: String): Predicate = {
        Enrolment(Mtd_Enrolment_Key)
          .withIdentifier(Mtd_Identifier, mtdId)
          .withDelegatedAuthRule(Mtd_Delegated_Auth_Rule) or
          Enrolment(IR_SA_Enrolment_Key).withIdentifier(IR_SA_Identifier, utr).withDelegatedAuthRule(IR_SA_Delegated_Auth_Rule)
      }
}