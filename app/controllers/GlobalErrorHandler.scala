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

import models.ServiceErrors.*
import models.{ApiErrorResponses, ServiceErrors}
import play.api.Logging
import play.api.http.HttpErrorHandler
import play.api.mvc.Results.*
import play.api.mvc.{RequestHeader, Result, Results}
import uk.gov.hmrc.auth.core.{AuthorisationException, NoActiveSession}
import utils.FutureConverter.FutureOps
import utils.constants.ErrorMessageConstansts.*

import javax.inject.Singleton
import scala.concurrent.Future

@Singleton
class GlobalErrorHandler extends HttpErrorHandler with Logging {

  override def onClientError(
      request: RequestHeader,
      statusCode: Int,
      message: String
  ): Future[Result] = {
    statusCode match
      case 400      => Future.successful(BadRequest(ApiErrorResponses(BAD_REQUEST_RESPONSE).asJson))
      case 404      => Future.successful(NotFound(ApiErrorResponses(NOT_FOUND_RESPONSE).asJson))
      case other4xx => Future.successful(Status(statusCode)(ApiErrorResponses(message).asJson))
  }

  override def onServerError(request: RequestHeader, exception: Throwable): Future[Result] = {
    exception match {
      case Downstream_Error | Json_Validation_Error | _: IllegalArgumentException =>
        InternalServerError(ApiErrorResponses(INTERNAL_ERROR_RESPONSE).asJson).toFuture
      case No_Data_Found_Error => NotFound(ApiErrorResponses(NOT_FOUND_RESPONSE).asJson).toFuture
      case Invalid_Start_Date_Error | Invalid_Utr_Error | _: NoActiveSession =>
        BadRequest(ApiErrorResponses(BAD_REQUEST_RESPONSE).asJson).toFuture
      case Unauthorised_Error | _: AuthorisationException =>
        Unauthorized(ApiErrorResponses(UNAUTHORISED_RESPONSE).asJson).toFuture
      case _ => ServiceUnavailable(ApiErrorResponses(SERVICE_UNAVAILABLE_RESPONSE).asJson).toFuture
    }
  }
}
