/*
 * Copyright 2026 HM Revenue & Customs
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

package models

import play.api.libs.json.Json
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class HipResponseSpec extends AnyWordSpec with Matchers {

  "HipResponse model" should {

    "Successfully deserialize a HIP response containing negative payment amounts" in {
      val json = Json.parse(
        """
          |{
          |  "balanceDetails": {
          |    "totalOverdueBalance": 1500,
          |    "totalPayableBalance": 0,
          |    "totalPendingBalance": 0,
          |    "totalBalance": 1500,
          |    "totalCreditAvailable": 1900
          |  },
          |  "paymentHistoryDetails": [
          |    {
          |      "allocationReference": "BCD-7206-20240916",
          |      "processedDate": "2025-01-10",
          |      "paymentAmount": 5300,
          |      "paymentMethod": "Bank GIRO Payment",
          |      "paymentDate": "2025-01-10"
          |    },
          |    {
          |      "processedDate": "2024-09-24",
          |      "paymentAmount": -1700,
          |      "paymentMethod": "BANK GIRO RECEIPTS",
          |      "paymentReference": "196000001008",
          |      "paymentDate": "2025-01-10"
          |    },
          |    {
          |      "processedDate": "2024-09-24",
          |      "paymentAmount": -200,
          |      "paymentMethod": "BANK GIRO RECEIPTS",
          |      "paymentReference": "196000001009",
          |      "paymentDate": "2025-01-10"
          |    }
          |  ]
          |}
          |""".stripMargin
      )

      val response = json.as[HipResponse]

      response.paymentHistoryDetails.map(_.paymentAmount) shouldBe Seq(
        BigDecimal(5300),
        BigDecimal(-1700),
        BigDecimal(-200)
      )
    }
  }
}
