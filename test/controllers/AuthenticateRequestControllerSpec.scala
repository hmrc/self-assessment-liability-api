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
import models.ServiceErrors.{Downstream_Error, Service_Currently_Unavailable}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.*
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.{Action, AnyContent, ControllerComponents, Results}
import play.api.test.FakeRequest
import play.api.test.Helpers.{running, *}
import services.SelfAssessmentService
import shared.{HttpWireMock, SpecBase}
import uk.gov.hmrc.auth.core.*
import uk.gov.hmrc.auth.core.AffinityGroup.{Agent, Individual, Organisation}
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.retrieve.{EmptyRetrieval, ~}
import uk.gov.hmrc.auth.core.syntax.retrieved.authSyntaxForRetrieved
import utils.SelfAssessmentEnrolments.{
  delegatedEnrolments,
  legacySaEnrolment,
  mtdSaEnrolment,
  principleAgentEnrolments
}
import utils.constants.ErrorMessageConstansts.*

import scala.concurrent.{ExecutionContext, Future}

class AuthenticateRequestControllerSpec extends SpecBase with HttpWireMock {
  override lazy val app: Application = new GuiceApplicationBuilder().build()

  private val authConnector: AuthConnector = mock[AuthConnector]
  private val selfAssessmentService: SelfAssessmentService = mock[SelfAssessmentService]
  private val cc = app.injector.instanceOf[ControllerComponents]
  private val validUtr = "1234567890"
  private val invalidUtr = "12345678901"
  private val invalidUtrWithSpecialChars = "123-456-78"
  private val minimumConfidence = ConfidenceLevel.L250
  private val lowConfidence = ConfidenceLevel.L50

  private def methodNeedingAuthentication(
      utr: String,
      authentication: AuthenticateRequestController
  ): Action[AnyContent] = authentication
    .authorisedAction(utr)(_ => Future.successful(Results.Ok))

