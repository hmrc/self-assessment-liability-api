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

package connectors

import models.ServiceErrors.Downstream_Error
import play.api.Application
import play.api.http.Status.*
import play.api.inject.guice.GuiceApplicationBuilder
import shared.{HttpWireMock, SpecBase}

class CitizenDetailsConnectorSpec extends SpecBase with HttpWireMock {
  override lazy val app: Application = new GuiceApplicationBuilder()
    .configure(
      conf = "microservice.services.citizen-details.port" -> server.port()
    )
    .build()
  private lazy val connector: CitizenDetailsConnector =
    app.injector.instanceOf[CitizenDetailsConnector]
  private def serviceUrl(utr: String) = s"/citizen-details/sautr/$utr"
  private val nino: String = "AA055075C"
  private val validSuccessResponse: String = """
  {
    "name": {
      "current": {
        "firstName": "John",
        "lastName": "Smith"
      },
      "previous": []
    },
    "ids": {
      "nino": "AA055075C"
    },
    "dateOfBirth": "11121971"
  }
  """

  "CitizenDetailsConnector" should {
    "return nino in case of a 200 response" in {
      simmulateGet(serviceUrl("utr"), OK, validSuccessResponse)
      val result = connector.getNino("utr")
      result.futureValue mustBe nino
    }
    "return erro in case of a non-200 response" in {
      simmulateGet(serviceUrl("invalidUtr"), BAD_REQUEST, "")
      val result = connector.getNino("invalidUtr")
      result.failed.futureValue mustBe Downstream_Error
    }
  }
}
