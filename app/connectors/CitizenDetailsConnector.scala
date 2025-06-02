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

package connectors

import config.AppConfig
import models.ApiErrorResponses

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import play.api.libs.json.JsValue
import uk.gov.hmrc.http.HttpReads.Implicits.readRaw
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}

class CitizenDetailsConnector @Inject() (client: HttpClientV2, appConfig: AppConfig) {
  def getNino(utr: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[String] = {
    client
      .get(url"${appConfig.citizenDetailsLookup}/citizen-details/sautr/$utr")
      .execute[HttpResponse]
      .flatMap {
        case response if response.status == 200 =>
          Future.apply((response.json \ "ids" \ "nino").as[String])
        case response if response.status == 400 =>
          Future.failed(
            ApiErrorResponses.apply(
              status = 400,
              message = "Invalid SaUtr."
            )
          )
        case response if response.status == 404 =>
          Future.failed(
            ApiErrorResponses.apply(
              status = 404,
              message = "No record for the given SaUtr is found."
            )
          )
        case response if response.status == 500 =>
          Future.failed(
            ApiErrorResponses.apply(
              status = 500,
              message = "More than one valid matching result."
            )
          )
        case _ =>
          Future.failed(
            ApiErrorResponses.apply(
              status = 500,
              message = "Service currently unavailable"
            )
          )
      }
  }
}