  "AuthenticateRequestController" when {
    "UTR validation" should {
      "return BadRequest for invalid UTR that exceeds max length" in {
        running(app) {
          val controller =
            new AuthenticateRequestController(cc, selfAssessmentService, authConnector)(
              ExecutionContext.global
            )
          val result = methodNeedingAuthentication(invalidUtr, controller)(FakeRequest())

          status(result) mustBe BAD_REQUEST
          contentAsJson(result) mustBe ApiErrorResponses(
            badRequestMessage
          ).asJson
        }
      }

      "return BadRequest for invalid UTR with special characters" in {
        running(app) {
          val controller =
            new AuthenticateRequestController(cc, selfAssessmentService, authConnector)(
              ExecutionContext.global
            )
          val result =
            methodNeedingAuthentication(invalidUtrWithSpecialChars, controller)(FakeRequest())

          status(result) mustBe BAD_REQUEST
          contentAsJson(result) mustBe ApiErrorResponses(
            badRequestMessage
          ).asJson
        }
      }
    }

    "bearer token is missing" should {
      "return BadRequest" in {
        when(authConnector.authorise(any(), any())(any(), any()))
          .thenReturn(Future.failed(new NoActiveSession("No active session") {}))

        running(app) {
          val controller =
            new AuthenticateRequestController(cc, selfAssessmentService, authConnector)(
              ExecutionContext.global
            )
          val result = methodNeedingAuthentication(validUtr, controller)(FakeRequest())

          status(result) mustBe BAD_REQUEST
          contentAsJson(result) mustBe ApiErrorResponses(
            badRequestMessage
          ).asJson
        }
      }
      "return service unavailable if auth is down" in {
        when(
          authConnector.authorise(
            any(),
            eqTo(Retrievals.affinityGroup and Retrievals.confidenceLevel)
          )(any(), any())
        )
          .thenReturn(Future.failed(Downstream_Error))
        running(app) {
          val controller =
            new AuthenticateRequestController(cc, selfAssessmentService, authConnector)(
              ExecutionContext.global
            )
          val result = methodNeedingAuthentication(validUtr, controller)(FakeRequest())
          status(result) mustBe SERVICE_UNAVAILABLE
          contentAsJson(result) mustBe ApiErrorResponses(
            serviceUnavailableMessage
          ).asJson
        }
      }
    }

    "Individual Affinity" should {

      "return success if confidence level is 250 for legacy SA enrolement" in {
        when(
          authConnector.authorise(
            any(),
            eqTo(Retrievals.affinityGroup and Retrievals.confidenceLevel)
          )(any(), any())
        )
          .thenReturn(Future.successful(new ~(Some(Individual), minimumConfidence)))

        when(
          authConnector
            .authorise(eqTo(legacySaEnrolment(validUtr)), eqTo(EmptyRetrieval))(any(), any())
        )
          .thenReturn(Future.successful(()))

        running(app) {
          val controller =
            new AuthenticateRequestController(cc, selfAssessmentService, authConnector)(
              ExecutionContext.global
            )
          val result = methodNeedingAuthentication(validUtr, controller)(FakeRequest())

          status(result) mustBe OK
        }
      }
      "return Unauthorized if confidence level is below 250" in {
        when(authConnector.authorise(any(), any())(any(), any()))
          .thenReturn(Future.successful(Some(Individual) and lowConfidence))

        running(app) {
          val controller =
            new AuthenticateRequestController(cc, selfAssessmentService, authConnector)(
              ExecutionContext.global
            )
          val result = methodNeedingAuthentication(validUtr, controller)(FakeRequest())

          status(result) mustBe UNAUTHORIZED
          contentAsJson(result) mustBe ApiErrorResponses(
            unauthorisedMessage
          ).asJson
        }
      }

      "return ok if they meet the minimum threshold with an mtd enrolment" in {
        when(
          authConnector.authorise(
            any(),
            eqTo(Retrievals.affinityGroup and Retrievals.confidenceLevel)
          )(any(), any())
        )
          .thenReturn(Future.successful(new ~(Some(Individual), minimumConfidence)))

        when(
          authConnector
            .authorise(eqTo(legacySaEnrolment(validUtr)), eqTo(EmptyRetrieval))(any(), any())
        )
          .thenReturn(Future.failed(InsufficientEnrolments()))

        when(selfAssessmentService.getMtdIdFromUtr(eqTo(validUtr))(any()))
          .thenReturn(Future.successful("mtdId"))

        when(
          authConnector.authorise(eqTo(mtdSaEnrolment("mtdId")), eqTo(EmptyRetrieval))(any(), any())
        )
          .thenReturn(Future.successful(()))
        running(app) {
          val controller =
            new AuthenticateRequestController(cc, selfAssessmentService, authConnector)(
              ExecutionContext.global
            )
          val result = methodNeedingAuthentication(validUtr, controller)(FakeRequest())

          status(result) mustBe OK
        }
      }

      "return unauthorised if they do not have any of the accepted enrolments" in {
        when(
          authConnector.authorise(
            any(),
            eqTo(Retrievals.affinityGroup and Retrievals.confidenceLevel)
          )(any(), any())
        )
          .thenReturn(Future.successful(new ~(Some(Individual), minimumConfidence)))

        when(
          authConnector
            .authorise(eqTo(legacySaEnrolment(validUtr)), eqTo(EmptyRetrieval))(any(), any())
        )
          .thenReturn(Future.failed(InsufficientEnrolments()))

        when(selfAssessmentService.getMtdIdFromUtr(eqTo(validUtr))(any()))
          .thenReturn(Future.successful("mtdId"))
        when(
          authConnector.authorise(eqTo(mtdSaEnrolment("mtdId")), eqTo(EmptyRetrieval))(any(), any())
        )
          .thenReturn(Future.failed(InsufficientEnrolments()))

        running(app) {
          val controller =
            new AuthenticateRequestController(cc, selfAssessmentService, authConnector)(
              ExecutionContext.global
            )
          val result = methodNeedingAuthentication(validUtr, controller)(FakeRequest())

          status(result) mustBe FORBIDDEN
          contentAsJson(result) mustBe ApiErrorResponses(
            forbiddenMessage
          ).asJson
        }
      }
      "return service unavailable if call to fetch mtd id fails due to service being unvailable" in {
        when(
          authConnector.authorise(
            any(),
            eqTo(Retrievals.affinityGroup and Retrievals.confidenceLevel)
          )(any(), any())
        )
          .thenReturn(Future.successful(new ~(Some(Individual), minimumConfidence)))

        when(
          authConnector
            .authorise(eqTo(legacySaEnrolment(validUtr)), eqTo(EmptyRetrieval))(any(), any())
        )
          .thenReturn(Future.failed(InsufficientEnrolments()))

        when(selfAssessmentService.getMtdIdFromUtr(eqTo(validUtr))(any()))
          .thenReturn(Future.failed(Service_Currently_Unavailable))
        running(app) {
          val controller =
            new AuthenticateRequestController(cc, selfAssessmentService, authConnector)(
              ExecutionContext.global
            )
          val result = methodNeedingAuthentication(validUtr, controller)(FakeRequest())

          status(result) mustBe SERVICE_UNAVAILABLE
          contentAsJson(result) mustBe ApiErrorResponses(
            serviceUnavailableMessage
          ).asJson
        }
      }
    }

    "return internal server error if call to fetch mtd id fails due to bad data quality" in {
      when(
        authConnector.authorise(
          any(),
          eqTo(Retrievals.affinityGroup and Retrievals.confidenceLevel)
        )(any(), any())
      )
        .thenReturn(Future.successful(new ~(Some(Individual), minimumConfidence)))

      when(
        authConnector
          .authorise(eqTo(legacySaEnrolment(validUtr)), eqTo(EmptyRetrieval))(any(), any())
      )
        .thenReturn(Future.failed(InsufficientEnrolments()))

      when(selfAssessmentService.getMtdIdFromUtr(eqTo(validUtr))(any()))
        .thenReturn(Future.failed(Downstream_Error))
      running(app) {
        val controller =
          new AuthenticateRequestController(cc, selfAssessmentService, authConnector)(
            ExecutionContext.global
          )
        val result = methodNeedingAuthentication(validUtr, controller)(FakeRequest())

        status(result) mustBe INTERNAL_SERVER_ERROR
        contentAsJson(result) mustBe ApiErrorResponses(
          internalErrorMEssage
        ).asJson
      }
    }
  }

