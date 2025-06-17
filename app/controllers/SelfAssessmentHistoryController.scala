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
import models.ServiceErrors.Invalid_SAUTR
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import services.SelfAssessmentService
import uk.gov.hmrc.auth.core.AuthConnector

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.matching.Regex

class SelfAssessmentHistoryController @Inject() (
    override val authConnector: AuthConnector,
    val service: SelfAssessmentService,
    cc: ControllerComponents
)(implicit appConfig: AppConfig, ec: ExecutionContext)
    extends AuthenticateRequestController(cc, service, authConnector) {

  def getYourSelfAssessmentData(utr: String): Action[AnyContent] = {
    val utrPattern: Regex = "^[0-9]{10}$".r

    utrPattern.findFirstMatchIn(utr) match {
      case Some(_) =>
        authorisedAction(utr) { implicit request =>
          Future.successful(Ok(Json.obj("message" -> "Success!")))
        }
      case None =>
        Action.apply(
          BadRequest(ApiErrorResponses(Invalid_SAUTR.toString, "invalid UTR format").asJson)
        )
    }
  }
}
