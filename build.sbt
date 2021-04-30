import uk.gov.hmrc.DefaultBuildSettings.{addTestReportOption, defaultSettings, scalaSettings}
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin.publishingSettings
import uk.gov.hmrc.versioning.SbtGitVersioning.autoImport.majorVersion

name := "vat-sign-up"

lazy val root = (project in file("."))
  .enablePlugins(play.sbt.PlayScala, SbtAutoBuildPlugin, SbtGitVersioning, SbtDistributablesPlugin, SbtArtifactory)
  .configs(IntegrationTest)
  .settings(inConfig(IntegrationTest)(Defaults.itSettings): _*)

scalaSettings
publishingSettings
defaultSettings()

libraryDependencies ++= AppDependencies.appDependencies
retrieveManaged := true
evictionWarningOptions in update := EvictionWarningOptions.default.withWarnScalaVersionEviction(false)

dependencyOverrides ++= Set(
  "com.typesafe.akka" %% "akka-actor" % "2.5.23",
  "com.typesafe.akka" %% "akka-protobuf" % "2.5.23",
  "com.typesafe.akka" %% "akka-slf4j" % "2.5.23",
  "com.typesafe.akka" %% "akka-stream" % "2.5.23"
)

Keys.fork in Test := true
javaOptions in Test += "-Dlogger.resource=logback-test.xml"
parallelExecution in Test := true

Keys.fork in IntegrationTest := true
unmanagedSourceDirectories in IntegrationTest := (baseDirectory in IntegrationTest) (base => Seq(base / "it")).value
javaOptions in IntegrationTest += "-Dlogger.resource=logback-test.xml"
addTestReportOption(IntegrationTest, "int-test-reports")
parallelExecution in IntegrationTest := false
majorVersion := 1
scalacOptions += "-Ypartial-unification"
