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

class ChargeDetailsSpec extends AnyWordSpec with Matchers {

  val validChargeDetail = ChargeDetails(
    chargeId = "12313213123213",
    creationDate = LocalDate.now(),
    chargeType = "ALASDAIR",
    chargeAmount = BigDecimal(100),
    outstandingAmount = BigDecimal(100),
    taxYear = "2019",
    dueDate = LocalDate.now(),
    outstandingInterestDue = None,
    accruingInterest = None,
    accruingInterestPeriod = None,
    accruingInterestRate = None,
    amendments = None
  )

  "ChargeDetails model" should {

    "Allow construction when chargeAmount and outstandingAmount is zero or positive" in {
      validChargeDetail.chargeAmount shouldBe >=(BigDecimal(0))
      validChargeDetail.outstandingAmount shouldBe >=(BigDecimal(0))
    }

    "Throw illegal argument exception if chargeAmount is negative" in {
      val exception = intercept[IllegalArgumentException] {
        validChargeDetail.copy(chargeAmount = BigDecimal(-200))
      }

      exception shouldBe a[IllegalArgumentException]
      exception.getMessage should include("chargeAmount must be >= 0 but was -200")
    }

    "Throw illegal argument exception if outstandingAmount is negative" in {
      val exception = intercept[IllegalArgumentException] {
        validChargeDetail.copy(outstandingAmount = BigDecimal(-200))
      }

      exception shouldBe a[IllegalArgumentException]
      exception.getMessage should include("outstandingAmount must be >= 0 but was -200")
    }
  }
}
