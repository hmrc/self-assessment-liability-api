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

package controllers.documentation

import config.AppConfig
import controllers.Assets
import play.api.libs.json.*
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}
import scala.io.Source

@Singleton
class DocumentationController @Inject() (
    assets: Assets,
    cc: ControllerComponents,
    appConfig: AppConfig
) extends BackendController(cc) {

  def definition(): Action[AnyContent] = Action {
    val status = appConfig.apiPlatformStatus
    val enabled = appConfig.apiPlatformEndpointsEnabled
    val json = Json.parse(Source.fromResource("public/api/definition.json").mkString)
    val optimus = (__ \ "api" \ "versions").json.update(
      Reads
        .list(
          (__ \ "status").json
            .update(Reads.of[JsString].map(_ => JsString(status)))
            andThen
              (__ \ "endpointsEnabled").json
                .update(
                  Reads.of[JsBoolean].map(_ => JsBoolean(enabled))
                )
        )
        .map(JsArray(_))
    )
    json
      .transform(optimus)
      .map(Ok(_))
      .getOrElse(throw new RuntimeException("Failed to create definition.json"))
  }

  def specification(version: String, file: String): Action[AnyContent] = {
    assets.at(s"/public/api/conf/$version", file)
  }
}
