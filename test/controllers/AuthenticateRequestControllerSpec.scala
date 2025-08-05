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
import models.ServiceErrors.More_Than_One_NINO_Found_For_SAUTR
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.{Action, AnyContent, ControllerComponents, Results}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.SelfAssessmentService
import shared.{HttpWireMock, SpecBase}
import uk.gov.hmrc.auth.core.*
import uk.gov.hmrc.auth.core.AffinityGroup.{Agent, Individual, Organisation}
import uk.gov.hmrc.auth.core.syntax.retrieved.authSyntaxForRetrieved
import uk.gov.hmrc.http.HeaderCarrier
import utils.constants.ErrorMessageConstants.*

import scala.concurrent.{ExecutionContext, Future}

class AuthenticateRequestControllerSpec extends SpecBase with HttpWireMock {
  override lazy val app: Application = new GuiceApplicationBuilder().build()

  private val authConnector: AuthConnector = mock[AuthConnector]
  private val selfAssessmentService: SelfAssessmentService = mock[SelfAssessmentService]
  private val appConfig: AppConfig = mock[AppConfig]
  private val cc = app.injector.instanceOf[ControllerComponents]
  private val utr = "1234567890"
  private val mtdId = "MTDITID123456"
  private val minimumConfidence = ConfidenceLevel.L250
  private val lowConfidence = ConfidenceLevel.L50

  private def methodNeedingAuthentication(
      utr: String,
      authentication: AuthenticateRequestController
  ): Action[AnyContent] = authentication
    .authorisedAction(utr)(_ => Future.successful(Results.Ok))

