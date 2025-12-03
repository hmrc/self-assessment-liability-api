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

package config

import javax.inject.{Inject, Singleton}
import play.api.Configuration
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.auth.core.ConfidenceLevel

@Singleton
class AppConfig @Inject() (config: Configuration, servicesConfig: ServicesConfig) {
  val useStubsForCitizenDetails: Boolean =
    servicesConfig.getBoolean("features.toggles.stubCid")
  val citizenDetailsLookup: String =
    if useStubsForCitizenDetails then servicesConfig.baseUrl("stubs")
    else servicesConfig.baseUrl("citizen-details")
  val hipBaseUrl: String = servicesConfig.baseUrl("hip")

  def confidenceLevel: ConfidenceLevel =
    ConfidenceLevel
      .fromInt(config.get[Int]("confidenceLevel"))
      .getOrElse(ConfidenceLevel.L250)

  lazy val apiPlatformStatus: String = servicesConfig.getString("features.api-platform.status")
  lazy val apiPlatformEndpointsEnabled: Boolean =
    servicesConfig.getBoolean("features.api-platform.endpoints-enabled")

  val hipClientId: String = config.get[String]("microservice.services.hip.clientId")
  val hipClientSecret: String = config.get[String]("microservice.services.hip.clientSecret")
}
