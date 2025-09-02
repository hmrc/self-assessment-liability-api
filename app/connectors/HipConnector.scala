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
import models.HipResponse
import models.ServiceErrors.*
import play.api.Logging
import play.api.libs.json.{JsError, JsSuccess}
import uk.gov.hmrc.http.HttpReads.Implicits.readRaw
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}

import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class HipConnector @Inject() (client: HttpClientV2, appConfig: AppConfig) extends Logging {
  def getSelfAssessmentData(
      utr: String,
      fromDate: LocalDate,
      toDate: LocalDate
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[HipResponse] = {
    val queryParameters = Seq(
      "dateFrom" -> fromDate.toString,
      "dateTo" -> toDate.toString
    )

    val correlationId: String = UUID.randomUUID.toString

    client
      .get(
        url"${appConfig.hipLookup}/self-assessment/account/$utr/liability-details"
      )
      .transform(_.withQueryStringParameters(queryParameters*))
      .setHeader("correlationId" -> correlationId)
      .execute[HttpResponse]
      .flatMap {
        case response if response.status == 200 =>
          response.json.validate[HipResponse] match {
            case JsSuccess(hipResponse, _) => Future.successful(hipResponse)
            case JsError(error)            => Future.failed(Json_Validation_Error)
          }
        case response if response.status == 404 =>
          Future.failed(No_Data_Found)
        case response =>
          Future.failed(Downstream_Error)
      }
  }
}
