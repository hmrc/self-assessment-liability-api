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

import models.ServiceErrors.*
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

class ServiceErrorsSpec extends AnyWordSpec with Matchers {

  "ServiceErrors" should {

    "have no exception message by default" in {
      Downstream_Error.getMessage mustBe null
      Service_Currently_Unavailable_Error.getMessage mustBe null
      Json_Validation_Error.getMessage mustBe null
      No_Data_Found_Error.getMessage mustBe null
      Invalid_Start_Date_Error.getMessage mustBe null
      Invalid_Utr_Error.getMessage mustBe null
      Unauthorised_Error.getMessage mustBe null
    }

    "retain their readable toString values" in {
      Downstream_Error.toString mustBe "Downstream_Error"
      Service_Currently_Unavailable_Error.toString mustBe "Service_Currently_Unavailable_Error"
      Json_Validation_Error.toString mustBe "Json_Validation_Error"
      No_Data_Found_Error.toString mustBe "No_Data_Found_Error"
      Invalid_Start_Date_Error.toString mustBe "Invalid_Start_Date_Error"
      Invalid_Utr_Error.toString mustBe "Invalid_Utr_Error"
      Unauthorised_Error.toString mustBe "Unauthorised_Error"
    }
  }
}