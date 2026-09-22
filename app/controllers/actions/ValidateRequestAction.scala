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

import com.google.inject.Inject
import models.{ApiErrorResponses, RequestPeriod, RequestWithUtr}
import play.api.Logging
import play.api.mvc.*
import play.api.mvc.Results.BadRequest
import utils.UkTaxYears.{getPastTwoUkTaxYears, isInvalidDate}
import utils.UtrValidator.isValidUtr
import utils.constants.ErrorMessageConstansts.BAD_REQUEST_RESPONSE

import java.time.LocalDate
import java.time.format.DateTimeParseException
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class ValidateRequestAction @Inject() ()(implicit val ec: ExecutionContext) extends Logging {

  def apply(utr: String): ActionRefiner[Request, RequestWithUtr] =
    new ActionRefiner[Request, RequestWithUtr] {

      override protected def executionContext: ExecutionContext = ec

      override protected def refine[A](
                                        request: Request[A]
                                      ): Future[Either[Result, RequestWithUtr[A]]] = {

        if (isValidUtr(utr)) {
          val requestPeriod = getPastTwoUkTaxYears()

          request
            .getQueryString("fromDate")
            .fold(
              Future.successful(
                Right(
                  RequestWithUtr(
                    utr = utr,
                    requestPeriod =
                      RequestPeriod(
                        startDate = requestPeriod._1,
                        endDate = requestPeriod._2
                      ),
                    request = request
                  )
                )
              )
            ) { dateInStringFormat =>
              validateAndParseDate(dateInStringFormat).map {
                case Right(date) =>
                  Right(
                    RequestWithUtr(
                      utr = utr,
                      requestPeriod =
                        RequestPeriod(
                          startDate = date,
                          endDate = requestPeriod._2
                        ),
                      request = request
                    )
                  )

                case Left(result) =>
                  Left(result)
              }
            }
        } else {
          Future.successful(
            Left(badRequest)
          )
        }
      }

      private def validateAndParseDate(
                                        dateInStringFormat: String
                                      ): Future[Either[Result, LocalDate]] = {
        Future
          .fromTry(Try(LocalDate.parse(dateInStringFormat)))
          .map { parsedDate =>
            if (isInvalidDate(dateToValidate = parsedDate)) {
              logger.info(s"Rejecting $dateInStringFormat as it is invalid")
              Left(badRequest)
            } else {
              Right(parsedDate)
            }
          }
          .recover { case _: DateTimeParseException =>
            logger.info(s"parsing of $dateInStringFormat failed")
            Left(badRequest)
          }
      }

      private def badRequest: Result =
        BadRequest(
          ApiErrorResponses(BAD_REQUEST_RESPONSE).asJson
        )
    }
}