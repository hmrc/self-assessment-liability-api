import uk.gov.hmrc.DefaultBuildSettings

ThisBuild / majorVersion := 0
ThisBuild / scalaVersion := "3.6.4"

lazy val microservice = Project("self-assessment-liability-api", file("."))
  .enablePlugins(play.sbt.PlayScala, SbtDistributablesPlugin,SwaggerPlugin)
  .disablePlugins(JUnitXmlReportPlugin) //Required to prevent https://github.com/scalatest/scalatest/issues/1427
  .settings(
    libraryDependencies ++= AppDependencies.compile ++ AppDependencies.test,
    // https://www.scala-lang.org/2021/01/12/configuring-and-suppressing-warnings.html
    // suppress warnings in generated routes files
    scalacOptions += "-Wconf:src=routes/.*:s",
  )
  .settings(CodeCoverageSettings.settings: _*)
  .settings(
    Compile / unmanagedResourceDirectories += baseDirectory.value / "resources",
  )
  .settings(JsonToYaml.settings *)
  .settings(Validate.settings *)
  .settings(PlaySwagger.settings *)
  .settings(PlayKeys.playDefaultPort := 49094)
addCommandAlias("testAll", "; test ; it/test")
addCommandAlias("genValOas", "; clean ; routesToYamlOas ; validateOas")
addCommandAlias("testGenVal", "; testAll ; routesToYamlOas ; validateOas")

lazy val it = (project in file("it"))
  .enablePlugins(PlayScala)
  .dependsOn(microservice % "compile->compile,test;test->compile,test")
  .settings(DefaultBuildSettings.itSettings())
  .settings(libraryDependencies ++= AppDependencies.it)
  .settings( Test / scalaSource := baseDirectory.value / "test" / "scala")
