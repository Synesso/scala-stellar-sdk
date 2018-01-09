
// format: off
name := "scala-stellar-sdk"

organization := "stellar.scala.sdk"

version := sys.env.getOrElse("TRAVIS_BRANCH", "dev")

scalaVersion := "2.12.4"

crossScalaVersions := Seq("2.11.12", "2.12.4")

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

coverageMinimum := 90
coverageFailOnMinimum := true
