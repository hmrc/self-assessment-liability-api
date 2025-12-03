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
import controllers.actions.AuthenticateRequestAction
import models.ServiceErrors.{
  Downstream_Error,
  Service_Currently_Unavailable_Error,
  Unauthorised_Error
}
import models.{RequestPeriod, RequestWithUtr}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.*
import org.scalatest.matchers.should.Matchers.should
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.mvc.{AnyContent, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.SelfAssessmentService
import shared.{HttpWireMock, SpecBase}
import uk.gov.hmrc.auth.core.*
import uk.gov.hmrc.auth.core.AffinityGroup.{Agent, Individual, Organisation}
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.retrieve.{EmptyRetrieval, ~}
import utils.SelfAssessmentEnrolments.*

import java.time.LocalDate
import scala.concurrent.Future

class AuthenticateRequestActionSpec extends SpecBase with HttpWireMock {

  private val authConnector: AuthConnector = mock[AuthConnector]
  private val selfAssessmentService: SelfAssessmentService = mock[SelfAssessmentService]
  private val appConfig = mock[AppConfig]
  private val minimumConfidence = ConfidenceLevel.L250
  private val lowConfidence = ConfidenceLevel.L50

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(authConnector, selfAssessmentService, appConfig)
    when(appConfig.confidenceLevel).thenReturn(minimumConfidence)
  }

  val now: LocalDate = LocalDate.now()
  val fakeRequest: FakeRequest[AnyContent] = FakeRequest("GET", "/utr")
  val requestWithUtr: RequestWithUtr[AnyContent] =
    RequestWithUtr("utr", RequestPeriod(now, now), fakeRequest)
  class Harness(service: SelfAssessmentService)
      extends AuthenticateRequestAction(service, authConnector)(ec, appConfig) {
    def callFilter[A](request: RequestWithUtr[A]): Future[Option[Result]] = filter(requestWithUtr)
  }

  "AuthenticateRequestAction" when {
    "no auth token" should {
      "return BadRequest" in {
        val sessionFailed = new NoActiveSession("No active session") {}
        when(authConnector.authorise(any(), any())(any(), any()))
          .thenReturn(Future.failed(sessionFailed))

        val result = new Harness(selfAssessmentService).callFilter(requestWithUtr)
        result.failed.futureValue mustBe sessionFailed

      }
    }

    "auth down" should {
      "return service unavailable if auth is down" in {
        val error = new RuntimeException("Auth service down")
        when(
          authConnector.authorise(
            any(),
            eqTo(Retrievals.affinityGroup and Retrievals.confidenceLevel)
          )(any(), any())
        )
          .thenReturn(Future.failed(error))

        val result = new Harness(selfAssessmentService).callFilter(requestWithUtr)
        result.failed.futureValue mustBe error
      }
    }

  }

  "Individual Affinity" should {

    "return success if confidence level is 250 for legacy SA enrolment" in {
      when(
        authConnector.authorise(
          any(),
          eqTo(Retrievals.affinityGroup and Retrievals.confidenceLevel)
        )(any(), any())
      )
        .thenReturn(Future.successful(new ~(Some(Individual), minimumConfidence)))

      when(
        authConnector
          .authorise(eqTo(legacySaEnrolment("utr")), eqTo(EmptyRetrieval))(any(), any())
      )
        .thenReturn(Future.successful(()))

      val result = await(new Harness(selfAssessmentService).callFilter(requestWithUtr))

      result mustBe None
    }
  }

  "return Unauthorized if confidence level is below 250" in {
    when(
      authConnector.authorise(
        any(),
        eqTo(Retrievals.affinityGroup and Retrievals.confidenceLevel)
      )(any(), any())
    )
      .thenReturn(Future.successful(new ~(Some(Individual), lowConfidence)))

    val result = new Harness(selfAssessmentService).callFilter(requestWithUtr)

    result.failed.futureValue mustBe Unauthorised_Error
  }

  "return ok if they meet the minimum confidence threshold with an mtd enrolment" in {
    when(
      authConnector.authorise(
        any(),
        eqTo(Retrievals.affinityGroup and Retrievals.confidenceLevel)
      )(any(), any())
    )
      .thenReturn(Future.successful(new ~(Some(Individual), minimumConfidence)))

    when(
      authConnector
        .authorise(eqTo(legacySaEnrolment("utr")), eqTo(EmptyRetrieval))(any(), any())
    )
      .thenReturn(Future.failed(InsufficientEnrolments()))

    when(selfAssessmentService.getMtdIdFromUtr(eqTo("utr"))(any()))
      .thenReturn(Future.successful("mtdId"))

    when(
      authConnector.authorise(eqTo(mtdSaEnrolment("mtdId")), eqTo(EmptyRetrieval))(any(), any())
    )
      .thenReturn(Future.successful(()))

    val result = await(new Harness(selfAssessmentService).callFilter(requestWithUtr))

    result mustBe None
  }

  "return AuthorisationException if they do not have any of the accepted enrolments" in {
    when(
      authConnector.authorise(
        any(),
        eqTo(Retrievals.affinityGroup and Retrievals.confidenceLevel)
      )(any(), any())
    )
      .thenReturn(Future.successful(new ~(Some(Individual), minimumConfidence)))

    when(
      authConnector
        .authorise(eqTo(legacySaEnrolment("utr")), eqTo(EmptyRetrieval))(any(), any())
    ).thenReturn(Future.failed(InsufficientEnrolments()))

    when(selfAssessmentService.getMtdIdFromUtr(eqTo("utr"))(any()))
      .thenReturn(Future.successful("mtdId"))

    when(
      authConnector.authorise(eqTo(mtdSaEnrolment("mtdId")), eqTo(EmptyRetrieval))(any(), any())
    )
      .thenReturn(Future.failed(InsufficientEnrolments()))

    val result = new Harness(selfAssessmentService).callFilter(requestWithUtr)

    result.failed.futureValue mustBe a[AuthorisationException]
  }

  "return the failed future if call to fetch mtd id fails due to service being unavailable" in {
    when(
      authConnector.authorise(
        any(),
        eqTo(Retrievals.affinityGroup and Retrievals.confidenceLevel)
      )(any(), any())
    )
      .thenReturn(Future.successful(new ~(Some(Individual), minimumConfidence)))

    when(
      authConnector
        .authorise(eqTo(legacySaEnrolment("utr")), eqTo(EmptyRetrieval))(any(), any())
    )
      .thenReturn(Future.failed(InsufficientEnrolments()))

    when(selfAssessmentService.getMtdIdFromUtr(eqTo("utr"))(any()))
      .thenReturn(Future.failed(Service_Currently_Unavailable_Error))

    val result = new Harness(selfAssessmentService).callFilter(requestWithUtr)

    result.failed.futureValue mustBe Service_Currently_Unavailable_Error
  }

  "return Downstream_Error if call to fetch mtd id fails" in {
    when(
      authConnector.authorise(
        any(),
        eqTo(Retrievals.affinityGroup and Retrievals.confidenceLevel)
      )(any(), any())
    )
      .thenReturn(Future.successful(new ~(Some(Individual), minimumConfidence)))

    when(
      authConnector
        .authorise(eqTo(legacySaEnrolment("utr")), eqTo(EmptyRetrieval))(any(), any())
    )
      .thenReturn(Future.failed(InsufficientEnrolments()))

    when(selfAssessmentService.getMtdIdFromUtr(eqTo("utr"))(any()))
      .thenReturn(Future.failed(Downstream_Error))

    val result = new Harness(selfAssessmentService).callFilter(requestWithUtr)
    result.failed.futureValue mustBe Downstream_Error

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
          .authorise(eqTo(legacySaEnrolment("utr")), eqTo(EmptyRetrieval))(any(), any())
      )
        .thenReturn(Future.failed(InsufficientEnrolments()))

      when(selfAssessmentService.getMtdIdFromUtr(eqTo("utr"))(any()))
        .thenReturn(Future.successful("mtdId"))

      when(
        authConnector.authorise(eqTo(mtdSaEnrolment("mtdId")), eqTo(EmptyRetrieval))(any(), any())
      )
        .thenReturn(Future.successful(()))
      val result = new Harness(selfAssessmentService).callFilter(requestWithUtr)
      result.futureValue mustBe None
    }

    "return success for a valid legacy SA enrolment" in {
      when(
        authConnector.authorise(
          any(),
          eqTo(Retrievals.affinityGroup and Retrievals.confidenceLevel)
        )(any(), any())
      )
        .thenReturn(Future.successful(new ~(Some(Organisation), minimumConfidence)))

      when(
        authConnector
          .authorise(eqTo(legacySaEnrolment("utr")), eqTo(EmptyRetrieval))(any(), any())
      )
        .thenReturn(Future.successful(()))
      val result = new Harness(selfAssessmentService).callFilter(requestWithUtr)
      result.futureValue mustBe None
    }

    "return forbidden if they do not have any of the accepted enrolments" in {
      when(
        authConnector.authorise(
          any(),
          eqTo(Retrievals.affinityGroup and Retrievals.confidenceLevel)
        )(any(), any())
      )
        .thenReturn(Future.successful(new ~(Some(Organisation), minimumConfidence)))

      when(
        authConnector
          .authorise(eqTo(legacySaEnrolment("utr")), eqTo(EmptyRetrieval))(any(), any())
      )
        .thenReturn(Future.failed(InsufficientEnrolments()))

      when(selfAssessmentService.getMtdIdFromUtr(eqTo("utr"))(any()))
        .thenReturn(Future.successful("mtdId"))
      when(
        authConnector.authorise(eqTo(mtdSaEnrolment("mtdId")), eqTo(EmptyRetrieval))(any(), any())
      )
        .thenReturn(Future.failed(InsufficientEnrolments()))

      val result = new Harness(selfAssessmentService).callFilter(requestWithUtr)
      result.failed.futureValue mustBe a[AuthorisationException]
    }

    "return error if call to fetch mtd id fails with Service_Currently_Unavailable_Error" in {
      when(
        authConnector.authorise(
          any(),
          eqTo(Retrievals.affinityGroup and Retrievals.confidenceLevel)
        )(any(), any())
      )
        .thenReturn(Future.successful(new ~(Some(Organisation), minimumConfidence)))

      when(
        authConnector
          .authorise(eqTo(legacySaEnrolment("utr")), eqTo(EmptyRetrieval))(any(), any())
      )
        .thenReturn(Future.failed(InsufficientEnrolments()))

      when(selfAssessmentService.getMtdIdFromUtr(eqTo("utr"))(any()))
        .thenReturn(Future.failed(Service_Currently_Unavailable_Error))
      val result = new Harness(selfAssessmentService).callFilter(requestWithUtr)
      result.failed.futureValue mustBe Service_Currently_Unavailable_Error
    }

    "return error if call to fetch mtd id returns Downstream_Error" in {
      when(
        authConnector.authorise(
          any(),
          eqTo(Retrievals.affinityGroup and Retrievals.confidenceLevel)
        )(any(), any())
      )
        .thenReturn(Future.successful(new ~(Some(Organisation), minimumConfidence)))

      when(
        authConnector
          .authorise(eqTo(legacySaEnrolment("utr")), eqTo(EmptyRetrieval))(any(), any())
      )
        .thenReturn(Future.failed(InsufficientEnrolments()))

      when(selfAssessmentService.getMtdIdFromUtr(eqTo("utr"))(any()))
        .thenReturn(Future.failed(Downstream_Error))
      val result = new Harness(selfAssessmentService).callFilter(requestWithUtr)
      result.failed.futureValue mustBe Downstream_Error
    }
  }

  "Agent Affinity" should {

    "return ok for an agent with delegated legacy SA enrolment" in {
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

      when(
        authConnector.authorise(
          eqTo(delegatedLegacySaEnrolment("utr")),
          eqTo(EmptyRetrieval)
        )(any(), any())
      )
        .thenReturn(Future.successful(()))
      val result = new Harness(selfAssessmentService).callFilter(requestWithUtr)
      result.futureValue mustBe None
    }

    "return ok for an agent with delegated MTD enrolment" in {
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

      when(
        authConnector.authorise(
          eqTo(delegatedLegacySaEnrolment("utr")),
          eqTo(EmptyRetrieval)
        )(any(), any())
      )
        .thenReturn(Future.failed(InsufficientEnrolments()))

      when(selfAssessmentService.getMtdIdFromUtr(eqTo("utr"))(any()))
        .thenReturn(Future.successful("mtdId"))

      when(
        authConnector.authorise(
          eqTo(delegatedMtdEnrolment("mtdId")),
          eqTo(EmptyRetrieval)
        )(any(), any())
      )
        .thenReturn(Future.successful(()))
      val result = new Harness(selfAssessmentService).callFilter(requestWithUtr)
      result.futureValue mustBe None
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

      when(
        authConnector.authorise(
          eqTo(delegatedLegacySaEnrolment("utr")),
          eqTo(EmptyRetrieval)
        )(any(), any())
      )
        .thenReturn(Future.failed(InsufficientEnrolments()))

      when(selfAssessmentService.getMtdIdFromUtr(eqTo("utr"))(any()))
        .thenReturn(Future.successful("mtdId"))

      when(
        authConnector.authorise(
          eqTo(delegatedMtdEnrolment("mtdId")),
          eqTo(EmptyRetrieval)
        )(any(), any())
      )
        .thenReturn(Future.failed(InsufficientEnrolments()))
      val result = new Harness(selfAssessmentService).callFilter(requestWithUtr)
      result.failed.futureValue mustBe Unauthorised_Error
    }

    "return Service_Currently_Unavailable_Error if call to fetch mtd fails due to services being down" in {
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

      when(
        authConnector.authorise(
          eqTo(delegatedLegacySaEnrolment("utr")),
          eqTo(EmptyRetrieval)
        )(any(), any())
      )
        .thenReturn(Future.failed(InsufficientEnrolments()))

      when(selfAssessmentService.getMtdIdFromUtr(eqTo("utr"))(any()))
        .thenReturn(Future.failed(Service_Currently_Unavailable_Error))
      val result = new Harness(selfAssessmentService).callFilter(requestWithUtr)
      result.failed.futureValue mustBe Service_Currently_Unavailable_Error
    }

    "return the error if call to fetch mtd fails with Downstream_Error" in {
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

      when(
        authConnector.authorise(
          eqTo(delegatedLegacySaEnrolment("utr")),
          eqTo(EmptyRetrieval)
        )(any(), any())
      )
        .thenReturn(Future.failed(InsufficientEnrolments()))

      when(selfAssessmentService.getMtdIdFromUtr(eqTo("utr"))(any()))
        .thenReturn(Future.failed(Downstream_Error))
      val result = new Harness(selfAssessmentService).callFilter(requestWithUtr)
      result.failed.futureValue mustBe Downstream_Error
    }
  }
}
