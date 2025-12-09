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
import models.CidPerson
import models.ServiceErrors.*
import play.api.Logging
import uk.gov.hmrc.http.HttpReads.Implicits.readRaw
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CitizenDetailsConnector @Inject() (client: HttpClientV2, appConfig: AppConfig)
    extends Logging {
  def getNino(
      utr: String
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[String]] = {
    client
      .get(url"${appConfig.citizenDetailsLookup}/citizen-details/sautr/$utr")
      .execute[HttpResponse]
      .flatMap {
        case response if response.status == 200 =>
          response.json
            .validate[CidPerson]
            .fold(
              errors => Future.failed(Downstream_Error),
              cid => Future.successful(cid.ids.nino)
            )
        case response if response.status == 500 =>
          Future.failed(Downstream_Error)
        case response if response.status == 404 =>
          logger.info("Call to CID failed as no nino found for the utr provided")
          Future.failed(Downstream_Error)
        case response =>
          logger.info(
            s"Call to CID failed with ${response.status} status and body ${response.body}"
          )
          Future.failed(Service_Currently_Unavailable_Error)
      }
  }
}
