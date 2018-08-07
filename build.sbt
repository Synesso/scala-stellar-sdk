import sbt.Keys.organization
import scoverage.ScoverageKeys.coverageMinimum

// format: off
lazy val commonSettings = Seq(
  name := "scala-stellar-sdk",
  organization := "io.github.synesso",
  scalaVersion := "2.12.6",
  homepage := Some(url("https://github.com/synesso/scala-stellar-sdk")),
  developers := List(
    Developer("jem", "Jem Mawson", "jem@loftinspace.com.au", url = url("https://keybase.io/jem"))
  ),
  crossScalaVersions := Seq("2.12.6"),
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
  coverageMinimum := 98,
  coverageFailOnMinimum := true,
  licenses += ("Apache-2.0", url("https://opensource.org/licenses/Apache-2.0")),
  bintrayRepository := "mvn",
  bintrayPackageLabels := Seq("scala", "stellar"),
  pgpPublicRing := baseDirectory.value / "project" / ".gnupg" / "pubring.gpg",
  pgpSecretRing := baseDirectory.value / "project" / ".gnupg" / "secring.gpg",
  pgpPassphrase := sys.env.get("PGP_PASS").map(_.toArray)
)

resolvers += "scala-stellar-sdk-repo" at "https://dl.bintray.com/synesso/mvn"

lazy val root = (project in file("."))
  .enablePlugins(GitVersioning)
  .enablePlugins(SitePlugin).settings(
    siteSourceDirectory := target.value / "paradox" / "site" / "main"
  )
  .enablePlugins(GhpagesPlugin).settings(
    git.remoteRepo := "git@github.com:synesso/scala-stellar-sdk.git"
  )
  .enablePlugins(ParadoxPlugin).settings(
    paradoxProperties ++= Map(
      "name" -> name.value,
      "organization" -> organization.value,
      "version" -> version.value,
      "scalaBinaryVersion" -> scalaBinaryVersion.value,
      "scaladoc.stellar.base_url" -> "https://synesso.github.io/scala-stellar-sdk/api"
    )
  )
  .enablePlugins(ParadoxMaterialThemePlugin).settings(
    paradoxMaterialTheme in Compile ~= { _
      .withRepository(url("https://github.com/synesso/scala-stellar-sdk").toURI)
      .withSocial(uri("https://github.com/synesso"), uri("https://keybase.io/jem"))
      .withoutSearch()
      // .withGoogleAnalytics() // todo
    }
  ).configs(IntegrationTest)
  .settings(
    commonSettings,
    Defaults.itSettings,
    target in Compile in doc := target.value / "paradox" / "site" / "main" / "api",
    libraryDependencies ++= List(
      "commons-codec" % "commons-codec" % "1.11",
      "net.i2p.crypto" % "eddsa" % "4.0.0",
      "com.softwaremill.sttp" %% "akka-http-backend" % "1.1.5",
      "com.softwaremill.sttp" %% "core" % "1.1.5",
      "com.softwaremill.sttp" %% "json4s" % "1.1.5",
      "com.typesafe.scala-logging" %% "scala-logging" % "3.9.0",
      "ch.qos.logback" % "logback-classic" % "1.2.3",
      "org.specs2" %% "specs2-core" % "4.3.2" % "test,it",
      "org.specs2" %% "specs2-mock" % "4.3.2" % "test",
      "org.specs2" %% "specs2-scalacheck" % "4.3.2" % "test"
    )
  )

