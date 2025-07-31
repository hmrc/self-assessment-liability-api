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

import models.ServiceErrors.*
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import shared.{HttpWireMock, SpecBase}

import java.time.LocalDate

class HipConnectorSpec extends SpecBase with HttpWireMock {

  override lazy val app: Application = new GuiceApplicationBuilder()
    .configure(
      conf = "microservice.services.hip.port" -> server.port()
    )
    .build()
  private lazy val connector: HipConnector =
    app.injector.instanceOf[HipConnector]
  private val utr: String = "1234567890"
  private val fromDate: String = "2025-04-06"
  private val toDate: String = LocalDate.now.toString
  private val serviceUrl =
    s"/self-assessment/account/$utr/liability-details?dateFrom=$fromDate&dateTo=$toDate"

  "getSelfAssessmentData" should {
    "return JSON associated with the utr and date if 200 response is received" in {
      simulateGet(serviceUrl, OK, jsonSuccessResponse)
      val result = connector.getSelfAssessmentData(utr, fromDate, toDate)
      result.futureValue mustBe hipResponse
    }

    "return expected error if returned JSON is malformed" in {
      simulateGet(serviceUrl, OK, """{malformedJson}""")
      val result = connector.getSelfAssessmentData(utr, fromDate, toDate)
      result.failed.futureValue mustBe Downstream_Error
    }

    "return expected error if 400 response is received" in {
      simulateGet(serviceUrl, BAD_REQUEST, "")
      val result = connector.getSelfAssessmentData(utr, fromDate, toDate)
      result.failed.futureValue mustBe Invalid_Correlation_Id
    }

    "return expected error if 401 response is received" in {
      simulateGet(serviceUrl, UNAUTHORIZED, "")
      val result = connector.getSelfAssessmentData(utr, fromDate, toDate)
      result.failed.futureValue mustBe HIP_Unauthorised
    }

    "return expected error if 403 response is received" in {
      simulateGet(serviceUrl, FORBIDDEN, "")
      val result = connector.getSelfAssessmentData(utr, fromDate, toDate)
      result.failed.futureValue mustBe HIP_Forbidden
    }

    "return expected error if 404 response is received" in {
      simulateGet(serviceUrl, NOT_FOUND, "")
      val result = connector.getSelfAssessmentData(utr, fromDate, toDate)
      result.failed.futureValue mustBe No_Payments_Found_For_UTR
    }

    "return expected error if 422 response is received" in {
      simulateGet(serviceUrl, UNPROCESSABLE_ENTITY, "")
      val result = connector.getSelfAssessmentData(utr, fromDate, toDate)
      result.failed.futureValue mustBe Invalid_UTR
    }

    "return expected error if 500 response is received" in {
      simulateGet(serviceUrl, INTERNAL_SERVER_ERROR, "")
      val result = connector.getSelfAssessmentData(utr, fromDate, toDate)
      result.failed.futureValue mustBe HIP_Server_Error
    }

    "return expected error if 502 response is received" in {
      simulateGet(serviceUrl, BAD_GATEWAY, "")
      val result = connector.getSelfAssessmentData(utr, fromDate, toDate)
      result.failed.futureValue mustBe HIP_Bad_Gateway
    }

    "return expected error if 503 response is received" in {
      simulateGet(serviceUrl, SERVICE_UNAVAILABLE, "")
      val result = connector.getSelfAssessmentData(utr, fromDate, toDate)
      result.failed.futureValue mustBe HIP_Service_Unavailable
    }

    "return Downstream_Error in case of any other response" in {
      simulateGet(serviceUrl, IM_A_TEAPOT, "")
      val result = connector.getSelfAssessmentData(utr, fromDate, toDate)
      result.failed.futureValue mustBe Downstream_Error
    }
  }
}
