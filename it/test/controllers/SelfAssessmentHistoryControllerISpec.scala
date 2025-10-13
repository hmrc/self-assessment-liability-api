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

package controllers

import models.{ApiErrorResponses, CidPerson, MtdId, TaxIds}
import org.scalatest.time.SpanSugar.convertIntToGrainOfTime
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks.forAll
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers.{GET, contentAsJson, defaultAwaitTimeout, route, status}
import shared.HipResponseGenerator
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.play.bootstrap.http.HttpClientV2Provider
import utils.IntegrationSpecBase
import utils.constants.ErrorMessageConstansts.*

import java.net.URI
import java.time.LocalDate
import scala.concurrent.Await

class SelfAssessmentHistoryControllerISpec extends IntegrationSpecBase {


  lazy val provider: HttpClientV2Provider = app.injector.instanceOf[HttpClientV2Provider]
  lazy val client: HttpClientV2 = provider.get()
  val cidPerson: CidPerson = CidPerson(
    name = None,
    ids = TaxIds(nino = Some(nino)),
    dateOfBirth = None
  )

  val dateFrom: String = LocalDate.now().minusYears(2).toString
  val dateTo: String = LocalDate.now().toString
  val cidPayload: String = Json.toJson(cidPerson).toString
  val mtdId = MtdId("mtdId")
  val mtdIdPayload: String = Json.obj("mtdbsa" -> "mtdId").toString
  val baseUrl = s"http://localhost:$port/$utr?fromDate=$dateFrom"
  val hipUrl = s"/self-assessment/account/$utr/liability-details?dateFrom=$dateFrom&dateTo=$dateTo"
  val cidUrl = s"/citizen-details/sautr/$utr"
  val mtdLookupUrl = s"/mtd-identifier-lookup/nino/$nino"

