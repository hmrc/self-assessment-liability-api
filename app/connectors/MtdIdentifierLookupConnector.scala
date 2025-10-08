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
import models.ServiceErrors.{Downstream_Error, Service_Currently_Unavailable_Error}
import models.{MtdId, ServiceErrors}
import play.api.Logging
import play.api.libs.json.JsResultException
import uk.gov.hmrc.http.HttpReads.Implicits.readRaw
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}
import utils.FutureConverter.FutureOps

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class MtdIdentifierLookupConnector @Inject() (client: HttpClientV2, appConfig: AppConfig)
    extends Logging {
  def getMtdId(nino: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[MtdId] = {
    client
      .get(url"${appConfig.mtdIdLookup}/mtd-identifier-lookup/nino/$nino")
      .execute[HttpResponse]
      .flatMap {
        case response if response.status == 200 => response.json.as[MtdId].toFuture
        case response if response.status == 500 => Future.failed(Downstream_Error)
        case response if response.status == 400 => Future.failed(Downstream_Error)
        case response =>
          logger.warn(
            s"call to ${appConfig.mtdIdLookup}/mtd-identifier-lookup/ failed with ${response.status} status"
          )
          Future.failed(Service_Currently_Unavailable_Error)
      }
      .recoverWith { case _: JsResultException =>
        logger.warn("unexpected payload received from mtd look up service")
        Future.failed(Downstream_Error)
      }
  }
}
