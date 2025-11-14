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

class BalanceDetailsSpec extends AnyWordSpec with Matchers {

  val validBalanceDetail = BalanceDetails(
    totalOverdueBalance = 1000.00,
    totalPayableBalance = 500.00,
    earliestPayableDueDate = Some(LocalDate.of(2024, 2, 15)),
    totalPendingBalance = 200.00,
    earliestPendingDueDate = Some(LocalDate.of(2024, 6, 15)),
    totalBalance = 1700.00,
    totalCreditAvailable = 0.00,
    codedOutDetail = List.empty[CodedOutDetail]
  )

  "ChargeDetails model" should {

    "Allow construction when balances and amounts are greater than zero" in {
      validBalanceDetail.totalOverdueBalance shouldBe >=(BigDecimal(0))
      validBalanceDetail.totalPayableBalance shouldBe >=(BigDecimal(0))
      validBalanceDetail.totalPendingBalance shouldBe >=(BigDecimal(0))
      validBalanceDetail.totalCreditAvailable shouldBe >=(BigDecimal(0))
      validBalanceDetail.totalBalance shouldBe >=(BigDecimal(0))
      validBalanceDetail.earliestPayableDueDate shouldBe defined
      validBalanceDetail.earliestPendingDueDate shouldBe defined
    }

    "Throw illegal argument exception if totalOverdueBalance is negative" in {
      val exception = intercept[IllegalArgumentException] {
        validBalanceDetail.copy(totalOverdueBalance = BigDecimal(-200))
      }

      exception shouldBe a[IllegalArgumentException]
      exception.getMessage should include("totalOverdueBalance must be >= 0 but was -200")
    }

    "Throw illegal argument exception if totalPayableBalance is negative" in {
      val exception = intercept[IllegalArgumentException] {
        validBalanceDetail.copy(totalPayableBalance = BigDecimal(-200))
      }

      exception shouldBe a[IllegalArgumentException]
      exception.getMessage should include("totalPayableBalance must be >= 0 but was -200")
    }

    "Throw illegal argument exception if totalPendingBalance is negative" in {
      val exception = intercept[IllegalArgumentException] {
        validBalanceDetail.copy(totalPendingBalance = BigDecimal(-200))
      }

      exception shouldBe a[IllegalArgumentException]
      exception.getMessage should include("totalPendingBalance must be >= 0 but was -200")
    }

    "Throw illegal argument exception if totalCreditAvailable is negative" in {
      val exception = intercept[IllegalArgumentException] {
        validBalanceDetail.copy(totalCreditAvailable = BigDecimal(-200))
      }

      exception shouldBe a[IllegalArgumentException]
      exception.getMessage should include("totalCreditAvailable must be >= 0 but was -200")
    }

    "Throw illegal argument exception if totalBalance is negative" in {
      val exception = intercept[IllegalArgumentException] {
        validBalanceDetail.copy(totalBalance = BigDecimal(-200))
      }

      exception shouldBe a[IllegalArgumentException]
      exception.getMessage should include("totalBalance must be >= 0 but was -200")
    }

    "Throw illegal argument exception if earliestPayableDueDate is not defined" in {
      val exception = intercept[IllegalArgumentException] {
        validBalanceDetail.copy(earliestPayableDueDate = None)
      }

      exception shouldBe a[IllegalArgumentException]
      exception.getMessage should include(
        "earliestPayableDueDate must be defined when totalPayableBalance > 0 (was 500.0, earliestPayableDueDate=None)"
      )
    }

    "Throw illegal argument exception if earliestPendingDueDate is not defined" in {
      val exception = intercept[IllegalArgumentException] {
        validBalanceDetail.copy(earliestPendingDueDate = None)
      }

      exception shouldBe a[IllegalArgumentException]
      exception.getMessage should include(
        "earliestPendingDueDate must be defined when totalPendingBalance > 0 (was 200.0, earliestPendingDueDate=None)"
      )
    }
  }
}
