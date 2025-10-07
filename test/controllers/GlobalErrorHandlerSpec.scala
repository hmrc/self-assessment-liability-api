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

import models.ServiceErrors.*
import models.{ApiErrorResponses, ServiceErrors}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.JsValue
import play.api.mvc.{RequestHeader, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.auth.core.AuthorisationException
import utils.constants.ErrorMessageConstansts.*
import scala.concurrent.Future
import scala.util.Random

class GlobalErrorHandlerSpec extends AnyWordSpec with Matchers with MockitoSugar with ScalaFutures {

  val errorHandler = new GlobalErrorHandler()
  val fakeRequest: RequestHeader = FakeRequest("GET", "/test")
  val customMessage = "Some message"

  "GlobalErrorHandler" should {

    "onClientError" should {

      "return BadRequest with correct JSON for 400 status code" in {
        val result: Future[Result] =
          errorHandler.onClientError(fakeRequest, BAD_REQUEST, customMessage)

        status(result) shouldBe BAD_REQUEST

        val expectedJson = ApiErrorResponses(BAD_REQUEST_RESPONSE).asJson
        contentAsJson(result) shouldBe expectedJson
      }

      "return NotFound with correct JSON for 404 status code" in {
        val result: Future[Result] =
          errorHandler.onClientError(fakeRequest, NOT_FOUND, customMessage)

        status(result) shouldBe NOT_FOUND

        val expectedJson = ApiErrorResponses(NOT_FOUND_RESPONSE).asJson
        contentAsJson(result) shouldBe expectedJson
      }

      "handle other 4xx responses" in {

        val result: Future[Result] =
          errorHandler.onClientError(fakeRequest, UNPROCESSABLE_ENTITY, customMessage)

        status(result) shouldBe UNPROCESSABLE_ENTITY

        val expectedJson = ApiErrorResponses(customMessage).asJson
        contentAsJson(result) shouldBe expectedJson
      }
    }

    "onServerError" should {

      "return InternalServerError for Downstream_Error" in {
        val result: Future[Result] = errorHandler.onServerError(fakeRequest, Downstream_Error)

        status(result) shouldBe INTERNAL_SERVER_ERROR

        val expectedJson = ApiErrorResponses(INTERNAL_ERROR_RESPONSE).asJson
        contentAsJson(result) shouldBe expectedJson
      }

      "return InternalServerError for Json_Validation_Error" in {
        val result: Future[Result] = errorHandler.onServerError(fakeRequest, Json_Validation_Error)

        status(result) shouldBe INTERNAL_SERVER_ERROR

        val expectedJson = ApiErrorResponses(INTERNAL_ERROR_RESPONSE).asJson
        contentAsJson(result) shouldBe expectedJson
      }

      "return NotFound for No_Data_Found_Error" in {
        val result: Future[Result] = errorHandler.onServerError(fakeRequest, No_Data_Found_Error)

        status(result) shouldBe NOT_FOUND

        val expectedJson = ApiErrorResponses(NOT_FOUND_RESPONSE).asJson
        contentAsJson(result) shouldBe expectedJson
      }

      "return BadRequest for Invalid_Start_Date_Error" in {
        val result: Future[Result] =
          errorHandler.onServerError(fakeRequest, Invalid_Start_Date_Error)

        status(result) shouldBe BAD_REQUEST

        val expectedJson = ApiErrorResponses(BAD_REQUEST_RESPONSE).asJson
        contentAsJson(result) shouldBe expectedJson
      }

      "return BadRequest for Invalid_Utr_Error" in {
        val result: Future[Result] = errorHandler.onServerError(fakeRequest, Invalid_Utr_Error)

        status(result) shouldBe BAD_REQUEST

        val expectedJson = ApiErrorResponses(BAD_REQUEST_RESPONSE).asJson
        contentAsJson(result) shouldBe expectedJson
      }

      "return BadRequest for NoActiveSession" in {
        val noActiveSessionException = AuthorisationException.fromString("InvalidBearerToken")
        val result: Future[Result] =
          errorHandler.onServerError(fakeRequest, noActiveSessionException)

        status(result) shouldBe BAD_REQUEST

        val expectedJson = ApiErrorResponses(BAD_REQUEST_RESPONSE).asJson
        contentAsJson(result) shouldBe expectedJson
      }

      "return Unauthorized for Unauthorised_Error" in {
        val result: Future[Result] = errorHandler.onServerError(fakeRequest, Unauthorised_Error)

        status(result) shouldBe UNAUTHORIZED

        val expectedJson = ApiErrorResponses(UNAUTHORISED_RESPONSE).asJson
        contentAsJson(result) shouldBe expectedJson
      }

      "return Forbidden for Forbidden_Error" in {
        val result: Future[Result] = errorHandler.onServerError(fakeRequest, Forbidden_Error)

        status(result) shouldBe FORBIDDEN

        val expectedJson = ApiErrorResponses(FORBIDDEN_RESPONSE).asJson
        contentAsJson(result) shouldBe expectedJson
      }

      "return Forbidden for AuthorisationException" in {
        val authException = AuthorisationException.fromString("UnsupportedAffinityGroup")
        val result: Future[Result] = errorHandler.onServerError(fakeRequest, authException)

        status(result) shouldBe FORBIDDEN

        val expectedJson = ApiErrorResponses(FORBIDDEN_RESPONSE).asJson
        contentAsJson(result) shouldBe expectedJson
      }

      "return ServiceUnavailable for any other exception" in {
        val genericException = new RuntimeException("Generic runtime exception")
        val nullPointerException = new NullPointerException("Null pointer exception")
        val noSuchElementException = new NoSuchElementException("Element not found")
        val randomException = new Random()
          .shuffle(List(genericException, nullPointerException, noSuchElementException))
          .head
        val result: Future[Result] = errorHandler.onServerError(fakeRequest, randomException)

        status(result) shouldBe SERVICE_UNAVAILABLE

        val expectedJson = ApiErrorResponses(SERVICE_UNAVAILABLE_RESPONSE).asJson
        contentAsJson(result) shouldBe expectedJson
      }
    }
  }
}
