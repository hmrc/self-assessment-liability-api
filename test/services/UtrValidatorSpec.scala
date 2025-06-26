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

import shared.SpecBase
import org.scalatest.prop.TableDrivenPropertyChecks.forEvery
import org.scalatest.prop.Tables.Table
import utils.UtrValidator

class UtrValidatorSpec extends SpecBase {
    

  private val invalidUtrTable = Table(
    "Invalid UTR",
    "",
    "          ",
    "?!#*567890",
    "12345678901234567890"
  )

  private val validUtrTable = Table(
    "Valid UTR",
    "1",
    "123456",
    "aBc4567890",
    "1234567890",
    "0123456789",
    "0000000000",
    "9999999999"
  )

  "UtrValidationService" when {
    "validating a UTR" should {
      "return false for invalid UTRs" in {
        forEvery(invalidUtrTable) { utr =>
          UtrValidator.isValidUtr(utr) mustBe false
        }
      }

      "return true for valid UTRs" in {
        forEvery(validUtrTable) { utr =>
          UtrValidator.isValidUtr(utr) mustBe true
        }
      }
    }
  }
}
