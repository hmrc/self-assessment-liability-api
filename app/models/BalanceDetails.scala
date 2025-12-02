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

case class BalanceDetails(
    totalOverdueBalance: BigDecimal,
    totalPayableBalance: BigDecimal,
    earliestPayableDueDate: Option[LocalDate],
    totalPendingBalance: BigDecimal,
    earliestPendingDueDate: Option[LocalDate],
    totalBalance: BigDecimal,
    totalCreditAvailable: BigDecimal,
    codedOutDetail: List[CodedOutDetail]= List.empty
) {
  require(
    totalOverdueBalance >= 0,
    s"totalOverdueBalance must be >= 0 but was $totalOverdueBalance"
  )
  require(
    totalPayableBalance >= 0,
    s"totalPayableBalance must be >= 0 but was $totalPayableBalance"
  )
  require(
    totalPendingBalance >= 0,
    s"totalPendingBalance must be >= 0 but was $totalPendingBalance"
  )
  require(
    totalCreditAvailable >= 0,
    s"totalCreditAvailable must be >= 0 but was $totalCreditAvailable"
  )
  require(
    totalBalance >= 0,
    s"totalBalance must be >= 0 but was $totalBalance"
  )
  require(
    totalPayableBalance == 0 || earliestPayableDueDate.isDefined,
    s"earliestPayableDueDate must be defined when totalPayableBalance > 0 (was $totalPayableBalance, earliestPayableDueDate=$earliestPayableDueDate)"
  )
  require(
    totalPendingBalance == 0 || earliestPendingDueDate.isDefined,
    s"earliestPendingDueDate must be defined when totalPendingBalance > 0 (was $totalPendingBalance, earliestPendingDueDate=$earliestPendingDueDate)"
  )
}

object BalanceDetails {
  implicit val format: OFormat[BalanceDetails] = Json.format[BalanceDetails]
}
