/*
 * Copyright 2024 HM Revenue & Customs
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

package utils

import shared.SpecBase

class UtrValidatorSpec extends SpecBase {

  // List of valid values used for unit test
  val validList = List("1234567890", "9876543210")

  // List of invalid values used for unit test
  val invalidList = List("", "12345678901", "ABC@123", "ABC123", "A12345678", "AB123", "12345")

  "UtrValidator" should {

    validList.foreach(validList =>
      s"return true for valid UTRs $validList" in {
        UtrValidator.isValidUtr(s"$validList") mustEqual true
      }
    )

    invalidList.foreach(invalidList =>
      s"return false for invalid UTRs $invalidList" in {
        UtrValidator.isValidUtr(s"$invalidList") mustEqual false
      }
    )
  }
}
