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

import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{BeforeAndAfterEach, OptionValues}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.*
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

}
