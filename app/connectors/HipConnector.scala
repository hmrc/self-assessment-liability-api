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

import com.fasterxml.jackson.core.JsonParseException
import config.AppConfig
import models.HipResponse
import models.ServiceErrors.*
import uk.gov.hmrc.http.HttpReads.Implicits.readRaw
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}
import utils.FutureConverter.FutureOps

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class HipConnector @Inject() (client: HttpClientV2, appConfig: AppConfig) {
  def getSelfAssessmentData(
      utr: String,
      fromDate: String,
      toDate: String
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[HipResponse] = {
    val queryParameters = Seq(
      "dateFrom" -> fromDate,
      "dateTo" -> toDate
    )

    client
      .get(
        url"${appConfig.hipLookup}/self-assessment/account/$utr/liability-details"
      )
      .transform(_.withQueryStringParameters(queryParameters*))
      .execute[HttpResponse]
      .flatMap {
        case response if response.status == 200 =>
          response.json.as[HipResponse].toFuture
        case response if response.status == 400 =>
          Future.failed(Invalid_Correlation_Id)
        case response if response.status == 401 =>
          Future.failed(HIP_Unauthorised)
        case response if response.status == 403 =>
          Future.failed(HIP_Forbidden)
        case response if response.status == 404 =>
          Future.failed(No_Payments_Found_For_UTR)
        case response if response.status == 422 =>
          Future.failed(Invalid_UTR)
        case response if response.status == 500 =>
          Future.failed(HIP_Server_Error)
        case response if response.status == 502 =>
          Future.failed(HIP_Bad_Gateway)
        case response if response.status == 503 =>
          Future.failed(HIP_Service_Unavailable)
        case _ =>
          Future.failed(Downstream_Error)
      }
      .recoverWith { case _: JsonParseException =>
        Future.failed(Downstream_Error)
      }
  }
}
