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

  "UtrValidator" should {
    "return true for valid UTRs" in {
      UtrValidator.isValidUtr("1234567890") mustBe true
      UtrValidator.isValidUtr("12345") mustBe true
      UtrValidator.isValidUtr("A12345678") mustBe true
      UtrValidator.isValidUtr("AB123") mustBe true
    }

    "return false for invalid UTRs" in {
      UtrValidator.isValidUtr("") mustBe false
      UtrValidator.isValidUtr("12345678901") mustBe false
      UtrValidator.isValidUtr("ABC@123") mustBe false
      UtrValidator.isValidUtr("ABC 123") mustBe false
    }
  }

}
