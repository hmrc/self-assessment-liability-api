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

package shared

import models.*
import org.scalacheck.Gen

import java.time.{LocalDate, ZoneId}

object HipResponseGenerator {

  def formatTaxYear(year: String) = {
    val startYear = year.toInt
    val endYear = (startYear + 1)
    s"$startYear-$endYear"
  }

  val localDateGen: Gen[LocalDate] = for {
    year <- Gen.choose(LocalDate.now().minusYears(7).getYear, LocalDate.now().getYear)
    date <- Gen.calendar.map(c => LocalDate.ofInstant(c.toInstant, ZoneId.of("UTC")).withYear(year))
  } yield date

  val codedOutDetailGen: Gen[CodedOutDetail] = for {
    totalAmount <- Gen.choose(0.0, 10000.0).map(BigDecimal(_))
    startDate <- localDateGen
    daysToAdd <- Gen.choose(1, 365)
    endDate = startDate.plusDays(daysToAdd)
  } yield CodedOutDetail(
    totalAmount = totalAmount,
    effectiveStartDate = startDate,
    effectiveEndDate = endDate
  )

  val balanceDetailsGen: Gen[BalanceDetails] = for {
    totalOverdueBalance <- Gen.choose(0.0, 50000.0).map(BigDecimal(_))
    totalPayableBalance <- Gen.choose(0.0, 100000.0).map(BigDecimal(_))
    earliestPayableDueDate <- Gen.option(localDateGen)
    totalPendingBalance <- Gen.choose(0.0, 25000.0).map(BigDecimal(_))
    earliestPendingDueDate <- Gen.option(localDateGen)
    totalBalance <- Gen.choose(0.0, 150000.0).map(BigDecimal(_))
    totalCreditAvailable <- Gen.choose(0.0, 20000.0).map(BigDecimal(_))
    codedOutDetails <- Gen.containerOf[List, CodedOutDetail](codedOutDetailGen)
  } yield BalanceDetails(
    totalOverdueBalance = totalOverdueBalance,
    totalPayableBalance = totalPayableBalance,
    earliestPayableDueDate = earliestPayableDueDate,
    totalPendingBalance = totalPendingBalance,
    earliestPendingDueDate = earliestPendingDueDate,
    totalBalance = totalBalance,
    totalCreditAvailable = totalCreditAvailable,
    codedOutDetail = codedOutDetails
  )

  val accruingInterestPeriodGen: Gen[AccruingInterestPeriod] = for {
    startDate <- localDateGen
    daysToAdd <- Gen.choose(1, 365)
    endDate = startDate.plusDays(daysToAdd)
  } yield AccruingInterestPeriod(
    interestStartDate = startDate,
    interestEndDate = endDate
  )

  val amendmentsGen: Gen[Amendment] = for {
    amendmentDate <- localDateGen
    amendmentAmount <- Gen.choose(-10000.0, 10000.0).map(BigDecimal(_))
    amendmentReason <- Gen.oneOf(
      "Correction",
      "Late Filing Penalty",
      "Interest Adjustment",
      "Manual Override"
    )
    updatedChargeAmount <- Gen.option(Gen.choose(0.0, 50000.0)).map(_.map(BigDecimal(_)))
    paymentMethod <- Gen.option(Gen.oneOf("Bank Transfer", "Credit Card", "Cheque", "Direct Debit"))
    paymentDate <- Gen.option(localDateGen)
  } yield Amendment(
    amendmentDate = amendmentDate,
    amendmentAmount = amendmentAmount,
    amendmentReason = amendmentReason,
    updatedChargeAmount = updatedChargeAmount,
    paymentMethod = paymentMethod,
    paymentDate = paymentDate
  )

