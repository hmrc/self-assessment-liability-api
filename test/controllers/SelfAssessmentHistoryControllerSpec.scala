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
import models.RequestData
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.Json
import play.api.mvc.{AnyContent, AnyContentAsEmpty, ControllerComponents, Result}
import play.api.test.Helpers.*
import play.api.test.{FakeRequest, Helpers}
import services.SelfAssessmentService
import uk.gov.hmrc.auth.core.AuthConnector

import scala.concurrent.{ExecutionContext, Future}

class SelfAssessmentHistoryControllerSpec
    extends AnyWordSpec
    with Matchers
    with MockitoSugar
    with ScalaFutures {

  implicit val ec: ExecutionContext = ExecutionContext.global

  val mockAuthConnector: AuthConnector = mock[AuthConnector]
  val mockService: SelfAssessmentService = mock[SelfAssessmentService]
  val mockCC: ControllerComponents = Helpers.stubControllerComponents()
  implicit val mockAppConfig: AppConfig = mock[AppConfig]

  val controller = new SelfAssessmentHistoryController(
    mockAuthConnector,
    mockService,
    mockCC
  )

  val testUtr = "1234567890"
  val fakeRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  "SelfAssessmentHistoryController" should {
    "return OK with success message" in {
      val testController = new SelfAssessmentHistoryController(
        mockAuthConnector,
        mockService,
        mockCC
      ) {
        override def authorisedAction(
            utr: String
        )(block: RequestData[AnyContent] => Future[Result]) =
          Action.async { request =>
            block(RequestData(utr, None, request))
          }
      }

      val result: Future[Result] = testController.getYourSelfAssessmentData(testUtr)(fakeRequest)

      status(result) mustBe OK
      contentAsJson(result) mustBe Json.obj("message" -> "Success!")
    }
  }
}
