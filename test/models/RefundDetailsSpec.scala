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

class RefundDetailsSpec extends AnyWordSpec with Matchers {

  val validRefund = RefundDetails(
    refundDate = Some(LocalDate.of(2024, 1, 10)),
    refundMethod = Some("Bank Transfer"),
    refundRequestDate = Some(LocalDate.of(2023, 12, 12)),
    refundRequestAmount = BigDecimal(350),
    refundDescription = Some("From overpayment from return 05 APR 23"),
    interestAddedToRefund = Some(5.25),
    totalRefundAmount = BigDecimal(0),
    refundStatus = Some("processed")
  )

  "RefundDetails model" should {

    "Allow construction when refundRequestAmount and totalRefundAmount is zero or positive" in {
      validRefund.refundRequestAmount shouldBe >=(BigDecimal(0))
      validRefund.totalRefundAmount shouldBe >=(BigDecimal(0))
    }

    "Throw illegal argument exception if refundRequestAmount is negative" in {
      val exception = intercept[IllegalArgumentException] {
        validRefund.copy(refundRequestAmount = BigDecimal(-100))
      }

      exception shouldBe a[IllegalArgumentException]
      exception.getMessage should include("refundRequestAmount must be >= 0 but was -100")
    }

    "Throw illegal argument exception if totalRefundAmount is negative" in {
      val exception = intercept[IllegalArgumentException] {
        validRefund.copy(totalRefundAmount = BigDecimal(-100))
      }

      exception shouldBe a[IllegalArgumentException]
      exception.getMessage should include("totalRefundAmount must be >= 0 but was -100")
    }
  }
}
