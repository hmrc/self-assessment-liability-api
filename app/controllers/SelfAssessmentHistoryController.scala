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

import controllers.actions.{AuthenticateRequestAction, ValidateRequestAction}
import play.api.Logging
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import services.SelfAssessmentService
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class SelfAssessmentHistoryController @Inject() (
    authenticateUser: AuthenticateRequestAction,
    validateRequest: ValidateRequestAction,
    cc: ControllerComponents,
    service: SelfAssessmentService
)(implicit ec: ExecutionContext)
    extends BackendController(cc)
    with Logging {

  def getYourSelfAssessmentData(utr: String, fromDate: Option[String]): Action[AnyContent] =
    (Action andThen validateRequest(utr) andThen authenticateUser).async { implicit request =>
      for {
        selfAssessmentData <-
          service.viewAccountService(
            utr,
            request.requestPeriod.startDate,
            request.requestPeriod.endDate
          )
      } yield Ok(Json.toJson(selfAssessmentData))
    }

}
