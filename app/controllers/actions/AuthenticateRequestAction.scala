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

package controllers.actions

import config.AppConfig
import models.RequestWithUtr
import models.ServiceErrors.Unauthorised_Error
import play.api.Logging
import play.api.mvc.*
import services.SelfAssessmentService
import uk.gov.hmrc.auth.core.*
import uk.gov.hmrc.auth.core.AffinityGroup.{Agent, Individual, Organisation}
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals.{affinityGroup, confidenceLevel}
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import utils.SelfAssessmentEnrolments.*

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AuthenticateRequestAction @Inject() (
    selfAssessmentService: SelfAssessmentService,
    val authConnector: AuthConnector
)(implicit ec: ExecutionContext, config: AppConfig)
    extends ActionFilter[RequestWithUtr]
    with AuthorisedFunctions
    with Logging {

  override protected def executionContext: ExecutionContext = ec

  override protected def filter[A](request: RequestWithUtr[A]): Future[Option[Result]] = {
    implicit val headerCarrier: HeaderCarrier = HeaderCarrierConverter.fromRequest(request)
    authorised()
      .retrieve(affinityGroup and confidenceLevel) {
        case Some(Individual) ~ userConfidence =>
          dealWithSelfAssessmentIndividual(userConfidence, request)
        case Some(Organisation) ~ _ =>
          dealWithNonAgentAffinity(request)
        case Some(Agent) ~ _ =>
          authenticateAgent(request)
      }
  }

  private def dealWithSelfAssessmentIndividual[A](
      confidenceLevel: ConfidenceLevel,
      request: RequestWithUtr[A]
  )(implicit hc: HeaderCarrier): Future[Option[Result]] = {
    if (confidenceLevel < config.confidenceLevel) {
      logger.info(
        s"Authentication of individual failed as the minimum confidence level of ${config.confidenceLevel} not reached"
      )
      Future.failed(Unauthorised_Error)
    } else {
      dealWithNonAgentAffinity(request)
    }
  }

  private def dealWithNonAgentAffinity[A](
      request: RequestWithUtr[A]
  )(implicit hc: HeaderCarrier): Future[Option[Result]] = {
    authorised(legacySaEnrolment(request.utr)) {
      Future.successful(None)
    }.recoverWith { case _: AuthorisationException =>
      selfAssessmentService
        .getMtdIdFromUtr(request.utr)
        .flatMap { mtdId =>
          authorised(mtdSaEnrolment(mtdId)) {
            Future.successful(None)
          }
        }
    }
  }

  private def authenticateAgent[A](
      request: RequestWithUtr[A]
  )(implicit hc: HeaderCarrier): Future[Option[Result]] = {
    authorised(principleAgentEnrolments) {
      authorised(delegatedLegacySaEnrolment(request.utr)) {
        Future.successful(None)
      }.recoverWith { case _: AuthorisationException =>
        selfAssessmentService
          .getMtdIdFromUtr(request.utr, config.useStubsForAgentAuthorisation)
          .flatMap { mtdId =>
            authorised(delegatedMtdEnrolment(mtdId)) {
              Future.successful(None)
            }.recoverWith { case _: AuthorisationException =>
              logger.info("Agent authorisation failed as agent client relationship not established")
              throw Unauthorised_Error
            }
          }
      }
    }
  }
}
