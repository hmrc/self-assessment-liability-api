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

import models.ApiErrorResponses
import models.ServiceErrors.{
  Downstream_Error,
  Invalid_SAUTR,
  More_Than_One_NINO_Found_For_SAUTR,
  Not_Allowed
}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{times, verify, when}
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
import uk.gov.hmrc.http.HeaderCarrier
import utils.constants.EnrolmentConstants.*

import scala.concurrent.Future

class AuthenticateRequestControllerSpec extends SpecBase with HttpWireMock {

  override lazy val app: Application = new GuiceApplicationBuilder().build()

  private val validUtr = "1234567890"
  private val invalidUtr = "1231254243213213"
  private val mtdId = "MTDITID123456"

  private val mockAuthConnector: AuthConnector = mock[AuthConnector]
  private val mockSelfAssessmentService: SelfAssessmentService = mock[SelfAssessmentService]

  private val cc: ControllerComponents = app.injector.instanceOf[ControllerComponents]

  private val controller: AuthenticateRequestController = {
    new AuthenticateRequestController(cc, mockSelfAssessmentService, mockAuthConnector)(
      ec
    )
  }

  override def beforeEach(): Unit = {
    super.beforeEach()
    org.mockito.Mockito.reset(mockAuthConnector, mockSelfAssessmentService)
  }
  private def authenticatedAction(
      utr: String,
      controller: AuthenticateRequestController
  ): Action[AnyContent] = controller
    .authorisedAction(utr)(_ => Future.successful(Results.Ok))

