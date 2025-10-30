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
import models.ServiceErrors.{Downstream_Error, Json_Validation_Error, No_Data_Found_Error}
import models.{MtdId, ServiceErrors}
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.{any, eq as meq}
import org.mockito.Mockito.when
import org.scalatest.matchers.should.Matchers.shouldBe
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks.forAll
import shared.{HipResponseGenerator, SpecBase}
import utils.TaxYearFormatter
import java.time.LocalDate
import scala.concurrent.Future
import scala.util.Random

class SelfAssessmentServiceSpec extends SpecBase {

  val mockCitizenDetailsConnector: CitizenDetailsConnector = mock[CitizenDetailsConnector]
  val mockMtdConnector: MtdIdentifierLookupConnector = mock[MtdIdentifierLookupConnector]
  val mockHipConnector: HipConnector = mock[HipConnector]

  val service: SelfAssessmentService =
    new SelfAssessmentService(mockCitizenDetailsConnector, mockMtdConnector, mockHipConnector)

  val testUtr = "1234567890"
  val testNino = "AB123456C"
  val testMtdId = "XAIT0000123456"

  "getMtdIdFromUtr" should {
    "return mtdId when both connectors return successful responses" in {
      when(mockCitizenDetailsConnector.getNino(meq(testUtr))(any(), any()))
        .thenReturn(Future.successful(Some(testNino)))

      when(mockMtdConnector.getMtdId(meq(testNino))(any(), any()))
        .thenReturn(Future.successful(MtdId(testMtdId)))

      val result = service.getMtdIdFromUtr(testUtr)

      result.futureValue mustBe testMtdId
    }

    "fail with Downstream_Error when CitizenDetailsConnector returns no NINO" in {
      when(mockCitizenDetailsConnector.getNino(meq(testUtr))(any(), any()))
        .thenReturn(Future.successful(None))

      val result = service.getMtdIdFromUtr(testUtr)

      result.failed.futureValue mustBe Downstream_Error
    }

    "fail with Downstream_Error when CitizenDetailsConnector fails" in {
      when(mockCitizenDetailsConnector.getNino(meq(testUtr))(any(), any()))
        .thenReturn(Future.failed(Downstream_Error))

      val result = service.getMtdIdFromUtr(testUtr)

      result.failed.futureValue mustBe Downstream_Error
    }

    "fail with Downstream_Error when MtdIdentifierLookupConnector fails" in {
      when(mockCitizenDetailsConnector.getNino(meq(testUtr))(any(), any()))
        .thenReturn(Future.successful(Some(testNino)))

      when(mockMtdConnector.getMtdId(meq(testNino))(any(), any()))
        .thenReturn(Future.failed(Downstream_Error))

      val result = service.getMtdIdFromUtr(testUtr)

      result.failed.futureValue mustBe Downstream_Error
    }

    "correctly pass the NINO from the first connector to the second connector" in {
      val anotherNino = "XY987654Z"

      when(mockCitizenDetailsConnector.getNino(meq(testUtr))(any(), any()))
        .thenReturn(Future.successful(Some(anotherNino)))

      when(mockMtdConnector.getMtdId(meq(anotherNino))(any(), any()))
        .thenReturn(Future.successful(MtdId(testMtdId)))

      val result = service.getMtdIdFromUtr(testUtr)

      result.futureValue mustBe testMtdId
    }
  }
  "viewAccountService" should {

    "Enquire for self assessment data starting with fromDate provided until today's date" in {
      val today = LocalDate.now()
      forAll(HipResponseGenerator.hipResponseGen) { hipResponse =>
        when(
          mockHipConnector.getSelfAssessmentData(
            meq("utr"),
            meq(today.minusYears(2)),
            meq(today)
          )(any(), any())
        ).thenReturn(Future.successful(hipResponse))
        val formattedResponse = TaxYearFormatter.formatter(hipResponse)
        val result = service.viewAccountService("utr", today.minusYears(2), today)
        result.futureValue shouldBe formattedResponse
      }
    }
    "Enquire for self assessment data with start date provided until today's date" in {
      val today = LocalDate.now()
      forAll(HipResponseGenerator.hipResponseGen) { hipResponse =>
        when(
          mockHipConnector.getSelfAssessmentData(
            meq("utr"),
            meq(today.minusYears(4)),
            meq(today)
          )(any(), any())
        ).thenReturn(Future.successful(hipResponse))
        val formattedResponse = TaxYearFormatter.formatter(hipResponse)
        val result = service.viewAccountService("utr", today.minusYears(4), today)
        result.futureValue shouldBe formattedResponse
      }
    }

    "Return failure if call to hip fails" in {
      val today = LocalDate.now()
      val randomError: ServiceErrors =
        Random().shuffle(List(Json_Validation_Error, No_Data_Found_Error, Downstream_Error)).head
      when(
        mockHipConnector.getSelfAssessmentData(
          meq("utr"),
          any(),
          any()
        )(any(), any())
      ).thenReturn(Future.failed(randomError))
      val result = service.viewAccountService("utr", today, today)
      result.failed.futureValue mustEqual randomError
    }

    "throw Json_Validation_Error if a taxYear in any format else than YYYY is received from HIP" in {
      val today = LocalDate.now()
      HipResponseGenerator.hipResponseGen.map{hipResponse =>
        val transformedHipResponse = hipResponse.copy(chargeDetails= hipResponse.chargeDetails.map{charge=>
          charge.copy(taxYear = charge.taxYear + "----")
          }
        )
        when(
          mockHipConnector.getSelfAssessmentData(
            meq("utr"),
            meq(today.minusYears(4)),
            meq(today)
          )(any(), any())
        ).thenReturn(Future.successful(transformedHipResponse))
        val result = service.viewAccountService("utr", today.minusYears(4), today)
        result.failed.futureValue shouldBe Json_Validation_Error
      }
    }
  }
}
