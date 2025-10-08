import sbt.*

object AppDependencies {

  private val bootstrapVersion = "10.1.0"


  val compile = Seq(
    "uk.gov.hmrc"             %% "bootstrap-backend-play-30"  % bootstrapVersion,

  )

  val testDependencies = Seq(
    "uk.gov.hmrc"             %% "bootstrap-test-play-30"     % bootstrapVersion            % Test,
    "org.scalacheck"          %% "scalacheck"                 % "1.18.1"                    % Test,
    "org.scalatest"           %% "scalatest"                  % "3.2.19"                    % Test,
    "org.scalatestplus"       %% "scalacheck-1-18"            % "3.2.19.0"                  % Test
  )
  val test = testDependencies
  val it = testDependencies
}
