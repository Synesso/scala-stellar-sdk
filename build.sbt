
// format: off
name := "scala-stellar-sdk"

organization := "stellar.scala.sdk"

scalaVersion := "2.12.4"

crossScalaVersions := Seq("2.11.12", "2.12.4")

enablePlugins(GitVersioning)

libraryDependencies ++= List(
  "commons-codec" % "commons-codec" % "1.11",
  "org.specs2" %% "specs2-core" % "4.0.0" % "test",
  "org.specs2" %% "specs2-scalacheck" % "4.0.0" % "test"
)

scalacOptions ++= Seq(
  "-Yrangepos",
  "-unchecked",
  "-deprecation",
  "-feature",
  "-language:postfixOps",
  "-encoding",
  "UTF-8",
  "-target:jvm-1.8"
)

fork := true

coverageMinimum := 95
coverageFailOnMinimum := true

licenses += ("Apache-2.0", url("https://opensource.org/licenses/Apache-2.0"))

bintrayRepository := "mvn"

bintrayPackageLabels := Seq("scala", "stellar")
