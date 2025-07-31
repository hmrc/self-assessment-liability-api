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
import models.ApiErrorResponses
import models.ServiceErrors.*
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents, Result}
import play.api.mvc.Results.*
import services.SelfAssessmentService
import uk.gov.hmrc.auth.core.AuthConnector
import utils.constants.ErrorMessageConstants.*
import utils.FutureConverter.FutureOps

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SelfAssessmentHistoryController @Inject() (
    override val authConnector: AuthConnector,
    val service: SelfAssessmentService,
    cc: ControllerComponents
)(implicit appConfig: AppConfig, ec: ExecutionContext)
    extends AuthenticateRequestController(cc, service, authConnector) {

  def getYourSelfAssessmentData(utr: String, fromDate: Option[String]): Action[AnyContent] =
    authorisedAction(utr) { implicit request =>
      service
        .getHipData(utr, fromDate.getOrElse("2025-04-06"))
        .flatMap { hipResponse =>
          Future.successful(Ok(Json.toJson(hipResponse)))
        }
        .recoverWith {
          handleFailedRequest
        }
    }

  private def handleFailedRequest: PartialFunction[Throwable, Future[Result]] = {
    case HIP_Unauthorised =>
      constructErrorResponse(Unauthorized, unauthorisedMessage)
    case HIP_Forbidden =>
      constructErrorResponse(Forbidden, forbiddenMessage)
    case No_Payments_Found_For_UTR =>
      constructErrorResponse(BadRequest, badRequestMessage)
    case HIP_Service_Unavailable =>
      constructErrorResponse(ServiceUnavailable, serviceUnavailableMessage)
    case Invalid_Correlation_Id | Invalid_UTR | HIP_Server_Error | HIP_Bad_Gateway |
        Downstream_Error | _ =>
      constructErrorResponse(InternalServerError, internalErrorMessage)
  }

  private def constructErrorResponse(status: Status, message: String): Future[Result] = {
    status(ApiErrorResponses(message).asJson).toFuture
  }
}
