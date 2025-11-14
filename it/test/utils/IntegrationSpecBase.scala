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



import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{reset, when}
import org.scalatest.wordspec.AnyWordSpec
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.{Application, inject}
import shared.{HttpWireMock, SpecBase}
import uk.gov.hmrc.auth.core.AffinityGroup.Organisation
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.{EmptyRetrieval, Retrieval, ~}
import uk.gov.hmrc.auth.core.{AffinityGroup, AuthConnector, ConfidenceLevel, InsufficientEnrolments}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps

trait IntegrationSpecBase
    extends SpecBase with HttpWireMock {
  val mockAuthConnector: AuthConnector = mock[AuthConnector]
  type RetrievalsType = Option[AffinityGroup] ~ ConfidenceLevel
  val retrievalResult: RetrievalsType = new~(Some(Organisation), ConfidenceLevel.L250)
  val utr = "1234567890"
  val nino = "GG000000Z"
  override def beforeEach(): Unit = {
    reset(mockAuthConnector)
    when(
      mockAuthConnector.authorise[RetrievalsType](any[Predicate](), any[Retrieval[RetrievalsType]]())(any[HeaderCarrier](), any[ExecutionContext]())
    ).thenReturn(Future.successful(retrievalResult))
    when(
      mockAuthConnector.authorise(any[Predicate](), eqTo(EmptyRetrieval))(any[HeaderCarrier](), any[ExecutionContext]())
    ).thenReturn(
      Future.failed(InsufficientEnrolments()),
      Future.successful(())
    )
    super.beforeEach()
  }

  override lazy val app: Application =
    server.start()
    new GuiceApplicationBuilder()
    .overrides(
      inject.bind[AuthConnector].toInstance(mockAuthConnector),
  )
    .configure(
   "play.http.errorHandler" -> "controllers.GlobalErrorHandler",
   "microservice.services.citizen-details.port" ->server.port(),
   "microservice.services.mtd-id-lookup.port" -> server.port(),
   "microservice.services.hip.port" -> server.port(),
   "auditing.enabled" -> false,
   "metrics.enabled" -> false
    )
    .build()
}
