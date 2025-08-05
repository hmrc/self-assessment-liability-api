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

case class ChargeDetails(
    chargeId: String,
    creationDate: String,
    chargeType: String,
    chargeAmount: Double,
    outstandingAmount: Double,
    taxYear: String,
    dueDate: String,
    interestAmountDue: Option[Double],
    accruingInterest: Option[Double],
    accruingInterestDateRange: Option[AccruingInterestDateRange],
    accruingInterestRate: Option[Double],
    amendments: Option[Set[Amendments]],
    codedOutDetail: Option[Set[CodedOutDetail]]
)

object ChargeDetails {
  implicit val format: OFormat[ChargeDetails] = Json.format[ChargeDetails]
}
