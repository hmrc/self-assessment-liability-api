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

case class PaymentHistoryDetails(
    paymentAmount: BigDecimal,
    paymentReference: Option[String],
    paymentMethod: Option[String],
    paymentDate: LocalDate,
    processedDate: Option[LocalDate],
    allocationReference: Option[String]
) {
  require(
    paymentAmount >= 0,
    s"paymentAmount must be >= 0 but was $paymentAmount"
  )
}

object PaymentHistoryDetails {
  implicit val format: OFormat[PaymentHistoryDetails] = Json.format[PaymentHistoryDetails]
}
