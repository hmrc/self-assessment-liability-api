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

class CodedOutDetailSpec extends AnyWordSpec with Matchers {
  val validCodedOutDetail = CodedOutDetail(
    totalAmount = BigDecimal(200),
    effectiveStartDate = LocalDate.of(23, 4, 6),
    effectiveEndDate = LocalDate.of(23, 5, 1)
  )

  "CodedOutDetail model" should {

    "Allow construction when totalAmount is zero or positive" in {
      validCodedOutDetail.totalAmount shouldBe >=(BigDecimal(0))
    }

    "Throw illegal argument exception if totalAmount is negative" in {
      val exception = intercept[IllegalArgumentException] {
        validCodedOutDetail.copy(totalAmount = BigDecimal(-200))
      }

      exception shouldBe a[IllegalArgumentException]
      exception.getMessage should include(s"totalAmount must be >= 0 but was -200")
    }
  }
}
