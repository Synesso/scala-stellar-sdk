import sbt.Keys.organization
import scoverage.ScoverageKeys.coverageMinimum

// format: off
lazy val commonSettings = Seq(
  name := "scala-stellar-sdk",
  organization := "stellar.scala.sdk",
  scalaVersion := "2.12.4",
  crossScalaVersions := Seq("2.11.12", "2.12.4"),
  scalacOptions ++= Seq(
    "-Yrangepos",
    "-unchecked",
    "-deprecation",
    "-feature",
    "-language:postfixOps",
    "-encoding",
    "UTF-8",
    "-target:jvm-1.8"),
  fork := true,
  coverageMinimum := 95,
  coverageFailOnMinimum := true,
  licenses += ("Apache-2.0", url("https://opensource.org/licenses/Apache-2.0")),
  bintrayRepository := "mvn",
  bintrayPackageLabels := Seq("scala", "stellar")
)

resolvers += "scala-stellar-sdk-repo" at "https://dl.bintray.com/synesso/mvn"

enablePlugins(GitVersioning)

lazy val root = (project in file("."))
  .configs(IntegrationTest)
  .settings(
    commonSettings,
    Defaults.itSettings,
    libraryDependencies ++= List(
      "commons-codec" % "commons-codec" % "1.11",
      "net.i2p.crypto" % "eddsa" % "4.0.0",
      "com.softwaremill.sttp" %% "akka-http-backend" % "1.1.5",
      "com.softwaremill.sttp" %% "core" % "1.1.5",
      "com.softwaremill.sttp" %% "json4s" % "1.1.5",
      "org.specs2" %% "specs2-core" % "4.0.0" % "test,it",
      "org.specs2" %% "specs2-scalacheck" % "4.0.0" % "test"
    )
  )
