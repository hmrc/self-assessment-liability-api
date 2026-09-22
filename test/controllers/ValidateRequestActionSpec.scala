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

import controllers.actions.ValidateRequestAction
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsJson, defaultAwaitTimeout, status}
import shared.SpecBase
import utils.UkTaxYears.getPastTwoUkTaxYears
import java.time.{LocalDate, Month}
import scala.concurrent.Future

class ValidateRequestActionSpec extends SpecBase {

  val validateAction = new ValidateRequestAction()
  val cc: ControllerComponents = app.injector.instanceOf[ControllerComponents]

  def testMethod(utr: String): Action[AnyContent] = {
    (cc.actionBuilder andThen validateAction(utr)).async { request =>
      Future.successful(
        Ok(
          Json.obj(
            "utr" -> request.utr,
            "startDate" -> request.requestPeriod.startDate.toString,
            "endDate" -> request.requestPeriod.endDate.toString
          )
        )
      )
    }
  }

  "ValidateRequestAction" should {

    "Be successful for a valid utr with no date provided" in {
      val validUtr = "1234567890"
      val result = testMethod(validUtr)(FakeRequest("GET", s"/test"))

      status(result) mustBe OK
      val json = contentAsJson(result)
      (json \ "utr").as[String] mustBe validUtr

      val startDate = LocalDate.parse((json \ "startDate").as[String])
      val endDate = LocalDate.parse((json \ "endDate").as[String])

      val expectedDates = getPastTwoUkTaxYears()
      startDate mustBe expectedDates._1
      endDate mustBe expectedDates._2
    }

    "Be successful for a valid query date and utr" in {
      val validUtr = "1234567890"
      val validDate: String = LocalDate.now().minusYears(2).toString
      val result = testMethod(validUtr)(FakeRequest("GET", s"/test?fromDate=$validDate"))

      status(result) mustBe OK
      val json = contentAsJson(result)
      (json \ "utr").as[String] mustBe validUtr
      (json \ "startDate").as[String] mustBe validDate

      val endDate = LocalDate.parse((json \ "endDate").as[String])
      val expectedEndDate = getPastTwoUkTaxYears()._2
      endDate mustBe expectedEndDate
    }

    "return BadRequest for an invalid utr" in {
      val invalidUtr = "invalid"
      val result = testMethod(invalidUtr)(FakeRequest())

      status(result) mustBe BAD_REQUEST
      contentAsJson(result) mustBe Json.obj(
        "message" -> "Invalid request format or parameters."
      )
    }

    "return BadRequest for a date with incorrect type" in {
      val validUtr = "1234567890"
      val badDate = "invalid-date"
      val result = testMethod(validUtr)(FakeRequest("GET", s"/test?fromDate=$badDate"))

      status(result) mustBe BAD_REQUEST
      contentAsJson(result) mustBe Json.obj(
        "message" -> "Invalid request format or parameters."
      )
    }

    "return BadRequest for a date in the future" in {
      val validUtr = "1234567890"
      val badDate = LocalDate.now().plusDays(1)
      val result = testMethod(validUtr)(FakeRequest("GET", s"/test?fromDate=$badDate"))

      status(result) mustBe BAD_REQUEST
      contentAsJson(result) mustBe Json.obj(
        "message" -> "Invalid request format or parameters."
      )
    }

    "return BadRequest for a date more than 7 tax years ago" in {
      val validUtr = "1234567890"
      val currentDate: LocalDate = LocalDate.now()
      val badDate = LocalDate.of(currentDate.getYear - 7, Month.APRIL, 6).minusDays(1)
      val result = testMethod(validUtr)(FakeRequest("GET", s"/test?fromDate=$badDate"))

      status(result) mustBe BAD_REQUEST
      contentAsJson(result) mustBe Json.obj(
        "message" -> "Invalid request format or parameters."
      )
    }

    "validate specific date ranges are correct" in {
      val validUtr = "1234567890"
      val specificDate = "2024-06-15"
      val result = testMethod(validUtr)(FakeRequest("GET", s"/test?fromDate=$specificDate"))

      status(result) mustBe OK
      val json = contentAsJson(result)

      (json \ "utr").as[String] mustBe validUtr
      (json \ "startDate").as[String] mustBe specificDate

      val endDate = LocalDate.parse((json \ "endDate").as[String])
      endDate mustBe LocalDate.now()
    }
  }
}
