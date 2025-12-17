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

package models

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.time.LocalDate

class PaymentHistoryDetailsSpec extends AnyWordSpec with Matchers {

  val validPaymentHistory = PaymentHistoryDetails(
    paymentAmount = BigDecimal(500),
    paymentReference = Some("PAY123456"),
    paymentMethod = Some("Bank Transfer"),
    paymentDate = LocalDate.of(2025, 4, 11),
    processedDate = Some(LocalDate.of(2025, 4, 15)),
    allocationReference = Some("AB1234567")
  )

  "PaymentHistoryDetails model" should {

    "Allow construction when paymentAmount is zero or positive" in {
      validPaymentHistory.paymentAmount shouldBe >=(BigDecimal(0))
    }

    "Throw illegal argument exception if paymentAmount is negative" in {
      val exception = intercept[IllegalArgumentException] {
        validPaymentHistory.copy(paymentAmount = BigDecimal(-50))
      }

      exception shouldBe a[IllegalArgumentException]
      exception.getMessage should include("paymentAmount must be >= 0 but was -50")
    }

    "Handle none paymentReference correctly" in {
      val exception = validPaymentHistory.copy(paymentReference = None)

      exception.paymentReference shouldBe None
    }
  }
}
