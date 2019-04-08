import play.core.PlayVersion
import play.sbt.PlayImport.ws
import sbt.{ModuleID, _}

object AppDependencies {

  lazy val appDependencies: Seq[ModuleID] = compile ++ test()

  private val scalaTestPlusVersion = "2.0.0"
  private val wiremockVersion = "2.5.1"
  private val mockitoVersion = "2.23.0"
  private val catsVersion = "1.4.0"

  val compile = Seq(
    "uk.gov.hmrc" %% "play-reactivemongo" % "6.4.0",
    ws,
    "uk.gov.hmrc" %% "bootstrap-play-25" % "4.10.0",
    "org.typelevel" %% "cats-core" % catsVersion
  )

  def test(scope: String = "test,it") = Seq(
    "uk.gov.hmrc" %% "hmrctest" % "3.6.0-play-25" % scope,
    "org.scalatest" %% "scalatest" % "2.2.6" % scope,
    "org.pegdown" % "pegdown" % "1.6.0" % scope,
    "com.typesafe.play" %% "play-test" % PlayVersion.current % scope,
    "org.scalatestplus.play" %% "scalatestplus-play" % scalaTestPlusVersion % scope,
    "com.github.tomakehurst" % "wiremock" % wiremockVersion % scope,
    "org.mockito" % "mockito-core" % mockitoVersion % scope
  )

}