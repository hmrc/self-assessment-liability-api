/*
 * Copyright 2024 HM Revenue & Customs
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

import models.MtdId
import models.ServiceErrors.{Downstream_Error, Invalid_NINO}
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import shared.{HttpWireMock, SpecBase}

class MtdIdentifierLookupConnectorSpec extends SpecBase with HttpWireMock {

  override lazy val app: Application = new GuiceApplicationBuilder()
    .configure(
      conf = "microservice.services.mtd-id-lookup.port" -> server.port()
    )
    .build()
  private lazy val connector: MtdIdentifierLookupConnector =
    app.injector.instanceOf[MtdIdentifierLookupConnector]
  private def serviceUrl(nino: String) = s"/mtd-identifier-lookup/nino/$nino"
  private val mtdId: MtdId = MtdId("MtdItId")
  private val successResponse: String = Json.obj("mtdbsa" -> "MtdItId").toString

  "getMtdId" should {
    "return mtd ID associated with the nino if 200 response is received" in {
      simulateGet(serviceUrl("nino"), OK, successResponse)
      val result = connector.getMtdId("nino")
      result.futureValue mustBe mtdId
    }

    "return Invalid_NINO error in case of a 400 response" in {
      simulateGet(serviceUrl("invalidNino"), BAD_REQUEST, "")
      val result = connector.getMtdId("invalidNino")
      result.failed.futureValue mustBe Invalid_NINO

    }

    "return Downstream_Error in case of a any other response" in {
      simulateGet(serviceUrl("ninoCausingInternalError"), INTERNAL_SERVER_ERROR, "")
      val result = connector.getMtdId("ninoCausinginternalError")
      result.failed.futureValue mustBe Downstream_Error

    }
  }

}
