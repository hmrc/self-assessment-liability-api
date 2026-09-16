/*
 * Copyright 2026 HM Revenue & Customs
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

import com.github.tomakehurst.wiremock.client.WireMock.*
import models.ServiceErrors.Downstream_Error
import org.scalatest.concurrent.Eventually
import play.api.Application
import play.api.http.Status.INTERNAL_SERVER_ERROR
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import shared.{HttpWireMock, SpecBase}

class CitizenDetailsOutboundAuditSpec
  extends SpecBase
    with HttpWireMock
    with Eventually {

  override lazy val app: Application =
    new GuiceApplicationBuilder()
      .configure(
        "microservice.services.citizen-details.port" -> server.port(),
        "auditing.enabled" -> true,
        "auditing.consumer.baseUri.host" -> "localhost",
        "auditing.consumer.baseUri.port" -> server.port(),
        "metrics.enabled" -> false
      )
      .build()

  private lazy val connector: CitizenDetailsConnector =
    app.injector.instanceOf[CitizenDetailsConnector]

  private def serviceUrl(utr: String): String =
    s"/citizen-details/sautr/$utr"

  "CitizenDetailsConnector outbound auditing" should {

    "generate an outbound audit containing the response details when Citizen Details returns 500" in {

      server.stubFor(
        post(urlEqualTo("/write/audit/merged"))
          .willReturn(
            aResponse()
              .withStatus(204)
          )
      )

      simulateGet(
        serviceUrl("invalidUtr"),
        INTERNAL_SERVER_ERROR,
        """{"message":"downstream failed"}"""
      )

      val result =
        connector.getNino("invalidUtr")

      result.failed.futureValue mustBe Downstream_Error

      eventually {
        server.verify(
          postRequestedFor(
            urlEqualTo("/write/audit/merged")
          )
        )
      }

      val auditRequests =
        server.findAll(
          postRequestedFor(
            urlEqualTo("/write/audit/merged")
          )
        )

      auditRequests.size() mustBe 1

      val auditBody =
        Json.parse(auditRequests.get(0).getBodyAsString)

      println(s"Audit body:\n${Json.prettyPrint(auditBody)}")

      (auditBody \ "auditType").as[String] mustBe "OutboundCall"

      (auditBody \ "response" \ "detail" \ "statusCode")
        .as[String] mustBe "500"

      (auditBody \ "response" \ "detail" \ "responseMessage")
        .as[String] mustBe """{"message":"downstream failed"}"""

      (auditBody \ "response" \ "detail" \ "failedRequestReason")
        .toOption mustBe None
    }
  }

  "generate an outbound audit containing the response details when Citizen Details returns 404" in {

    server.stubFor(
      post(urlEqualTo("/write/audit/merged"))
        .willReturn(
          aResponse()
            .withStatus(204)
        )
    )

    simulateGet(
      serviceUrl("invalidUtr"),
      NOT_FOUND,
      """{"message":"not found"}"""
    )

    val result =
      connector.getNino("invalidUtr")

    result.failed.futureValue mustBe Downstream_Error

    eventually {
      server.verify(
        postRequestedFor(
          urlEqualTo("/write/audit/merged")
        )
      )
    }

    val auditRequests =
      server.findAll(
        postRequestedFor(
          urlEqualTo("/write/audit/merged")
        )
      )

    auditRequests.size() mustBe 1

    val auditBody =
      Json.parse(auditRequests.get(0).getBodyAsString)

    println(s"404 Audit body:\n${Json.prettyPrint(auditBody)}")

    (auditBody \ "auditType").as[String] mustBe "OutboundCall"

    (auditBody \ "response" \ "detail" \ "statusCode")
      .as[String] mustBe "404"

    (auditBody \ "response" \ "detail" \ "responseMessage")
      .as[String] mustBe """{"message":"not found"}"""

    (auditBody \ "response" \ "detail" \ "failedRequestReason")
      .toOption mustBe None
  }
}