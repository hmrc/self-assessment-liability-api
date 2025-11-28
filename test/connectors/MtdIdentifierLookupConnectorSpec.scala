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

import models.{EtmpError, EtmpValidationError, HipError, HipErrorDetails, HipResponseError, MtdId}
import models.ServiceErrors.{
  Downstream_Error,
  Json_Validation_Error,
  Service_Currently_Unavailable_Error
}
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
  private def serviceUrl(nino: String) =
    s"/etmp/RESTAdapter/itsa/taxpayer/business-details?nino=$nino"
  private val mtdId: MtdId = MtdId("MtdItId")
  private val successResponse: String = Json
    .obj("success" -> Json.obj("taxPayerDisplayResponse" -> Json.obj("mtdId" -> "MtdItId")))
    .toString
  private val hipError = Json
    .toJson(HipResponseError("hip", None, HipErrorDetails(List(HipError("badType", "badMessage")))))
    .toString
  val invalidJsonResponse: String = Json.obj("invalidField" -> "invalidValue").toString()
  private val etmpError = Json
    .toJson(
      EtmpValidationError(List(EtmpError(processingDate = "date", code = "001", text = "bad utr")))
    )
    .toString
  "getMtdId" should {
    "return mtd ID associated with the nino if 200 response is received" in {
      simulateGet(serviceUrl("nino"), OK, successResponse)
      val result = connector.getMtdId("nino")
      result.futureValue mustBe mtdId
    }
    "return Downstream_Error error in case of a 500 response" in {
      simulateGet(serviceUrl("invalidNino"), INTERNAL_SERVER_ERROR, hipError)
      val result = connector.getMtdId("invalidNino")
      result.failed.futureValue mustBe Downstream_Error

    }
    "return Service_Currently_Unavailable error in case of a 503 response" in {
      simulateGet(serviceUrl("invalidNino"), SERVICE_UNAVAILABLE, hipError)
      val result = connector.getMtdId("invalidNino")
      result.failed.futureValue mustBe Service_Currently_Unavailable_Error
    }
    "return Downstream_Error error in case of bad request response" in {
      simulateGet(serviceUrl("invalidNino"), BAD_REQUEST, hipError)
      val result = connector.getMtdId("invalidNino")
      result.failed.futureValue mustBe Downstream_Error
    }
    "return Downstream_Error when JSON validation fails in case of an ok response from HIP" in {
      simulateGet(serviceUrl("nino"), OK, invalidJsonResponse)
      val result = connector.getMtdId("nino")
      result.failed.futureValue mustBe Downstream_Error
    }
    "return Downstream_Error error in case of a 422 response" in {
      simulateGet(serviceUrl("invalidNino"), UNPROCESSABLE_ENTITY, etmpError)
      val result = connector.getMtdId("invalidNino")
      result.failed.futureValue mustBe Downstream_Error
    }
    "return Json_Validation_Error error in case of a 422 response with invalid response body" in {
      simulateGet(serviceUrl("invalidNino"), UNPROCESSABLE_ENTITY, invalidJsonResponse)
      val result = connector.getMtdId("invalidNino")
      result.failed.futureValue mustBe Json_Validation_Error
    }
    "return Json_Validation_Error error in case of any other response with invalid response body" in {
      simulateGet(serviceUrl("invalidNino"), BAD_REQUEST, invalidJsonResponse)
      val result = connector.getMtdId("invalidNino")
      result.failed.futureValue mustBe Json_Validation_Error
    }
  }

}