  val chargeDetailsGen: Gen[ChargeDetails] = for {
    chargeId <- Gen.alphaNumStr.suchThat(_.nonEmpty)
    creationDate <- localDateGen
    chargeType <- Gen.oneOf("Income Tax", "VAT", "Corporation Tax", "PAYE", "National Insurance")
    chargeAmount <- Gen.choose(1.0, 100000.0).map(BigDecimal(_))
    outstandingAmount <- Gen.choose(0.0, 50000.0).map(BigDecimal(_))
    taxYear <- Gen.choose(2020, 2025).map(year => formatTaxYear(year.toString))
    dueDate <- localDateGen
    outstandingInterestDue <- Gen.option(Gen.choose(0.0, 5000.0)).map(_.map(BigDecimal(_)))
    accruingInterest <- Gen.option(Gen.choose(0.0, 1000.0)).map(_.map(BigDecimal(_)))
    accruingInterestPeriod <- Gen.option(accruingInterestPeriodGen)
    accruingInterestRate <- Gen.option(Gen.choose(0.0, 15.0)).map(_.map(BigDecimal(_)))
    amendments <- Gen.containerOf[List, Amendment](amendmentsGen)
  } yield ChargeDetails(
    chargeId = chargeId,
    creationDate = creationDate,
    chargeType = chargeType,
    chargeAmount = chargeAmount,
    outstandingAmount = outstandingAmount,
    taxYear = taxYear,
    dueDate = dueDate,
    outstandingInterestDue = outstandingInterestDue,
    accruingInterest = accruingInterest,
    accruingInterestPeriod = accruingInterestPeriod,
    accruingInterestRate = accruingInterestRate,
    amendments = amendments
  )

  val refundDetailsGen: Gen[RefundDetails] = for {
    refundDate <- Gen.option(localDateGen)
    refundMethod <- Gen.option(Gen.oneOf("Bank Transfer", "Cheque", "Credit Card Refund"))
    refundRequestDate <- Gen.option(localDateGen)
    refundRequestAmount <- Gen.choose(1.0, 50000.0).map(BigDecimal(_))
    refundDescription <- Gen.option(
      Gen.oneOf("Overpayment", "Double Payment", "Incorrect Charge", "System Error")
    )
    interestAddedToRefund <- Gen.option(Gen.choose(0.0, 1000.0)).map(_.map(BigDecimal(_)))
    totalRefundAmount <- Gen.choose(1.0, 55000.0)
    refundStatus <- Gen.option(Gen.oneOf("Pending", "Approved", "Processed", "Rejected"))
  } yield RefundDetails(
    refundDate = refundDate,
    refundMethod = refundMethod,
    refundRequestDate = refundRequestDate,
    refundRequestAmount = refundRequestAmount,
    refundDescription = refundDescription,
    interestAddedToRefund = interestAddedToRefund,
    totalRefundAmount = totalRefundAmount,
    refundStatus = refundStatus
  )

  val paymentHistoryDetailsGen: Gen[PaymentHistoryDetails] = for {
    paymentAmount <- Gen.choose(1.0, 100000.0)
    paymentReference <- Gen.alphaNumStr.suchThat(_.nonEmpty)
    paymentMethod <- Gen.option(Gen.oneOf("Bank Transfer", "Credit Card", "Direct Debit", "Cheque"))
    paymentDate <- localDateGen
    processedDate <- Gen.option(localDateGen)
    allocationReference <- Gen.listOf(Gen.alphaNumStr)
  } yield PaymentHistoryDetails(
    paymentAmount = paymentAmount,
    paymentReference = paymentReference,
    paymentMethod = paymentMethod,
    paymentDate = paymentDate,
    processedDate = processedDate,
    allocationReference = allocationReference
  )

  val hipResponseGen: Gen[HipResponse] = for {
    balanceDetails <- balanceDetailsGen
    chargeDetails <- Gen.containerOf[List, ChargeDetails](chargeDetailsGen)
    refundDetails <- Gen.containerOf[List, RefundDetails](refundDetailsGen)
    paymentHistoryDetails <-
      Gen.containerOf[List, PaymentHistoryDetails](paymentHistoryDetailsGen)

  } yield HipResponse(
    balanceDetails = balanceDetails,
    chargeDetails = chargeDetails,
    refundDetails = refundDetails,
    paymentHistoryDetails = paymentHistoryDetails
  )
}
