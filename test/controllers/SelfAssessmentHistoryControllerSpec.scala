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

import config.AppConfig
import controllers.actions.{AuthenticateRequestAction, ValidateRequestAction}
import models.{HipResponse, RequestWithUtr}
import models.ServiceErrors.{
  Downstream_Error,
  Invalid_Start_Date_Error,
  Json_Validation_Error,
  No_Data_Found_Error
}
import org.mockito.ArgumentMatchers.{any, eq as meq}
import org.mockito.Mockito.when
import org.scalatest.matchers.should.Matchers.shouldBe
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks.forAll
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.*
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.SelfAssessmentService
import shared.{HipResponseGenerator, SpecBase}
import uk.gov.hmrc.auth.core.AuthConnector

import java.time.{LocalDate, Month}
import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps

class SelfAssessmentHistoryControllerSpec extends SpecBase {

  val mockService: SelfAssessmentService = mock[SelfAssessmentService]
  val authConnector: AuthConnector = mock[AuthConnector]
  val cc: ControllerComponents = app.injector.instanceOf[ControllerComponents]
  val validDate: LocalDate = LocalDate.now()
  val bodyParser: BodyParsers.Default = app.injector.instanceOf[BodyParsers.Default]
  implicit val appConfig: AppConfig = mock[AppConfig]
  val fakeValidateAction: ValidateRequestAction = new ValidateRequestAction()
  val fakeAuthenticaAction: AuthenticateRequestAction =
    new AuthenticateRequestAction(mockService, authConnector) {
      override def filter[A](request: RequestWithUtr[A]): Future[Option[Result]] = {
        Future.successful(None)
      }

      override protected def executionContext: ExecutionContext = ec
    }

  val controller: SelfAssessmentHistoryController =
    new SelfAssessmentHistoryController(fakeAuthenticaAction, fakeValidateAction, cc, mockService)
  def request(utr: String, fromDate: LocalDate): Future[Result] = {
    controller.getYourSelfAssessmentData("1234567890", Some(validDate.toString))(FakeRequest())
  }

  "SelfAssessmentHistoryController" should {
    "return OK with success message" in {
      forAll(HipResponseGenerator.hipResponseGen) { hipResponse =>
        when(
          mockService.viewAccountService(
            any(),
            meq(LocalDate.of(validDate.getYear - 1, Month.APRIL, 6)),
            meq(validDate)
          )(any())
        )
          .thenReturn(Future.successful(hipResponse))
        val result = request("1234567890", validDate)
        status(result) mustBe OK
        contentAsJson(result) mustBe Json.toJson(hipResponse)
      }
    }
    "successfully parse a payload with balance details only" in {
      val minimalHipResponseJson: JsValue = Json.obj(
        "balanceDetails" -> Json.obj(
          "totalOverdueBalance" -> 0,
          "totalPayableBalance" -> 0,
          "totalPendingBalance" -> 0,
          "totalBalance" -> 0,
          "totalCreditAvailable" -> 0
        )
      )
      val hipResponse = minimalHipResponseJson.as[HipResponse]
      when(
        mockService.viewAccountService(
          any(),
          meq(LocalDate.of(validDate.getYear - 1, Month.APRIL, 6)),
          meq(validDate)
        )(any())
      )
        .thenReturn(Future.successful(hipResponse))
      val result = request("1234567890", validDate)
      status(result) mustBe OK

    }
  }
  "return bad request if a date with bad format is provided" in {
    when(mockService.viewAccountService(meq("1234567890"), any(), any())(any()))
      .thenReturn(Future.failed(Invalid_Start_Date_Error))
    val result = request("1234567890", validDate)

    result.failed.futureValue mustEqual Invalid_Start_Date_Error
  }

  "return Internal server error json validation on HIP response fails" in {
    when(mockService.viewAccountService(meq("1234567890"), any(), any())(any()))
      .thenReturn(Future.failed(Json_Validation_Error))
    val result = request("1234567890", validDate)
    result.failed.futureValue mustEqual Json_Validation_Error

  }

  "return not found if no data is found in HIP for the utr provided" in {
    when(mockService.viewAccountService(meq("1234567890"), any(), any())(any()))
      .thenReturn(Future.failed(No_Data_Found_Error))

    val result = request("1234567890", validDate)
    result.failed.futureValue mustEqual No_Data_Found_Error

  }
  "return service unavailable if call to HIP fails" in {
    when(mockService.viewAccountService(meq("1234567890"), any(), any())(any()))
      .thenReturn(Future.failed(Downstream_Error))
    val result = request("1234567890", validDate)

    result.failed.futureValue mustEqual Downstream_Error

  }
}
