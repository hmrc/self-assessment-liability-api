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

import models.{BalanceDetails, ChargeDetails, HipResponse}
import shared.SpecBase

import java.time.LocalDate

class TaxYearFormatterSpec extends SpecBase {

  val balanceDetails = BalanceDetails(
    totalOverdueBalance = 100,
    totalPayableBalance = 0,
    earliestPayableDueDate = None,
    totalPendingBalance = 0,
    earliestPendingDueDate = None,
    totalBalance = 100,
    totalCreditAvailable = 100,
    codedOutDetail = None
  )

  val charge = ChargeDetails(
    chargeId = "12313213123213",
    creationDate = LocalDate.now(),
    chargeType = "ALASDAIR",
    chargeAmount = 100,
    outstandingAmount = 100,
    taxYear = "2019",
    dueDate = LocalDate.now(),
    outstandingInterestDue = None,
    accruingInterest = None,
    accruingInterestPeriod = None,
    accruingInterestRate = None,
    amendments = None
  )

  "formatter" should {

    "reformat all tax years in a hip response object from YYYY to YYYY-YYYY+1" in {
      val hipResponse = HipResponse(
        balanceDetails = balanceDetails,
        chargeDetails = Some(List(charge)),
        refundDetails = None,
        paymentHistoryDetails = None
      )
      TaxYearFormatter.formatter(hipResponse).chargeDetails.map(_.map(_.taxYear mustEqual "2019-2020"))
    }
    "do nothing if chargeDetails is empty" in {
      val hipResponse = HipResponse(
        balanceDetails = balanceDetails,
        chargeDetails = None,
        refundDetails = None,
        paymentHistoryDetails = None
      )
      TaxYearFormatter.formatter(hipResponse) mustEqual hipResponse
    }
  }
}
