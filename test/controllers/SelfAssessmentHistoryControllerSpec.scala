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
import models.ServiceErrors.*
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.prop.TableDrivenPropertyChecks.forEvery
import org.scalatest.prop.Tables.Table
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.mvc.ControllerComponents
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.SelfAssessmentService
import shared.{HttpWireMock, SpecBase}
import uk.gov.hmrc.auth.core.AffinityGroup.Individual
import uk.gov.hmrc.auth.core.{AuthConnector, ConfidenceLevel}
import uk.gov.hmrc.auth.core.syntax.retrieved.authSyntaxForRetrieved
import utils.constants.ErrorMessageConstants.*

import scala.concurrent.Future

class SelfAssessmentHistoryControllerSpec extends SpecBase with HttpWireMock {
  override lazy val app: Application = new GuiceApplicationBuilder().build()

  private val authConnector: AuthConnector = mock[AuthConnector]
  private val selfAssessmentService: SelfAssessmentService = mock[SelfAssessmentService]
  private val appConfig: AppConfig = mock[AppConfig]
  private val cc: ControllerComponents = app.injector.instanceOf[ControllerComponents]
  private val utr: String = "1234567890"
  private val fromDate: Option[String] = Some("2025-04-06")
  private val controller =
    new SelfAssessmentHistoryController(
      authConnector,
      selfAssessmentService,
      cc
    )(
      appConfig,
      ec
    )

  "SelfAssessmentHistoryControllerSpec" when {
    when(authConnector.authorise(any(), any())(any(), any()))
      .thenReturn(Future.successful(Some(Individual) and ConfidenceLevel.L250))

    "getting self assessment data" should {
      "return details as JSON when successful" in {
        when(selfAssessmentService.getHipData(any(), any())(any()))
          .thenReturn(Future.successful(hipResponse))

        running(app) {
          val result = controller.getYourSelfAssessmentData(utr, fromDate)(FakeRequest())

          status(result) mustBe OK
          contentAsJson(result) mustBe Json.parse(jsonSuccessResponse)
        }
      }

      "return Bad Request for relevant HIP error(s)" in {
        when(selfAssessmentService.getHipData(any(), any())(any()))
          .thenReturn(Future.failed(No_Payments_Found_For_UTR))

        running(app) {
          val result = controller.getYourSelfAssessmentData(utr, fromDate)(FakeRequest())

          status(result) mustBe BAD_REQUEST
          contentAsJson(result) mustBe ApiErrorResponses(badRequestMessage).asJson
        }
      }

      "return Unauthorized for relevant HIP error(s)" in {
        when(selfAssessmentService.getHipData(any(), any())(any()))
          .thenReturn(Future.failed(HIP_Unauthorised))

        running(app) {
          val result = controller.getYourSelfAssessmentData(utr, fromDate)(FakeRequest())

          status(result) mustBe UNAUTHORIZED
          contentAsJson(result) mustBe ApiErrorResponses(unauthorisedMessage).asJson
        }
      }

      "return Forbidden for relevant HIP error(s)" in {
        when(selfAssessmentService.getHipData(any(), any())(any()))
          .thenReturn(Future.failed(HIP_Forbidden))

        running(app) {
          val result = controller.getYourSelfAssessmentData(utr, fromDate)(FakeRequest())

          status(result) mustBe FORBIDDEN
          contentAsJson(result) mustBe ApiErrorResponses(forbiddenMessage).asJson
        }
      }

      "return Internal Server Error for relevant HIP error(s)" in {
        forEvery(
          Table(
            "Internal Server Errors",
            Invalid_Correlation_Id,
            Invalid_UTR,
            HIP_Server_Error,
            HIP_Bad_Gateway,
            Downstream_Error,
            Throwable()
          )
        ) { serviceError =>
          when(selfAssessmentService.getHipData(any(), any())(any()))
            .thenReturn(Future.failed(serviceError))

          running(app) {
            val result = controller.getYourSelfAssessmentData(utr, fromDate)(FakeRequest())

            status(result) mustBe INTERNAL_SERVER_ERROR
            contentAsJson(result) mustBe ApiErrorResponses(internalErrorMessage).asJson
          }
        }
      }

      "return Service Unavailable for relevant HIP error(s)" in {
        when(selfAssessmentService.getHipData(any(), any())(any()))
          .thenReturn(Future.failed(HIP_Service_Unavailable))

        running(app) {
          val result = controller.getYourSelfAssessmentData(utr, fromDate)(FakeRequest())

          status(result) mustBe SERVICE_UNAVAILABLE
          contentAsJson(result) mustBe ApiErrorResponses(serviceUnavailableMessage).asJson
        }
      }
    }
  }
}