  "Organisation Affinity" should {
    "return ok for a valid mtd enrolment" in {
      when(
        authConnector.authorise(
          any(),
          eqTo(Retrievals.affinityGroup and Retrievals.confidenceLevel)
        )(any(), any())
      )
        .thenReturn(Future.successful(new ~(Some(Organisation), minimumConfidence)))

      when(
        authConnector
          .authorise(eqTo(legacySaEnrolment(validUtr)), eqTo(EmptyRetrieval))(any(), any())
      )
        .thenReturn(Future.failed(InsufficientEnrolments()))

      when(selfAssessmentService.getMtdIdFromUtr(eqTo(validUtr))(any()))
        .thenReturn(Future.successful("mtdId"))

      when(
        authConnector.authorise(eqTo(mtdSaEnrolment("mtdId")), eqTo(EmptyRetrieval))(any(), any())
      )
        .thenReturn(Future.successful(()))
      running(app) {
        val controller =
          new AuthenticateRequestController(cc, selfAssessmentService, authConnector)(
            ExecutionContext.global
          )
        val result = methodNeedingAuthentication(validUtr, controller)(FakeRequest())

        status(result) mustBe OK
      }
    }
    "return success for a valid legacy SA enrolement" in {
      when(
        authConnector.authorise(
          any(),
          eqTo(Retrievals.affinityGroup and Retrievals.confidenceLevel)
        )(any(), any())
      )
        .thenReturn(Future.successful(new ~(Some(Organisation), minimumConfidence)))

      when(
        authConnector
          .authorise(eqTo(legacySaEnrolment(validUtr)), eqTo(EmptyRetrieval))(any(), any())
      )
        .thenReturn(Future.successful(()))

      running(app) {
        val controller =
          new AuthenticateRequestController(cc, selfAssessmentService, authConnector)(
            ExecutionContext.global
          )
        val result = methodNeedingAuthentication(validUtr, controller)(FakeRequest())

        status(result) mustBe OK
      }
    }
    "return unauthorised if they do not have any of the accepted enrolments" in {
      when(
        authConnector.authorise(
          any(),
          eqTo(Retrievals.affinityGroup and Retrievals.confidenceLevel)
        )(any(), any())
      )
        .thenReturn(Future.successful(new ~(Some(Organisation), minimumConfidence)))

      when(
        authConnector
          .authorise(eqTo(legacySaEnrolment(validUtr)), eqTo(EmptyRetrieval))(any(), any())
      )
        .thenReturn(Future.failed(InsufficientEnrolments()))

      when(selfAssessmentService.getMtdIdFromUtr(eqTo(validUtr))(any()))
        .thenReturn(Future.successful("mtdId"))
      when(
        authConnector.authorise(eqTo(mtdSaEnrolment("mtdId")), eqTo(EmptyRetrieval))(any(), any())
      )
        .thenReturn(Future.failed(InsufficientEnrolments()))

      running(app) {
        val controller =
          new AuthenticateRequestController(cc, selfAssessmentService, authConnector)(
            ExecutionContext.global
          )
        val result = methodNeedingAuthentication(validUtr, controller)(FakeRequest())

        status(result) mustBe FORBIDDEN
        contentAsJson(result) mustBe ApiErrorResponses(
          forbiddenMessage
        ).asJson
      }
    }
    "return service unavailble error if call to fetch mtd id fails due to services being down" in {
      when(
        authConnector.authorise(
          any(),
          eqTo(Retrievals.affinityGroup and Retrievals.confidenceLevel)
        )(any(), any())
      )
        .thenReturn(Future.successful(new ~(Some(Individual), minimumConfidence)))

      when(
        authConnector
          .authorise(eqTo(legacySaEnrolment(validUtr)), eqTo(EmptyRetrieval))(any(), any())
      )
        .thenReturn(Future.failed(InsufficientEnrolments()))

      when(selfAssessmentService.getMtdIdFromUtr(eqTo(validUtr))(any()))
        .thenReturn(Future.failed(Service_Currently_Unavailable))
      running(app) {
        val controller =
          new AuthenticateRequestController(cc, selfAssessmentService, authConnector)(
            ExecutionContext.global
          )
        val result = methodNeedingAuthentication(validUtr, controller)(FakeRequest())

        status(result) mustBe SERVICE_UNAVAILABLE
        contentAsJson(result) mustBe ApiErrorResponses(
          serviceUnavailableMessage
        ).asJson
      }
    }
    "return service unavailable error if call to fetch mtd id fails due to bad data quality" in {
      when(
        authConnector.authorise(
          any(),
          eqTo(Retrievals.affinityGroup and Retrievals.confidenceLevel)
        )(any(), any())
      )
        .thenReturn(Future.successful(new ~(Some(Organisation), minimumConfidence)))

      when(
        authConnector
          .authorise(eqTo(legacySaEnrolment(validUtr)), eqTo(EmptyRetrieval))(any(), any())
      )
        .thenReturn(Future.failed(InsufficientEnrolments()))

      when(selfAssessmentService.getMtdIdFromUtr(eqTo(validUtr))(any()))
        .thenReturn(Future.failed(Downstream_Error))
      running(app) {
        val controller =
          new AuthenticateRequestController(cc, selfAssessmentService, authConnector)(
            ExecutionContext.global
          )
        val result = methodNeedingAuthentication(validUtr, controller)(FakeRequest())

        status(result) mustBe INTERNAL_SERVER_ERROR
        contentAsJson(result) mustBe ApiErrorResponses(
          internalErrorMEssage
        ).asJson
      }
    }
  }