  "Integration Tests for SA History Controller" must {
    "CID connection" should {
      "return 200 with the correct response in success journey" in {

        forAll(HipResponseGenerator.hipResponseGen) { hipResponse =>
          val hipResponsePayload = Json.toJson(hipResponse)
          simulateGet(cidUrl, OK, cidPayload)
          simulateGet(mtdLookupUrl, OK, mtdIdPayload)
          simulateGet(hipUrl, OK, hipResponsePayload.toString)
          val request = FakeRequest(GET, s"/$utr?fromDate=$dateFrom")
            .withHeaders("Authorization" -> "Bearer 1234")

          val result = route(app, request).get

          status(result) mustEqual OK
          contentAsJson(result) mustEqual hipResponsePayload
        }
      }
      "return 500 if call fails due to data quality issues" in {
        simulateGet(cidUrl, INTERNAL_SERVER_ERROR, "")
        val result =
          Await.result(
            client
              .get(URI.create(baseUrl).toURL)
              .setHeader("Authorization" -> "Bearer 1234")
              .execute[HttpResponse], 5.seconds)

        result.status mustEqual INTERNAL_SERVER_ERROR
        result.json.as[ApiErrorResponses].message mustEqual INTERNAL_ERROR_RESPONSE
      }

      "return 500 if call if no nino is found" in {
        simulateGet(cidUrl, NOT_FOUND, "")

        val result =
          Await.result(
            client
              .get(URI.create(baseUrl).toURL)
              .setHeader("Authorization" -> "Bearer 1234")
              .execute[HttpResponse], 5.seconds)


        result.status mustEqual INTERNAL_SERVER_ERROR
        result.json.as[ApiErrorResponses].message mustEqual INTERNAL_ERROR_RESPONSE
      }

      "return 500 if call fails as an invalid json body returned" in {
        simulateGet(cidUrl, OK, Json.obj("someNewField" -> "someValue").toString)

        val result =
          Await.result(
            client
              .get(URI.create(baseUrl).toURL)
              .setHeader("Authorization" -> "Bearer 1234")
              .execute[HttpResponse], 5.seconds)


        result.status mustEqual INTERNAL_SERVER_ERROR
        result.json.as[ApiErrorResponses].message mustEqual INTERNAL_ERROR_RESPONSE
      }
      "return 503 if call fails with any other responses " in {
        simulateGet(cidUrl, PRECONDITION_FAILED, "")

        val result =
          Await.result(
            client
              .get(URI.create(baseUrl).toURL)
              .setHeader("Authorization" -> "Bearer 1234")
              .execute[HttpResponse], 5.seconds)

        result.status mustEqual SERVICE_UNAVAILABLE
        result.json.as[ApiErrorResponses].message mustEqual SERVICE_UNAVAILABLE_RESPONSE
      }
    }
    "mtd lookup service" should {
      "return 500 if call fails with a 500 response" in {
        simulateGet(cidUrl, OK, cidPayload)
        simulateGet(mtdLookupUrl, INTERNAL_SERVER_ERROR, "")
        val result =
          Await.result(
            client
              .get(URI.create(baseUrl).toURL)
              .setHeader("Authorization" -> "Bearer 1234")
              .execute[HttpResponse], 5.seconds)

        result.status mustEqual INTERNAL_SERVER_ERROR
        result.json.as[ApiErrorResponses].message mustEqual INTERNAL_ERROR_RESPONSE
      }
      "return 500 if call fails with a 400 response" in {
        simulateGet(cidUrl, OK, cidPayload)
        simulateGet(mtdLookupUrl, BAD_REQUEST, Json.obj().toString)
        val result =
          Await.result(
            client
              .get(URI.create(baseUrl).toURL)
              .setHeader("Authorization" -> "Bearer 1234")
              .execute[HttpResponse], 5.seconds)

        result.status mustEqual INTERNAL_SERVER_ERROR
        result.json.as[ApiErrorResponses].message mustEqual INTERNAL_ERROR_RESPONSE
      }
      "return 503 if  call fails due to bad payload" in {
        simulateGet(cidUrl, OK, cidPayload)
        simulateGet(mtdLookupUrl, NOT_FOUND, "")
        val result =
          Await.result(
            client
              .get(URI.create(baseUrl).toURL)
              .setHeader("Authorization" -> "Bearer 1234")
              .execute[HttpResponse], 5.seconds)

        result.status mustEqual SERVICE_UNAVAILABLE
        result.json.as[ApiErrorResponses].message mustEqual SERVICE_UNAVAILABLE_RESPONSE
      }

      "return 503 if call fails with any other response" in {
        simulateGet(cidUrl, OK, cidPayload)
        simulateGet(mtdLookupUrl, REQUEST_TIMEOUT, Json.obj().toString)
        val result =
          Await.result(
            client
              .get(URI.create(baseUrl).toURL)
              .setHeader("Authorization" -> "Bearer 1234")
              .execute[HttpResponse], 5.seconds)

        result.status mustEqual SERVICE_UNAVAILABLE
        result.json.as[ApiErrorResponses].message mustEqual SERVICE_UNAVAILABLE_RESPONSE
      }

      "HIP" should {
        "respond with a 500 if a bad payload is returned" in {
          simulateGet(cidUrl, OK, cidPayload)
          simulateGet(mtdLookupUrl, OK, mtdIdPayload)
          simulateGet(hipUrl, OK, Json.obj().toString)
          val result =
            Await.result(
              client
                .get(URI.create(baseUrl).toURL)
                .setHeader("Authorization" -> "Bearer 1234")
                .execute[HttpResponse], 5.seconds)

          result.status mustEqual INTERNAL_SERVER_ERROR
          result.json.as[ApiErrorResponses].message mustEqual INTERNAL_ERROR_RESPONSE
        }
        "respond with a 404 if no data is found" in {
          simulateGet(cidUrl, OK, cidPayload)
          simulateGet(mtdLookupUrl, OK, mtdIdPayload)
          simulateGet(hipUrl, NOT_FOUND, Json.obj().toString)
          val result =
            Await.result(
              client
                .get(URI.create(baseUrl).toURL)
                .setHeader("Authorization" -> "Bearer 1234")
                .execute[HttpResponse], 5.seconds)

          result.status mustEqual NOT_FOUND
          result.json.as[ApiErrorResponses].message mustEqual NOT_FOUND_RESPONSE
        }
        "respond with a 500 for any other response" in {
          simulateGet(cidUrl, OK, cidPayload)
          simulateGet(mtdLookupUrl, OK, mtdIdPayload)
          simulateGet(hipUrl,TOO_MANY_REQUESTS , Json.obj().toString)
          val result =
            Await.result(
              client
                .get(URI.create(baseUrl).toURL)
                .setHeader("Authorization" -> "Bearer 1234")
                .execute[HttpResponse], 5.seconds)

          result.status mustEqual INTERNAL_SERVER_ERROR
          result.json.as[ApiErrorResponses].message mustEqual INTERNAL_ERROR_RESPONSE
        }
      }
    }
  }
}