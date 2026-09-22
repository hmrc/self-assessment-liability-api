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

package controllers

import com.github.tomakehurst.wiremock.client.WireMock.{
  aResponse,
  get,
  post,
  postRequestedFor,
  urlEqualTo
}
import org.scalatest.concurrent.Eventually
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.auth.core.AuthConnector
import utils.IntegrationSpecBase

class SelfAssessmentHistoryAuditSpec
  extends IntegrationSpecBase
    with Eventually {

  override lazy val app: Application = {
    server.start()

    new GuiceApplicationBuilder()
      .overrides(
        play.api.inject.bind[AuthConnector].toInstance(mockAuthConnector)
      )
      .configure(
        "play.http.errorHandler" -> "controllers.GlobalErrorHandler",
        "microservice.services.citizen-details.port" -> server.port(),
        "microservice.services.mtd-id-lookup.port" -> server.port(),
        "microservice.services.hip.port" -> server.port(),
        "auditing.enabled" -> true,
        "auditing.consumer.baseUri.host" -> "localhost",
        "auditing.consumer.baseUri.port" -> server.port(),
        "metrics.enabled" -> false
      )
      .build()
  }

  "SelfAssessmentHistoryController request auditing" should {

    "audit statusCode and responseMessage instead of failedRequestReason for an invalid UTR" in {

      server.stubFor(
        post(urlEqualTo("/write/audit"))
          .willReturn(
            aResponse()
              .withStatus(NO_CONTENT)
          )
      )

      val request =
        FakeRequest(
          GET,
          "/invalidUtr?fromDate=2024-01-01"
        )

      val result =
        route(app, request).get

      result
        .map(_ => ())
        .recover { case _ => () }
        .futureValue

      eventually {
        server.verify(
          postRequestedFor(
            urlEqualTo("/write/audit")
          )
        )
      }

      val auditRequests =
        server.findAll(
          postRequestedFor(
            urlEqualTo("/write/audit")
          )
        )

      auditRequests.size() must be >= 1

      val requestReceivedAudit =
        auditRequests
          .toArray
          .map(
            _.asInstanceOf[
              com.github.tomakehurst.wiremock.verification.LoggedRequest
            ]
          )
          .map(request => Json.parse(request.getBodyAsString))
          .find { auditBody =>
            (auditBody \ "auditType")
              .asOpt[String]
              .contains("RequestReceived")
          }
          .getOrElse {
            fail("No RequestReceived audit event was found")
          }

      println(
        s"RequestReceived audit:\n${Json.prettyPrint(requestReceivedAudit)}"
      )

      val detail =
        requestReceivedAudit \ "detail"

      (detail \ "statusCode")
        .asOpt[String] mustBe Some("400")

      (detail \ "responseMessage")
        .asOpt[String] must not be empty

      (detail \ "failedRequestReason")
        .toOption mustBe None
    }

    "audit statusCode and responseMessage instead of failedRequestReason when a downstream call fails" in {

      server.stubFor(
        post(urlEqualTo("/write/audit"))
          .willReturn(
            aResponse()
              .withStatus(NO_CONTENT)
          )
      )

      server.stubFor(
        post(urlEqualTo("/write/audit/merged"))
          .willReturn(
            aResponse()
              .withStatus(NO_CONTENT)
          )
      )

      server.stubFor(
        get(urlEqualTo("/citizen-details/sautr/1234567890"))
          .willReturn(
            aResponse()
              .withStatus(INTERNAL_SERVER_ERROR)
              .withHeader("Content-Type", "application/json")
              .withBody(
                """{"message":"downstream failed"}"""
              )
          )
      )

      val request =
        FakeRequest(
          GET,
          "/1234567890?fromDate=2024-01-01"
        )

      val result =
        route(app, request).get

      result
        .map(_ => ())
        .recover { case _ => () }
        .futureValue

      eventually {
        server.verify(
          postRequestedFor(
            urlEqualTo("/write/audit")
          )
        )
      }

      val auditRequests =
        server.findAll(
          postRequestedFor(
            urlEqualTo("/write/audit")
          )
        )

      auditRequests.size() must be >= 1

      val requestReceivedAudit =
        auditRequests
          .toArray
          .map(
            _.asInstanceOf[
              com.github.tomakehurst.wiremock.verification.LoggedRequest
            ]
          )
          .map(request => Json.parse(request.getBodyAsString))
          .find { auditBody =>
            (auditBody \ "auditType")
              .asOpt[String]
              .contains("RequestReceived")
          }
          .getOrElse {
            fail("No RequestReceived audit event was found")
          }

      println(
        s"Downstream failure RequestReceived audit:\n${Json.prettyPrint(requestReceivedAudit)}"
      )

      val detail =
        requestReceivedAudit \ "detail"

      (detail \ "statusCode")
        .asOpt[String] mustBe Some("500")

      (detail \ "responseMessage")
        .asOpt[String] must not be empty

      (detail \ "failedRequestReason")
        .toOption mustBe None
    }
  }
}