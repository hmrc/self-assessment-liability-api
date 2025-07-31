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

package services

import connectors.{CitizenDetailsConnector, HipConnector, MtdIdentifierLookupConnector}
import models.{HipResponse, MtdId}
import models.ServiceErrors.*
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers.*
import shared.{HttpWireMock, SpecBase}

import scala.concurrent.Future

class SelfAssessmentServiceSpec extends SpecBase with HttpWireMock {
  override lazy val app: Application = new GuiceApplicationBuilder().build()

  private val cidConnector: CitizenDetailsConnector = mock[CitizenDetailsConnector]
  private val mtdConnector: MtdIdentifierLookupConnector = mock[MtdIdentifierLookupConnector]
  private val hipConnector: HipConnector = mock[HipConnector]
  private val selfAssessmentService: SelfAssessmentService = new SelfAssessmentService(
    cidConnector,
    mtdConnector,
    hipConnector
  )(
    ec
  )
  private val utr: String = "1234567890"
  private val date: String = "2025-04-06"
  private val nino: String = "AA055075C"
  private val mtdId: MtdId = MtdId("MtdItId")

  "SelfAssessmentServiceSpec" when {
    "getting MTDID from UTR" should {
      "return MTDID as a string when successful" in {
        when(cidConnector.getNino(any())(any(), any()))
          .thenReturn(Future.successful(nino))
        when(mtdConnector.getMtdId(ArgumentMatchers.eq(nino))(any(), any()))
          .thenReturn(Future.successful(mtdId))

        running(app) {
          selfAssessmentService
            .getMtdIdFromUtr(utr)
            .onComplete(success => success.get mustBe mtdId.mtdbsa)
        }
      }

      "return an error when the request fails" in {
        when(cidConnector.getNino(any())(any(), any()))
          .thenReturn(Future.failed(Throwable()))

        running(app) {
          selfAssessmentService
            .getMtdIdFromUtr(utr)
            .onComplete(error => error mustBe Throwable())
        }
      }
    }

    "getting HIP data" should {
      "return details as a HipResponse object when successful" in {
        when(hipConnector.getSelfAssessmentData(any(), any(), any())(any(), any()))
          .thenReturn(Future.successful(hipResponse))

        running(app) {
          selfAssessmentService
            .getHipData(utr, date)
            .onComplete(success => success.get mustBe hipResponse)
        }
      }

      "return an error when the request fails" in {
        when(hipConnector.getSelfAssessmentData(any(), any(), any())(any(), any()))
          .thenReturn(Future.failed(HIP_Service_Unavailable))

        running(app) {
          selfAssessmentService
            .getHipData(utr, date)
            .onComplete(error => error.get mustBe HIP_Service_Unavailable)
        }
      }
    }
  }
}
