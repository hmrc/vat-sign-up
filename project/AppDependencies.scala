import play.core.PlayVersion
import play.sbt.PlayImport.ws
import sbt.{ModuleID, _}

object AppDependencies {

  lazy val appDependencies: Seq[ModuleID] = compile ++ test()

  private val scalaTestPlusVersion = "3.1.3"
  private val wiremockVersion = "2.27.1"
  private val mockitoVersion = "3.4.0"
  private val catsVersion = "1.6.1"

  val compile = Seq(
    "uk.gov.hmrc" %% "simple-reactivemongo" % "7.30.0-play-26",
    ws,
    "uk.gov.hmrc" %% "bootstrap-play-26" % "1.13.0",
    "org.typelevel" %% "cats-core" % catsVersion
  )

  def test(scope: String = "test,it") = Seq(
    "org.scalatest" %% "scalatest" % "3.0.8" % scope,
    "org.pegdown" % "pegdown" % "1.6.0" % scope,
    "com.typesafe.play" %% "play-test" % PlayVersion.current % scope,
    "org.scalatestplus.play" %% "scalatestplus-play" % scalaTestPlusVersion % scope,
    "com.github.tomakehurst" % "wiremock-jre8" % wiremockVersion % scope,
    "org.mockito" % "mockito-core" % mockitoVersion % scope
  )

}
