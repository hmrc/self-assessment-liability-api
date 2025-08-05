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

package shared

import models.HipResponse
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{BeforeAndAfterEach, OptionValues}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.*
import play.api.libs.json.Json
import play.api.mvc.*
import play.api.test.*
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext

trait SpecBase
    extends AnyWordSpec
    with OptionValues
    with ScalaFutures
    with IntegrationPatience
    with BeforeAndAfterEach
    with Matchers
    with Results
    with HttpProtocol
    with Status
    with Writeables
    with GuiceOneAppPerSuite {

  implicit lazy val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
  implicit lazy val hc: HeaderCarrier = HeaderCarrier()

  val jsonSuccessResponse: String =
    """
      |{
      |  "balanceDetails": {
      |  "totalOverdueBalance": 500.00,
      |  "totalPayableBalance": 500.00,
      |  "payableDueDate": "2025-04-31",
      |  "totalPendingBalance": 1500.00,
      |  "pendingDueDate": "2025-07-15",
      |  "totalBalance": 2000.00,
      |  "totalCodedOut": 250.00,
      |  "totalCreditAvailable": 0.00
      |},
      |"chargeDetails": [
      |  {
      |    "chargeId": "KL3456789",
      |    "creationDate": "2025-05-22",
      |    "chargeType": "VATC",
      |    "chargeAmount": 1500.00,
      |    "outstandingAmount": 1500.00,
      |    "taxYear": "2025-2026",
      |    "dueDate": "2025-07-15",
      |    "amendments": [
      |      {
      |        "amendmentDate": "ad",
      |        "amendmentAmount": 0.00,
      |        "amendmentReason": "ar"
      |      }
      |    ],
      |    "codedOutDetail": [
      |      {
      |        "amount": 0.00,
      |        "effectiveDate": "ed",
      |        "taxYear": "ty",
      |        "effectiveTaxYear": "ety"
      |      }
      |    ]
      |  }
      |],
      |"refundDetails": [
      |  {
      |    "issueDate": "id",
      |    "refundMethod": "rm",
      |    "refundRequestDate": "rrd",
      |    "refundRequestAmount": 0.00,
      |    "refundReference": "rr",
      |    "interestAddedToRefund": 0.00,
      |    "refundActualAmount": 0.00,
      |    "refundStatus": "rs"
      |  }
      |],
      |"paymentHistoryDetails": [
      |  {
      |    "paymentAmount": 500.00,
      |    "paymentId": "payment reference id",
      |    "paymentMethod": "payment method",
      |    "paymentDate": "2025-04-11",
      |    "dateProcessed": "2025-04-15",
      |    "allocationReference": "allocation reference"
      |  }
      |]
    |}""".stripMargin
  val hipResponse: HipResponse = Json.parse(jsonSuccessResponse).as[HipResponse]
}
