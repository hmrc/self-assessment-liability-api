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

import models.RequestData
import models.ServiceErrors.{
  Downstream_Error,
  Json_Validation_Error,
  No_Data_Found,
  Service_Currently_Unavailable
}
import org.mockito.ArgumentMatchers.{any, eq as meq}
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks.forAll
import play.api.inject
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.mvc.*
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.SelfAssessmentService
import shared.{HipResponseGenerator, SpecBase}

import java.time.format.DateTimeParseException
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}

class SelfAssessmentHistoryControllerSpec extends SpecBase {

  def createBypassAuthAction()(implicit bodyParsers: PlayBodyParsers): AuthenticateRequestAction = {
    val mockAuth = mock[AuthenticateRequestAction]

    when(mockAuth.apply(any[String])).thenAnswer { invocation =>
      val utr = invocation.getArgument[String](0)
      new ActionBuilder[RequestData, AnyContent] {
        override def parser: BodyParser[AnyContent] = bodyParsers.default
        override protected def executionContext: ExecutionContextExecutor = ExecutionContext.global

        override def invokeBlock[A](
            request: play.api.mvc.Request[A],
            block: RequestData[A] => Future[Result]
        ): Future[Result] = {
          val requestData = RequestData(utr, None, request)
          block(requestData)
        }
      }
    }
    mockAuth
  }

  val bodyParser: PlayBodyParsers = app.injector.instanceOf[PlayBodyParsers]
  val fakeRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, "/1234567890")
  val mockService: SelfAssessmentService = mock[SelfAssessmentService]

  "SelfAssessmentHistoryController" should {
    "return OK with success message" in {
      forAll(HipResponseGenerator.hipResponseGen) { hipResponse =>

        when(mockService.viewAccountService(meq("1234567890"), meq(Some("2024-05-01")))(any()))
          .thenReturn(Future.successful(hipResponse))

        val application = new GuiceApplicationBuilder()
          .overrides(
            inject.bind[AuthenticateRequestAction].toInstance(createBypassAuthAction()(bodyParser)),
            inject.bind[SelfAssessmentService].toInstance(mockService)
          )
          .build()

        running(application) {
          val controller = application.injector.instanceOf[SelfAssessmentHistoryController]
          val result =
            controller.getYourSelfAssessmentData("1234567890", Some("2024-05-01"))(fakeRequest)

          status(result) mustBe OK
          contentAsJson(result) mustBe Json.toJson(hipResponse)
        }
      }
    }

    "return bad request if a date with bad format is provided" in {
      when(mockService.viewAccountService(meq("1234567890"), any())(any()))
        .thenReturn(Future.failed(DateTimeParseException("s", "s", 2)))
      val application = new GuiceApplicationBuilder()
        .overrides(
          inject.bind[AuthenticateRequestAction].toInstance(createBypassAuthAction()(bodyParser)),
          inject.bind[SelfAssessmentService].toInstance(mockService)
        )
        .build()

      val controller = application.injector.instanceOf[SelfAssessmentHistoryController]
      val result = controller.getYourSelfAssessmentData("1234567890", None)(fakeRequest)
      status(result) mustBe BAD_REQUEST
      contentAsJson(result) mustBe Json.obj("message" -> "Invalid request format or parameters.")
    }

    "return Internal server error json validation on HIP response fails" in {
      when(mockService.viewAccountService(meq("1234567890"), any())(any()))
        .thenReturn(Future.failed(Json_Validation_Error))

      val application = new GuiceApplicationBuilder()
        .overrides(
          inject.bind[AuthenticateRequestAction].toInstance(createBypassAuthAction()(bodyParser)),
          inject.bind[SelfAssessmentService].toInstance(mockService)
        )
        .build()

      running(application) {
        val controller = application.injector.instanceOf[SelfAssessmentHistoryController]
        val result = controller.getYourSelfAssessmentData("1234567890", None)(fakeRequest)

        status(result) mustBe INTERNAL_SERVER_ERROR
        contentAsJson(result) mustBe Json.obj(
          "message" -> "Unexpected internal error. Please contact service desk."
        )
      }
    }

    "return not found if no data is found in HIP for the utr provided" in {
      when(mockService.viewAccountService(meq("1234567890"), any())(any()))
        .thenReturn(Future.failed(No_Data_Found))

      val application = new GuiceApplicationBuilder()
        .overrides(
          inject.bind[AuthenticateRequestAction].toInstance(createBypassAuthAction()(bodyParser)),
          inject.bind[SelfAssessmentService].toInstance(mockService)
        )
        .build()

      running(application) {
        val controller = application.injector.instanceOf[SelfAssessmentHistoryController]
        val result = controller.getYourSelfAssessmentData("1234567890", None)(fakeRequest)

        status(result) mustBe NOT_FOUND
        contentAsJson(result) mustBe Json.obj(
          "message" -> "The requested resource could not be found."
        )
      }
    }
    "return service unavailable if call to HIP fails" in {
      when(mockService.viewAccountService(meq("1234567890"), any())(any()))
        .thenReturn(Future.failed(Service_Currently_Unavailable))

      val application = new GuiceApplicationBuilder()
        .overrides(
          inject.bind[AuthenticateRequestAction].toInstance(createBypassAuthAction()(bodyParser)),
          inject.bind[SelfAssessmentService].toInstance(mockService)
        )
        .build()

      running(application) {
        val controller = application.injector.instanceOf[SelfAssessmentHistoryController]
        val result = controller.getYourSelfAssessmentData("1234567890", None)(fakeRequest)

        status(result) mustBe SERVICE_UNAVAILABLE
        contentAsJson(result) mustBe Json.obj(
          "message" -> "Service unavailable. Please try again later."
        )
      }
    }
  }
}
