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

import com.google.common.base.Charsets
import config.AppConfig
import models.ServiceErrors.{
  Downstream_Error,
  Json_Validation_Error,
  Service_Currently_Unavailable_Error
}
import models.{EtmpValidationError, HipResponseError, MtdId, ServiceErrors}
import play.api.Logging
import play.api.libs.json.{JsError, JsResultException, JsSuccess}
import uk.gov.hmrc.http.HttpReads.Implicits.readRaw
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}
import utils.FutureConverter.FutureOps

import java.time.format.DateTimeFormatter
import java.time.{ZoneId, ZonedDateTime}
import java.util.{Base64, UUID}
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class MtdIdentifierLookupConnector @Inject() (client: HttpClientV2, appConfig: AppConfig)
    extends Logging {
  def getMtdId(nino: String, stubAgentAuth: Boolean = false)(implicit
      hc: HeaderCarrier,
      ec: ExecutionContext
  ): Future[MtdId] = {
    val correlationID = UUID.randomUUID.toString
    logger.info(s"calling HIP with $correlationID to get an MTD ID")
    val encodedAuthToken = Base64.getEncoder.encodeToString(
      s"${appConfig.hipClientId}:${appConfig.hipClientSecret}".getBytes(Charsets.UTF_8)
    )
    def getTimeStamp: String = ZonedDateTime
      .now(ZoneId.of("UTC"))
      .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'"))
    val requestHeaders = Seq(
      "CorrelationId" -> correlationID,
      "Authorization" -> s"Basic $encodedAuthToken",
      "X-Message-Type" -> "TaxpayerDisplay",
      "X-Receipt-Date" -> getTimeStamp,
      "Content-Type" -> "application/json",
      "X-Originating-System" -> "MDTP",
      "X-Regime-Type" -> "ITSA",
      "X-Transmitting-System" -> "HIP"
    )

    client
      .get(
        url"${appConfig.mtdIdLookup(stubAgentAuth)}/etmp/RESTAdapter/itsa/taxpayer/business-details?nino=$nino"
      )
      .setHeader(requestHeaders*)
      .execute[HttpResponse]
      .flatMap {
        case response if response.status == 200 => response.json.as[MtdId].toFuture
        case response if response.status == 503 =>
          Future.failed(Service_Currently_Unavailable_Error)
        case response if response.status == 422 =>
          response.json.validate[EtmpValidationError] match {
            case JsSuccess(etmpError, _) =>
              val errorSummary = etmpError.errors
                .map(e =>
                  s"at${e.processingDate} error code ${e.code} returned with message: ${e.text}"
                )
                .mkString("; ")
              logger.warn(
                s"call to get MTD ID failed with status ${response.status}. Errors: $errorSummary"
              )
              Future.failed(Downstream_Error)
            case JsError(error) =>
              logger.warn(
                s"validation failed on the error received from HIP when fetching MTD id with error: $error"
              )
              Future.failed(Json_Validation_Error)
          }
        case response =>
          response.json.validate[HipResponseError] match {
            case JsSuccess(hipErrorResponse, _) =>
              val errorSummary = hipErrorResponse.response.failures
                .map(e => s"${e.`type`}: ${e.reason}")
                .mkString("; ")
              logger.warn(
                s"call failed when fetching MTD ID with status ${response.status}. Errors: $errorSummary"
              )
              Future.failed(Downstream_Error)
            case JsError(error) =>
              logger.warn(
                s"validation failed on the error received from HIP when fetching MTD id  with error: $error"
              )
              Future.failed(Json_Validation_Error)
          }
      }
      .recoverWith { case _: JsResultException =>
        logger.error("unexpected payload received when trying to fetch MTD ID")
        Future.failed(Downstream_Error)
      }
  }
}
