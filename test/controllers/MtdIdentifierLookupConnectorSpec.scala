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

package controllers

import connectors.MtdIdentifierLookupConnector
import models.{ApiErrorResponses, MtdId}
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
  private val badRequestResponse: ApiErrorResponses = ApiErrorResponses.apply(
    status = 400,
    message = "Invalid national insurance number returned from citizen details"
  )
  private val internalServerErrorResponse: ApiErrorResponses =
    ApiErrorResponses.apply(status = 500, message = "Service currently unavailable")

  "getMtdId" should {
    "return group IDs associated with an enrolment if 200 response is received" in {

      stubGet(serviceUrl("nino"), OK, successResponse)
      val result = connector.getMtdId("nino")
      result.futureValue mustBe mtdId
    }

    "return invalid nino error in case of a 400 response" in {
      stubGet(serviceUrl("invalidNino"), BAD_REQUEST, "")
      val result = connector.getMtdId("invalidNino")
      result.failed.futureValue mustBe badRequestResponse

    }

    "return internal server error in case of a any other response" in {
      stubGet(serviceUrl("ninoCausinginternalError"), INTERNAL_SERVER_ERROR, "")
      val result = connector.getMtdId("ninoCausinginternalError")
      result.failed.futureValue mustBe internalServerErrorResponse

    }
  }

}
