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

import models.ApiErrorResponses
import models.ServiceErrors.{Downstream_Error, Json_Validation_Error, No_Data_Found}
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import services.SelfAssessmentService
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import utils.constants.ErrorMessageConstansts.{
  BAD_REQUEST_RESPONSE,
  INTERNAL_ERROR_RESPONSE,
  NOT_FOUND_RESPONSE,
  SERVICE_UNAVAILABLE_RESPONSE
}

import java.time.format.DateTimeParseException
import javax.inject.Inject
import scala.concurrent.ExecutionContext

class SelfAssessmentHistoryController @Inject() (
    authenticate: AuthenticateRequestAction,
    cc: ControllerComponents,
    service: SelfAssessmentService
)(implicit ec: ExecutionContext)
    extends BackendController(cc) {

  def getYourSelfAssessmentData(utr: String, fromDate: Option[String]): Action[AnyContent] =
    authenticate(utr).async { implicit request =>
      (for {
        selfAssessmentData <- service.viewAccountService(utr, fromDate)
      } yield Ok(Json.toJson(selfAssessmentData)))
        .recover {
          case _: DateTimeParseException =>
            BadRequest(ApiErrorResponses(BAD_REQUEST_RESPONSE).asJson)
          case Json_Validation_Error =>
            InternalServerError(ApiErrorResponses(INTERNAL_ERROR_RESPONSE).asJson)
          case No_Data_Found => NotFound(ApiErrorResponses(NOT_FOUND_RESPONSE).asJson)
          case Downstream_Error =>
            ServiceUnavailable(ApiErrorResponses(SERVICE_UNAVAILABLE_RESPONSE).asJson)
        }
    }

}
