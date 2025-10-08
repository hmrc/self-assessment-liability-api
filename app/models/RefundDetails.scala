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

case class RefundDetails(
    refundDate: Option[LocalDate],
    refundMethod: Option[String],
    refundRequestDate: Option[LocalDate],
    refundRequestAmount: BigDecimal,
    refundDescription: Option[String],
    interestAddedToRefund: Option[BigDecimal],
    totalRefundAmount: BigDecimal,
    refundStatus: Option[String]
) {
  require(refundRequestAmount > 0 && totalRefundAmount > 0)
}

object RefundDetails {
  implicit val format: OFormat[RefundDetails] = Json.format[RefundDetails]
}