  "AuthenticateRequestController" when {
    "bearer token is missing" should {
      "return Unauthorized" in {
        when(authConnector.authorise(any(), any())(any(), any()))
          .thenReturn(Future.failed(new MissingBearerToken))

        running(app) {
          val controller =
            new AuthenticateRequestController(cc, selfAssessmentService, authConnector)(
              appConfig,
              ExecutionContext.global
            )
          val result = methodNeedingAuthentication(utr, controller)(FakeRequest())

          status(result) mustBe UNAUTHORIZED
          contentAsJson(result) mustBe ApiErrorResponses(unauthorisedMessage).asJson
        }
      }
    }

    "user has legacy enrolment" should {
      "authenticate the provided utr for Individual affinity group" in {
        when(authConnector.authorise(any(), any())(any(), any()))
          .thenReturn(Future.successful(Some(Individual) and minimumConfidence))

        running(app) {
          val controller =
            new AuthenticateRequestController(cc, selfAssessmentService, authConnector)(
              appConfig,
              ExecutionContext.global
            )
          val result = methodNeedingAuthentication(utr, controller)(FakeRequest())

          status(result) mustBe OK
        }
      }

      "authenticate the provided utr for Organisation affinity group" in {
        when(authConnector.authorise(any(), any())(any(), any()))
          .thenReturn(Future.successful(Some(Organisation) and lowConfidence))

        running(app) {
          val controller =
            new AuthenticateRequestController(cc, selfAssessmentService, authConnector)(
              appConfig,
              ExecutionContext.global
            )
          val result = methodNeedingAuthentication(utr, controller)(FakeRequest())

          status(result) mustBe OK
        }
      }

      "return Unauthorized for Individual with low user confidence level" in {
        when(authConnector.authorise(any(), any())(any(), any()))
          .thenReturn(Future.successful(Some(Individual) and lowConfidence))

        when(selfAssessmentService.getMtdIdFromUtr(eqTo(utr))(any[HeaderCarrier]))
          .thenReturn(Future.successful(mtdId))

        running(app) {
          val controller =
            new AuthenticateRequestController(cc, selfAssessmentService, authConnector)(
              appConfig,
              ExecutionContext.global
            )
          val result = methodNeedingAuthentication(utr, controller)(FakeRequest())

          status(result) mustBe UNAUTHORIZED
          contentAsJson(result) mustBe ApiErrorResponses(unauthorisedMessage).asJson
        }
      }
    }

    "user has MTD enrolment but not legacy enrolment" should {
      "authenticate successfully for Individual affinity group" in {
        when(authConnector.authorise(any(), any())(any(), any()))
          .thenReturn(Future.failed(new AuthorisationException("not authorised") {}))
          .thenReturn(Future.successful(Some(Individual) and minimumConfidence))

        when(selfAssessmentService.getMtdIdFromUtr(eqTo(utr))(any[HeaderCarrier]))
          .thenReturn(Future.successful(mtdId))

        running(app) {
          val controller =
            new AuthenticateRequestController(cc, selfAssessmentService, authConnector)(
              appConfig,
              ExecutionContext.global
            )
          val result = methodNeedingAuthentication(utr, controller)(FakeRequest())

          status(result) mustBe OK
        }
      }

      "authenticate successfully for Organisation affinity group" in {
        when(authConnector.authorise(any(), any())(any(), any()))
          .thenReturn(Future.failed(new AuthorisationException("not authorised") {}))
          .thenReturn(Future.successful(Some(Organisation) and lowConfidence))

        when(selfAssessmentService.getMtdIdFromUtr(eqTo(utr))(any[HeaderCarrier]))
          .thenReturn(Future.successful(mtdId))

        running(app) {
          val controller =
            new AuthenticateRequestController(cc, selfAssessmentService, authConnector)(
              appConfig,
              ExecutionContext.global
            )
          val result = methodNeedingAuthentication(utr, controller)(FakeRequest())

          status(result) mustBe OK
        }
      }

      "return Unauthorized for Individual with low user confidence level" in {
        when(authConnector.authorise(any(), any())(any(), any()))
          .thenReturn(Future.failed(new AuthorisationException("not authorised") {}))
          .thenReturn(Future.successful(Some(Individual) and lowConfidence))

        when(selfAssessmentService.getMtdIdFromUtr(eqTo(utr))(any[HeaderCarrier]))
          .thenReturn(Future.successful(mtdId))

        running(app) {
          val controller =
            new AuthenticateRequestController(cc, selfAssessmentService, authConnector)(
              appConfig,
              ExecutionContext.global
            )
          val result = methodNeedingAuthentication(utr, controller)(FakeRequest())

          status(result) mustBe UNAUTHORIZED
          contentAsJson(result) mustBe ApiErrorResponses(unauthorisedMessage).asJson
        }
      }
    }

    "user is an agent" should {
      "authenticate successfully when agents are allowed and delegation is established" in {
        when(authConnector.authorise(any(), any())(any(), any()))
          .thenReturn(Future.failed(new AuthorisationException("not authorised") {}))
          .thenReturn(Future.successful(Some(Agent) and lowConfidence))
          .thenReturn(Future.successful(()))

        when(selfAssessmentService.getMtdIdFromUtr(eqTo(utr))(any[HeaderCarrier]))
          .thenReturn(Future.successful(mtdId))

        when(appConfig.agentsAllowed).thenReturn(true)

        running(app) {
          val controller =
            new AuthenticateRequestController(cc, selfAssessmentService, authConnector)(
              appConfig,
              ExecutionContext.global
            )
          val result = methodNeedingAuthentication(utr, controller)(FakeRequest())

          status(result) mustBe OK
        }
      }

      "return Unauthorized when agents are not allowed" in {
        when(authConnector.authorise(any(), any())(any(), any()))
          .thenReturn(Future.failed(new AuthorisationException("not authorised") {}))
          .thenReturn(Future.successful(Some(Agent) and lowConfidence))

        when(selfAssessmentService.getMtdIdFromUtr(eqTo(utr))(any[HeaderCarrier]))
          .thenReturn(Future.successful(mtdId))

        when(appConfig.agentsAllowed).thenReturn(false)

        running(app) {
          val controller =
            new AuthenticateRequestController(cc, selfAssessmentService, authConnector)(
              appConfig,
              ExecutionContext.global
            )
          val result = methodNeedingAuthentication(utr, controller)(FakeRequest())

          status(result) mustBe UNAUTHORIZED
          contentAsJson(result) mustBe ApiErrorResponses(unauthorisedMessage).asJson
        }
      }

      "return InternalServerError when agent/client handshake is not established" in {
        when(authConnector.authorise(any(), any())(any(), any()))
          .thenReturn(Future.failed(new AuthorisationException("not authorised") {}))
          .thenReturn(Future.successful(Some(Agent) and lowConfidence))
          .thenReturn(Future.failed(new AuthorisationException("not authorised") {}))

        when(selfAssessmentService.getMtdIdFromUtr(eqTo(utr))(any[HeaderCarrier]))
          .thenReturn(Future.successful(mtdId))

        when(appConfig.agentsAllowed).thenReturn(true)

        running(app) {
          val controller =
            new AuthenticateRequestController(cc, selfAssessmentService, authConnector)(
              appConfig,
              ExecutionContext.global
            )
          val result = methodNeedingAuthentication(utr, controller)(FakeRequest())

          status(result) mustBe INTERNAL_SERVER_ERROR
          contentAsJson(result) mustBe ApiErrorResponses(internalErrorMessage).asJson
        }
      }
    }

    "error handling" should {
      "return InternalServerError when multiple NINOs are found for a UTR" in {
        when(authConnector.authorise(any(), any())(any(), any()))
          .thenReturn(Future.failed(new AuthorisationException("not authorised") {}))

        when(selfAssessmentService.getMtdIdFromUtr(eqTo(utr))(any[HeaderCarrier]))
          .thenReturn(Future.failed(More_Than_One_NINO_Found_For_SAUTR))

        running(app) {
          val controller =
            new AuthenticateRequestController(cc, selfAssessmentService, authConnector)(
              appConfig,
              ExecutionContext.global
            )
          val result = methodNeedingAuthentication(utr, controller)(FakeRequest())

          status(result) mustBe INTERNAL_SERVER_ERROR
          contentAsJson(result) mustBe ApiErrorResponses(internalErrorMessage).asJson
        }
      }

      "return InternalServerError when user doesn't have any self assessment enrolments" in {
        when(authConnector.authorise(any(), any())(any(), any()))
          .thenReturn(Future.failed(new AuthorisationException("not authorised") {}))
          .thenReturn(Future.failed(new AuthorisationException("not authorised") {}))

        when(selfAssessmentService.getMtdIdFromUtr(eqTo(utr))(any[HeaderCarrier]))
          .thenReturn(Future.successful(mtdId))

        running(app) {
          val controller =
            new AuthenticateRequestController(cc, selfAssessmentService, authConnector)(
              appConfig,
              ExecutionContext.global
            )
          val result = methodNeedingAuthentication(utr, controller)(FakeRequest())

          status(result) mustBe INTERNAL_SERVER_ERROR
          contentAsJson(result) mustBe ApiErrorResponses(internalErrorMessage).asJson
        }
      }

      "return InternalServerError for unsupported affinity group" in {
        when(authConnector.authorise(any(), any())(any(), any()))
          .thenReturn(Future.failed(new AuthorisationException("not authorised") {}))
          .thenReturn(Future.successful(None and lowConfidence))

        when(selfAssessmentService.getMtdIdFromUtr(eqTo(utr))(any[HeaderCarrier]))
          .thenReturn(Future.successful(mtdId))

        running(app) {
          val controller =
            new AuthenticateRequestController(cc, selfAssessmentService, authConnector)(
              appConfig,
              ExecutionContext.global
            )
          val result = methodNeedingAuthentication(utr, controller)(FakeRequest())

          status(result) mustBe INTERNAL_SERVER_ERROR
          contentAsJson(result) mustBe ApiErrorResponses(internalErrorMessage).asJson
        }
      }

      "return InternalServerError for general auth errors" in {
        when(authConnector.authorise(any(), any())(any(), any()))
          .thenReturn(Future.failed(new RuntimeException("unknown error")))

        running(app) {
          val controller =
            new AuthenticateRequestController(cc, selfAssessmentService, authConnector)(
              appConfig,
              ExecutionContext.global
            )
          val result = methodNeedingAuthentication(utr, controller)(FakeRequest())

          status(result) mustBe INTERNAL_SERVER_ERROR
          contentAsJson(result) mustBe ApiErrorResponses(internalErrorMessage).asJson
        }
      }
    }
  }
}
