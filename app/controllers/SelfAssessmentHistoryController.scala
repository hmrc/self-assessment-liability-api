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

package controllers

import config.AppConfig
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import services.SelfAssessmentService
import uk.gov.hmrc.auth.core.AuthConnector

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class SelfAssessmentHistoryController @Inject() (
    override val authConnector: AuthConnector,
    val service: SelfAssessmentService,
    cc: ControllerComponents
)(implicit appConfig: AppConfig, ec: ExecutionContext)
    extends AuthenticateRequestController(cc, service, authConnector) {

  def getYourSelfAssessmentData(utr: String, fromDate: String): Action[AnyContent] =
    authorisedAction(utr) { implicit request =>
      service.getHipData(utr, fromDate)
    }
}
