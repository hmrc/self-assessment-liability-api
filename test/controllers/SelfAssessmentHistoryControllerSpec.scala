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

import config.AppConfig
import models.ApiErrorResponses
import models.ServiceErrors.Invalid_SAUTR
import shared.{HttpWireMock, SpecBase}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.prop.TableDrivenPropertyChecks.forEvery
import org.scalatest.prop.Tables.Table
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsJson, defaultAwaitTimeout, running, status}
import services.SelfAssessmentService
import uk.gov.hmrc.auth.core.AuthConnector

import scala.concurrent.{ExecutionContext, Future}

class SelfAssessmentHistoryControllerSpec extends SpecBase with HttpWireMock {
  private val authConnector: AuthConnector = mock[AuthConnector]
  private val selfAssessmentService: SelfAssessmentService = mock[SelfAssessmentService]
  private val appConfig: AppConfig = mock[AppConfig]
  private val cc = app.injector.instanceOf[ControllerComponents]

  private val invalidUtrTable = Table(
    "Invalid UTR",
    "",
    "          ",
    "?!#*567890",
    "1",
    "123456",
    "aBc4567890",
    "12345678901234567890"
  )

  private val validUtrTable = Table(
    "Valid UTR",
    "1234567890",
    "0123456789",
    "0000000000",
    "9999999999"
  )

  private def controllerMethod(
      utr: String,
      controller: SelfAssessmentHistoryController
  ): Action[AnyContent] = controller.getYourSelfAssessmentData(utr)

  "SelfAssessmentHistoryController" when {
    when(authConnector.authorise(any(), any())(any(), any()))
      .thenReturn(Future.successful(()))

    "getting self assessment data" should {
      "return BadRequest for invalid UTRs" in {
        forEvery(invalidUtrTable) { utr =>
          running(app) {
            val controller =
              new SelfAssessmentHistoryController(authConnector, selfAssessmentService, cc)(
                appConfig,
                ExecutionContext.global
              )
            val result = controllerMethod(utr, controller)(FakeRequest())

            status(result) mustBe BAD_REQUEST
            contentAsJson(result) mustBe ApiErrorResponses(
              Invalid_SAUTR.toString,
              "invalid UTR format"
            ).asJson
          }
        }
      }

      "return Ok for valid UTRs" in {
        forEvery(validUtrTable) { utr =>
          running(app) {
            val controller =
              new SelfAssessmentHistoryController(authConnector, selfAssessmentService, cc)(
                appConfig,
                ExecutionContext.global
              )
            val result = controllerMethod(utr, controller)(FakeRequest())

            status(result) mustBe OK
            contentAsJson(result) mustBe Json.obj("message" -> "Success!")
          }
        }
      }
    }
  }
}