  "authenticating Individuals" should {

    "allow access when they have legacy SA enrollment matching the UTR" in {

      when(mockAuthConnector.authorise(any(), any())(any(), any()))
        .thenReturn(Future.successful(()))

      running(app) {
        val result = authenticatedAction(validUtr, controller)(FakeRequest())

        status(result) mustBe OK

        val expectedPredicate =
          (Individual and Enrolment(IR_SA_Enrolment_Key).withIdentifier(
            IR_SA_Identifier,
            validUtr
          )) or
            (Organisation and Enrolment(IR_SA_Enrolment_Key).withIdentifier(
              IR_SA_Identifier,
              validUtr
            ))

        verify(mockAuthConnector, times(1))
          .authorise(eqTo(expectedPredicate), any())(any(), any())
      }
    }

    "allow access for MTD enrollment" in {

      when(mockAuthConnector.authorise(any(), any())(any(), any()))
        .thenReturn(Future.failed(UnsupportedAuthProvider()))
        .thenReturn(Future.successful(Some(Individual)))

      when(mockSelfAssessmentService.getMtdIdFromUtr(eqTo(validUtr))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mtdId))

      running(app) {
        val result = authenticatedAction(validUtr, controller)(FakeRequest())
        status(result) mustBe OK
        val inOrder = org.mockito.Mockito.inOrder(mockAuthConnector)

        val firstPredicate =
          (Individual and Enrolment(IR_SA_Enrolment_Key).withIdentifier(
            IR_SA_Identifier,
            validUtr
          )) or
            (Organisation and Enrolment(IR_SA_Enrolment_Key).withIdentifier(
              IR_SA_Identifier,
              validUtr
            ))

        val secondPredicate =
          (Individual and Enrolment(Mtd_Enrolment_Key).withIdentifier(Mtd_Identifier, mtdId)) or
            (Organisation and Enrolment(Mtd_Enrolment_Key).withIdentifier(Mtd_Identifier, mtdId)) or
            (Agent and Enrolment(ASA_Enrolment_Key))
        inOrder.verify(mockAuthConnector).authorise(eqTo(firstPredicate), any())(any(), any())
        inOrder.verify(mockAuthConnector).authorise(eqTo(secondPredicate), any())(any(), any())
      }
    }

  }

  "authenticating an Organisation user" should {

    "allow access when they have legacy SA enrollment matching the UTR" in {
      when(mockAuthConnector.authorise(any(), any())(any(), any()))
        .thenReturn(Future.successful(()))

      running(app) {
        val result = authenticatedAction(validUtr, controller)(FakeRequest())

        status(result) mustBe OK
      }
    }

    "allow access when they have MTD enrollment" in {
      when(mockAuthConnector.authorise(any(), any())(any(), any()))
        .thenReturn(Future.failed(InsufficientEnrolments()))
        .thenReturn(Future.successful(Some(Organisation)))

      when(mockSelfAssessmentService.getMtdIdFromUtr(eqTo(validUtr))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mtdId))

      running(app) {
        val result = authenticatedAction(validUtr, controller)(FakeRequest())

        status(result) mustBe OK

        verify(mockAuthConnector, times(2)).authorise(any(), any())(any(), any())
      }
    }
  }

  "authenticating an Agent user" should {

    "allow access when client delegation is established with MTD enrollment" in {
      when(mockAuthConnector.authorise(any(), any())(any(), any()))
        .thenReturn(Future.failed(InsufficientEnrolments()))
        .thenReturn(Future.successful(Some(Agent)))
        .thenReturn(Future.successful(()))

      when(mockSelfAssessmentService.getMtdIdFromUtr(eqTo(validUtr))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mtdId))

      running(app) {
        val result = authenticatedAction(validUtr, controller)(FakeRequest())

        status(result) mustBe OK
        verify(mockAuthConnector, times(3)).authorise(any(), any())(any(), any())
      }
    }

    "allow access when client delegation is established with SA enrollment" in {
      when(mockAuthConnector.authorise(any(), any())(any(), any()))
        .thenReturn(Future.failed(InsufficientEnrolments()))
        .thenReturn(Future.successful(Some(Agent)))
        .thenReturn(Future.successful(()))

      when(mockSelfAssessmentService.getMtdIdFromUtr(eqTo(validUtr))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mtdId))

      running(app) {
        val result = authenticatedAction(validUtr, controller)(FakeRequest())

        status(result) mustBe OK
      }
    }

    "return InternalServerError when agent/client handshake is not established" in {
      when(mockAuthConnector.authorise(any(), any())(any(), any()))
        .thenReturn(Future.failed(InsufficientEnrolments()))
        .thenReturn(Future.successful(Some(Agent)))
        .thenReturn(Future.failed(InsufficientEnrolments()))

      when(mockSelfAssessmentService.getMtdIdFromUtr(eqTo(validUtr))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mtdId))

      running(app) {
        val result = authenticatedAction(validUtr, controller)(FakeRequest())

        status(result) mustBe INTERNAL_SERVER_ERROR
        contentAsJson(result) mustBe ApiErrorResponses(
          Downstream_Error.toString,
          "agent/client handshake was not established"
        ).asJson
      }
    }
  }

  "handling errors" should {
    "return Unauthorized when bearer token is missing" in {

      when(mockAuthConnector.authorise(any(), any())(any(), any()))
        .thenReturn(Future.failed(new MissingBearerToken))

      running(app) {
        val result = authenticatedAction(validUtr, controller)(FakeRequest())

        status(result) mustBe UNAUTHORIZED
        contentAsJson(result) mustBe ApiErrorResponses(
          Not_Allowed.toString,
          "missing auth token"
        ).asJson
      }
    }

    "return InternalServerError when MTD ID lookup fails" in {
      when(mockAuthConnector.authorise(any(), any())(any(), any()))
        .thenReturn(Future.failed(InsufficientEnrolments()))

      when(mockSelfAssessmentService.getMtdIdFromUtr(eqTo(validUtr))(any[HeaderCarrier]))
        .thenReturn(Future.failed(More_Than_One_NINO_Found_For_SAUTR))

      running(app) {
        val result = authenticatedAction(validUtr, controller)(FakeRequest())

        status(result) mustBe INTERNAL_SERVER_ERROR
        contentAsJson(result) mustBe ApiErrorResponses(
          Downstream_Error.toString,
          "calls to get mtdid failed for some reason"
        ).asJson
      }
    }

    "return InternalServerError when user doesn't have any sa enrolments" in {
      when(mockAuthConnector.authorise(any(), any())(any(), any()))
        .thenReturn(Future.failed(InsufficientEnrolments()))
        .thenReturn(Future.failed(InsufficientEnrolments()))

      when(mockSelfAssessmentService.getMtdIdFromUtr(eqTo(validUtr))(any[HeaderCarrier]))
        .thenReturn(Future.successful(mtdId))

      running(app) {
        val result = authenticatedAction(validUtr, controller)(FakeRequest())

        status(result) mustBe INTERNAL_SERVER_ERROR
        contentAsJson(result) mustBe ApiErrorResponses(
          Downstream_Error.toString,
          "user didnt have any of the self assessment enrolments"
        ).asJson
      }
    }

    "return InternalServerError for general auth errors" in {

      when(mockAuthConnector.authorise(any(), any())(any(), any()))
        .thenReturn(Future.failed(new RuntimeException()))

      running(app) {
        val result = authenticatedAction(validUtr, controller)(FakeRequest())

        status(result) mustBe INTERNAL_SERVER_ERROR
        contentAsJson(result) mustBe ApiErrorResponses(
          Downstream_Error.toString,
          "auth returned an error of some kind"
        ).asJson
      }
    }
    "validating UTR format" should {

      "return BadRequest if UTR is invalid" in {

        running(app) {
          val result = authenticatedAction(invalidUtr, controller)(FakeRequest())

          status(result) mustBe BAD_REQUEST
          contentAsJson(result) mustBe ApiErrorResponses(
            Invalid_SAUTR.toString,
            "invalid UTR format"
          ).asJson
        }
      }

      "return BadRequest if UTR is empty" in {

        running(app) {
          val result = authenticatedAction("", controller)(FakeRequest())

          status(result) mustBe BAD_REQUEST
          contentAsJson(result) mustBe ApiErrorResponses(
            Invalid_SAUTR.toString,
            "invalid UTR format"
          ).asJson
        }
      }
    }
  }
}
