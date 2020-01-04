import sbt.Keys.organization
import scoverage.ScoverageKeys.coverageMinimum

// format: off
lazy val commonSettings = Seq(
  name := "scala-stellar-sdk",
  organization := "io.github.synesso",
  scalaVersion := "2.12.9",
  homepage := Some(url("https://github.com/synesso/scala-stellar-sdk")),
  startYear := Some(2018),
  description := "Perform Stellar (distributed payments platform) operations from your Scala application. " +
    "Build and submit transactions, query the state of the network and stream updates.",
  developers := List(
    Developer("jem", "Jem Mawson", "jem.mawson@gmail.com", url = url("https://keybase.io/jem"))
  ),
  crossScalaVersions := Seq("2.12.9"),
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
  coverageExcludedPackages := "\\*DocExamples.scala",
  licenses += ("Apache-2.0", url("https://opensource.org/licenses/Apache-2.0")),
)

lazy val root = (project in file("."))
  .enablePlugins(GitVersioning)
  .enablePlugins(SitePlugin).settings(
    siteSourceDirectory := target.value / "paradox" / "site" / "main"
  )
  .enablePlugins(GhpagesPlugin).settings(
    git.remoteRepo := "git@github.com:synesso/scala-stellar-sdk.git"
  )
  .enablePlugins(BuildInfoPlugin).settings(
    buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion),
    buildInfoPackage := "stellar.sdk"
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
  paradoxMaterialTheme in Compile ~= {
    _
      .withRepository(url("https://github.com/synesso/scala-stellar-sdk").toURI)
      .withSocial(uri("https://github.com/synesso"), uri("https://keybase.io/jem"))
      .withoutSearch()
  }
).configs(IntegrationTest)
  .settings(
    commonSettings,
    compile := ((compile in Compile) dependsOn (paradox in Compile)).value,
    test := ((test in Test) dependsOn dependencyUpdates).value,
    dependencyUpdatesFailBuild := false,
    Defaults.itSettings,
    target in Compile in doc := target.value / "paradox" / "site" / "main" / "api",
    publishLocalConfiguration := publishLocalConfiguration.value.withOverwrite(true),
    resolvers += Resolver.jcenterRepo,
    libraryDependencies ++= List(
      "ch.qos.logback" % "logback-classic" % "1.2.3",
      "com.softwaremill.retry" %% "retry" % "0.3.3",
      "com.squareup.okhttp3" % "okhttp" % "4.3.0",
      "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2",
      "com.squareup.okio" % "okio" % "2.4.3",
      "commons-codec" % "commons-codec" % "1.14",
      "io.github.novacrypto" % "BIP39" % "2019.01.27",
      "net.i2p.crypto" % "eddsa" % "0.3.0",
      "org.json4s" %% "json4s-native" % "3.6.7",
      "org.typelevel" %% "cats-core" % "2.1.0",
      "tech.sparse" %%  "toml-scala" % "0.2.2",

      "com.squareup.okhttp3" % "mockwebserver" % "4.3.0" % "test",
      "org.specs2" %% "specs2-core" % "4.7.1" % "test,it",
      "org.specs2" %% "specs2-mock" % "4.7.1" % "test",
      "org.specs2" %% "specs2-scalacheck" % "4.7.1" % "test"
    )
  )

