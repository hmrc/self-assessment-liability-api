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

package services

import connectors.{CitizenDetailsConnector, HipConnector, MtdIdentifierLookupConnector}
import models.HipResponse
import models.ServiceErrors.{Downstream_Error, Json_Validation_Error}
import play.api.Logging
import uk.gov.hmrc.http.HeaderCarrier
import utils.TaxYearFormatter

import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SelfAssessmentService @Inject() (
    cidConnector: CitizenDetailsConnector,
    mtdConnector: MtdIdentifierLookupConnector,
    hipConnector: HipConnector
)(implicit ec: ExecutionContext)
    extends Logging {
  def getMtdIdFromUtr(utr: String)(implicit hc: HeaderCarrier): Future[String] = {
    for {
      maybeNino <- cidConnector.getNino(utr)
      mtdId <- maybeNino.map(mtdConnector.getMtdId(_)).getOrElse(Future.failed(Downstream_Error))
    } yield mtdId.mtdbsa
  }

  def viewAccountService(utr: String, dateFrom: LocalDate, dateTo: LocalDate)(implicit
      hc: HeaderCarrier
  ): Future[HipResponse] = {
    (for {
      hipResponse <- hipConnector.getSelfAssessmentData(
        utr,
        dateFrom,
        dateTo
      )
      hipResponseWithFormattedTaxYears = TaxYearFormatter.formatter(hipResponse)
    } yield hipResponseWithFormattedTaxYears).recoverWith { case _: NumberFormatException =>
      logger.warn(s"At least one tax year received does not follow YYYY format")
      Future.failed(Json_Validation_Error)
    }
  }

}
