import sbt.Setting
import scoverage.ScoverageKeys

object CodeCoverageSettings {

  private val excludedPackages: Seq[String] = Seq(
    "<empty>",
    "Reverse.*",
    ".*Routes.*",
    "config.*",
    "testOnly.*",
    "testOnlyDoNotUseInAppConf.*",
    "models.AccruingInterestPeriod",
    "models.Amendment",
    "models.ApiErrorResponses",
    "models.CitizenDetailsResponse",
    "models.ApiErrorResponses",
    "models.HipError",
    "models.HipErrorDetails",
    "models.HipResponse",
    "models.HipResponseError",
    "models.MtdId",
    "models.ApiErrorResponses",
    "models.RequestPeriod",
    "models.RequestWithUtr",
    "models.ServiceErrors"
  )

  val settings: Seq[Setting[_]] = Seq(
    ScoverageKeys.coverageExcludedPackages := excludedPackages.mkString(";"),
    ScoverageKeys.coverageMinimumStmtTotal := 90,
    ScoverageKeys.coverageFailOnMinimum := true,
    ScoverageKeys.coverageHighlighting := true
  )
}
