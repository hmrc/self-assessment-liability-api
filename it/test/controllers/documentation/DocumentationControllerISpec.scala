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

package controllers.documentation

import org.scalatest.time.SpanSugar.convertIntToGrainOfTime
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.play.bootstrap.http.HttpClientV2Provider
import utils.IntegrationSpecBase

import java.net.URI
import scala.concurrent.Await

class DocumentationControllerISpec extends IntegrationSpecBase {

  lazy val provider: HttpClientV2Provider = app.injector.instanceOf[HttpClientV2Provider]
  lazy val client: HttpClientV2 = provider.get()


  val baseUrl = s"http://localhost:$port/api/definition"

  "Integration Tests for SA Documentation Controller" must {
    "Connection" should {
      "GET /status return OK with correct field values" in {
        val result =
          Await.result(
            client
              .get(URI.create(baseUrl).toURL)
              .execute[HttpResponse],
            5.seconds
          )
        val descriptionText =
          "This API provides a breakdown of Self Assessment liability, showing overdue, payable and pending amounts, within HMRC tax account services."
        result.status mustEqual OK
        val json = result.json
        (json \ "api" \ "versions" \ 0 \ "status").as[String] mustEqual "ALPHA"
        (json \ "api" \ "versions" \ 0 \ "endpointsEnabled").as[Boolean] mustEqual false
        (json \ "api" \ "categories" \ 0).as[String] mustEqual "SELF_ASSESSMENT"
        (json \ "api" \ "context").as[String] mustEqual "individuals/self-assessment/breakdown"
        (json \ "api" \ "description").as[String] mustEqual descriptionText
        (json \ "api" \ "name").as[String] mustEqual "View Self Assessment Account"
      }
    }
  }
}
