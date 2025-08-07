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
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.inject
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.mvc.*
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import shared.SpecBase

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

  val fakeRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(
    GET,
    routes.SelfAssessmentHistoryController.getYourSelfAssessmentData("1234567890", None).url
  )

  "SelfAssessmentHistoryController" should {
    "return OK with success message" in {

      val application = new GuiceApplicationBuilder()
        .overrides(
          inject.bind[AuthenticateRequestAction].toInstance(createBypassAuthAction()(bodyParser))
        )
        .build()

      running(application) {
        val controller = application.injector.instanceOf[SelfAssessmentHistoryController]
        val result = controller.getYourSelfAssessmentData("1234567890", None)(fakeRequest)

        status(result) mustBe OK
        contentAsJson(result) mustBe Json.obj("message" -> "Success!")
      }
    }
  }
}
