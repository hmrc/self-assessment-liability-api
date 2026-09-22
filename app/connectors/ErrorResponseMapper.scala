/*
 * Copyright 2026 HM Revenue & Customs
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

import models.ServiceErrors.*
import models.ApiErrorResponses
import play.api.mvc.Result
import play.api.mvc.Results.*
import uk.gov.hmrc.auth.core.{AuthorisationException, NoActiveSession}
import utils.constants.ErrorMessageConstansts.*

object ErrorResponseMapper {

  def toResult(exception: Throwable): Result =
    exception match {
      case Downstream_Error | Json_Validation_Error | _: IllegalArgumentException =>
        InternalServerError(
          ApiErrorResponses(INTERNAL_ERROR_RESPONSE).asJson
        )

      case No_Data_Found_Error =>
        NotFound(
          ApiErrorResponses(NOT_FOUND_RESPONSE).asJson
        )

      case Invalid_Start_Date_Error | Invalid_Utr_Error | _: NoActiveSession =>
        BadRequest(
          ApiErrorResponses(BAD_REQUEST_RESPONSE).asJson
        )

      case Unauthorised_Error | _: AuthorisationException =>
        Unauthorized(
          ApiErrorResponses(UNAUTHORISED_RESPONSE).asJson
        )

      case _ =>
        ServiceUnavailable(
          ApiErrorResponses(SERVICE_UNAVAILABLE_RESPONSE).asJson
        )
    }
}
