import com.iheart.sbtPlaySwagger.SwaggerPlugin.autoImport.*
import sbt.Def

object PlaySwagger {
  lazy val settings: Seq[Def.Setting[_]] = Seq(
    swaggerDomainNameSpaces := Seq(
      "models.AccruingInterestPeriod",
      "models.Amendment",
      "models.BalanceDetails",
      "models.ChargeDetails",
      "models.CodedOutDetail",
      "models.HipResponse",
      "models.PaymentHistoryDetails",
      "models.RefundDetails"
    ),
    swaggerRoutesFile := "app.routes",
    swaggerV3 := true,
    swaggerPrettyJson := true
  )
}
