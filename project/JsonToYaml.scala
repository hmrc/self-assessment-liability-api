import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.{YAMLFactory, YAMLGenerator, YAMLMapper}
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.iheart.sbtPlaySwagger.SwaggerPlugin.autoImport.swagger
import play.api.libs.json.*
import play.api.libs.json.Reads.*
import sbt.*
import sbt.Keys.*

import scala.language.postfixOps

object JsonToYaml {

  val apiContext      = "/individuals/self-assessment/breakdown"
  val routesToYamlOas = taskKey[Unit]("Generate YAML OpenAPI specification from JSON")

  val updateVersion = (__ \ "version").json.update(
    __.read[String]
      .map { obj =>
        JsString(obj.stripSuffix("-SNAPSHOT"))
      }
  )

  def settings: Seq[Setting[_]] = Seq(
    routesToYamlOas := {
      swagger.value

      val yamlFactory = new YAMLFactory()
        .configure(YAMLGenerator.Feature.WRITE_DOC_START_MARKER, false)
        .configure(YAMLGenerator.Feature.MINIMIZE_QUOTES, true)
        .configure(YAMLGenerator.Feature.USE_NATIVE_TYPE_ID, false)
        .configure(YAMLGenerator.Feature.SPLIT_LINES, false)
      val jsonMapper = new ObjectMapper().registerModule(DefaultScalaModule)
      val yamlMapper = new YAMLMapper(yamlFactory).registerModule(DefaultScalaModule)

      val jsonFile: File = baseDirectory.value / "target/swagger/swagger.json"
      val yamlFile: File = baseDirectory.value / "resources/public/api/conf/1.0/application.yaml"

      val jsonString = IO.read(jsonFile)
      val parsedJson = Json.parse(jsonString)

      val openapi    = (parsedJson \ "openapi").as[JsString]
      val info       = (parsedJson \ "info").as[JsObject].transform(updateVersion).get
      val tags       = (parsedJson \ "tags").as[JsArray]
      val components = (parsedJson \ "components").as[JsObject]
      val servers    = (parsedJson \ "servers").asOpt[JsArray].getOrElse(JsArray())

      val pathsJson = (parsedJson \ "paths").as[JsObject]
      val processedPaths = JsObject(
        pathsJson.fields.map { case (path, value) =>
          s"$apiContext$path" -> value
        }
      )

      val orderedJson = Json.obj(
        "openapi"    -> openapi,
        "info"       -> info,
        "tags"       -> tags,
        "paths"      -> processedPaths,
        "components" -> components,
        "servers"    -> servers
      )

      val jsonNodeTree = jsonMapper.readTree(orderedJson.toString)
      val jsonAsYaml   = yamlMapper.writeValueAsString(jsonNodeTree)
      IO.write(yamlFile, jsonAsYaml)
    }
  )
}
