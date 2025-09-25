package controllers.documentation

import org.scalatest.time.SpanSugar.convertIntToGrainOfTime
import uk.gov.hmrc.http.client.HttpClientV2
import utils.IntegrationSpecBase
import uk.gov.hmrc.http.{HttpResponse}
import uk.gov.hmrc.play.bootstrap.http.HttpClientV2Provider

import java.net.URI
import scala.concurrent.Await

class DocumentationControllerISpec extends IntegrationSpecBase {

  lazy val provider: HttpClientV2Provider = app.injector.instanceOf[HttpClientV2Provider]
  lazy val client: HttpClientV2 = provider.get()

  val baseUrl = s"http://localhost:$port/api/definition"

  "Integration Tests for SA Documentation Controller" must {
    "Connection" should {
      "GET /status return OK with status ALPHA" in {
        simulateGet("/api/definition", OK, "")
        val result =
          Await.result(
            client
              .get(URI.create(baseUrl).toURL)
              .setHeader("Authorization" -> "Header")
              .execute[HttpResponse],
            5.seconds
          )

        result.status mustEqual OK
        val json = result.json
        (json \ "api" \ "versions" \ 0 \ "status").as[String] mustEqual "ALPHA"
      }

      "GET /endpointsEnabled return OK with endpointsEnabled false" in {
        simulateGet("/api/definition", OK, "")
        val result =
          Await.result(
            client
              .get(URI.create(baseUrl).toURL)
              .setHeader("Authorization" -> "Header")
              .execute[HttpResponse],
            5.seconds
          )

        result.status mustEqual OK
        val json = result.json
        (json \ "api" \ "versions" \ 0 \ "endpointsEnabled").as[Boolean] mustEqual false
      }

      "GET /version return OK with version 1.0" in {
        simulateGet("/api/definition", OK, "")
        val result =
          Await.result(
            client
              .get(URI.create(baseUrl).toURL)
              .setHeader("Authorization" -> "Header")
              .execute[HttpResponse],
            5.seconds
          )

        result.status mustEqual OK
        val json = result.json
        (json \ "api" \ "versions" \ 0 \ "version").as[String] mustEqual "1.0"
      }

      "GET /categories return OK with categories SELF_ASSESSMENT" in {
        simulateGet("/api/definition", OK, "")
        val result =
          Await.result(
            client
              .get(URI.create(baseUrl).toURL)
              .setHeader("Authorization" -> "Header")
              .execute[HttpResponse],
            5.seconds
          )

        result.status mustEqual OK
        val json = result.json
        (json \ "api" \ "categories" \ 0).as[String] mustEqual "SELF_ASSESSMENT"
      }

      "GET /context return OK with context individuals/self-assessment/breakdown" in {
        simulateGet("/api/definition", OK, "")
        val result =
          Await.result(
            client
              .get(URI.create(baseUrl).toURL)
              .setHeader("Authorization" -> "Header")
              .execute[HttpResponse],
            5.seconds
          )

        result.status mustEqual OK
        val json = result.json
        (json \ "api" \ "context").as[String] mustEqual "individuals/self-assessment/breakdown"
      }

      "GET /description return OK with description descriptionText" in {
        simulateGet("/api/definition", OK, "")
        val result =
          Await.result(
            client
              .get(URI.create(baseUrl).toURL)
              .setHeader("Authorization" -> "Header")
              .execute[HttpResponse],
            5.seconds
          )

        val descriptionText =
          "This API provides a breakdown of Self Assessment liability, showing overdue, payable and pending amounts, within HMRC tax account services."
        result.status mustEqual OK
        val json = result.json
        (json \ "api" \ "description").as[String] mustEqual descriptionText
      }

      "GET /name return OK with name View Self Assessment Account" in {
        simulateGet("/api/definition", OK, "")
        val result =
          Await.result(
            client
              .get(URI.create(baseUrl).toURL)
              .setHeader("Authorization" -> "Header")
              .execute[HttpResponse],
            5.seconds
          )

        result.status mustEqual OK
        val json = result.json
        (json \ "api" \ "name").as[String] mustEqual "View Self Assessment Account"
      }
    }
  }
}
