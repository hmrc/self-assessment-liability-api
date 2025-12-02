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

import play.api.libs.json.{Json, OFormat}

import java.time.LocalDate

case class ChargeDetails(
    chargeId: String,
    creationDate: LocalDate,
    chargeType: String,
    chargeAmount: BigDecimal,
    outstandingAmount: BigDecimal,
    taxYear: String,
    dueDate: LocalDate,
    outstandingInterestDue: Option[BigDecimal],
    accruingInterest: Option[BigDecimal],
    accruingInterestPeriod: Option[AccruingInterestPeriod],
    accruingInterestRate: Option[BigDecimal],
    amendments: List[Amendment]= List.empty
) {
  require(
    chargeAmount >= 0,
    s"chargeAmount must be >= 0 but was $chargeAmount"
  )
  require(
    outstandingAmount >= 0,
    s"outstandingAmount must be >= 0 but was $outstandingAmount"
  )
}

object ChargeDetails {
  implicit val format: OFormat[ChargeDetails] = Json.format[ChargeDetails]
}
