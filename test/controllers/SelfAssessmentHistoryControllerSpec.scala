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
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsJson, defaultAwaitTimeout, running, status}
import services.{SelfAssessmentService, UtrValidationService}
import uk.gov.hmrc.auth.core.AuthConnector

import scala.concurrent.Future

class SelfAssessmentHistoryControllerSpec extends SpecBase with HttpWireMock {
  private val authConnector: AuthConnector = mock[AuthConnector]
  private val selfAssessmentService: SelfAssessmentService = mock[SelfAssessmentService]
  private val utrValidationService: UtrValidationService = mock[UtrValidationService]
  private val appConfig: AppConfig = mock[AppConfig]
  private val cc = app.injector.instanceOf[ControllerComponents]
  private val controller =
    new SelfAssessmentHistoryController(
      authConnector,
      selfAssessmentService,
      utrValidationService,
      cc
    )(
      appConfig,
      ec
    )

  private def controllerMethod(
      utr: String,
      controller: SelfAssessmentHistoryController
  ): Action[AnyContent] = controller.getYourSelfAssessmentData(utr)

  "SelfAssessmentHistoryController" when {
    when(authConnector.authorise(any(), any())(any(), any()))
      .thenReturn(Future.successful(()))

    "getting self assessment data" should {
      "return BadRequest for an invalid UTR" in {
        when(utrValidationService.isValidUtr(any())).thenReturn(false)

        running(app) {
          val result = controllerMethod("!£$%", controller)(FakeRequest())

          status(result) mustBe BAD_REQUEST
          contentAsJson(result) mustBe ApiErrorResponses(
            Invalid_SAUTR.toString,
            "invalid UTR format"
          ).asJson
        }
      }

      "return Ok for a valid UTR" in {
        when(utrValidationService.isValidUtr(any())).thenReturn(true)

        running(app) {
          val result = controllerMethod("1234567890", controller)(FakeRequest())

          status(result) mustBe OK
          contentAsJson(result) mustBe Json.obj("message" -> "Success!")
        }
      }
    }
  }
}