  "Agent Affinity" should {

    "return ok for an agent allowed for self assessment tax and a delagated self assessment enrolment" in {
      when(
        authConnector.authorise(
          any(),
          eqTo(Retrievals.affinityGroup and Retrievals.confidenceLevel)
        )(any(), any())
      )
        .thenReturn(Future.successful(new ~(Some(Agent), lowConfidence)))
      when(
        authConnector.authorise(eqTo(principleAgentEnrolments), eqTo(EmptyRetrieval))(
          any(),
          any()
        )
      )
        .thenReturn(Future.successful(()))
      when(selfAssessmentService.getMtdIdFromUtr(any())(any()))
        .thenReturn(Future.successful("mtdId"))
      when(
        authConnector.authorise(
          eqTo(delegatedEnrolments(validUtr, "mtdId")),
          eqTo(EmptyRetrieval)
        )(any(), any())
      )
        .thenReturn(Future.successful(()))

      running(app) {
        val controller =
          new AuthenticateRequestController(cc, selfAssessmentService, authConnector)(
            ExecutionContext.global
          )
        val result = methodNeedingAuthentication(validUtr, controller)(FakeRequest())
        status(result) mustBe OK
      }
    }

    "return FORBIDDEN if agent/client relationship is not established via the utr provided" in {
      when(
        authConnector.authorise(
          any(),
          eqTo(Retrievals.affinityGroup and Retrievals.confidenceLevel)
        )(any(), any())
      )
        .thenReturn(Future.successful(new ~(Some(Agent), lowConfidence)))
      when(
        authConnector.authorise(eqTo(principleAgentEnrolments), eqTo(EmptyRetrieval))(
          any(),
          any()
        )
      )
        .thenReturn(Future.successful(()))
      when(selfAssessmentService.getMtdIdFromUtr(any())(any()))
        .thenReturn(Future.successful("mtdId"))
      when(
        authConnector.authorise(
          eqTo(delegatedEnrolments(validUtr, "mtdId")),
          eqTo(EmptyRetrieval)
        )(any(), any())
      )
        .thenReturn(Future.failed(InsufficientEnrolments()))

      running(app) {
        val controller =
          new AuthenticateRequestController(cc, selfAssessmentService, authConnector)(
            ExecutionContext.global
          )
        val result = methodNeedingAuthentication(validUtr, controller)(FakeRequest())
        status(result) mustBe FORBIDDEN
        contentAsJson(result) mustBe ApiErrorResponses(
          forbiddenMessage
        ).asJson
      }
    }

    "return service unavailable error if call to fetch mtd fails due to services being down" in {
      when(
        authConnector.authorise(
          any(),
          eqTo(Retrievals.affinityGroup and Retrievals.confidenceLevel)
        )(any(), any())
      )
        .thenReturn(Future.successful(new ~(Some(Agent), lowConfidence)))
      when(
        authConnector.authorise(eqTo(principleAgentEnrolments), eqTo(EmptyRetrieval))(
          any(),
          any()
        )
      )
        .thenReturn(Future.successful(()))
      when(selfAssessmentService.getMtdIdFromUtr(any())(any()))
        .thenReturn(Future.failed(Service_Currently_Unavailable))

      running(app) {
        val controller =
          new AuthenticateRequestController(cc, selfAssessmentService, authConnector)(
            ExecutionContext.global
          )
        val result = methodNeedingAuthentication(validUtr, controller)(FakeRequest())
        status(result) mustBe SERVICE_UNAVAILABLE
        contentAsJson(result) mustBe ApiErrorResponses(
          serviceUnavailableMessage
        ).asJson
      }
    }

    "return internal error if call to fetch mtd fails due to data quality errors" in {
      when(
        authConnector.authorise(
          any(),
          eqTo(Retrievals.affinityGroup and Retrievals.confidenceLevel)
        )(any(), any())
      )
        .thenReturn(Future.successful(new ~(Some(Agent), lowConfidence)))
      when(
        authConnector.authorise(eqTo(principleAgentEnrolments), eqTo(EmptyRetrieval))(
          any(),
          any()
        )
      )
        .thenReturn(Future.successful(()))
      when(selfAssessmentService.getMtdIdFromUtr(any())(any()))
        .thenReturn(Future.failed(Downstream_Error))

      running(app) {
        val controller =
          new AuthenticateRequestController(cc, selfAssessmentService, authConnector)(
            ExecutionContext.global
          )
        val result = methodNeedingAuthentication(validUtr, controller)(FakeRequest())
        status(result) mustBe INTERNAL_SERVER_ERROR
        contentAsJson(result) mustBe ApiErrorResponses(
          internalErrorMEssage
        ).asJson
      }
    }

  }

}
