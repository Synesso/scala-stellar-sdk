ThisBuild / scalaVersion := "2.13.1"

lazy val root = project
  .in(file("."))
  .enablePlugins(GitVersioning)
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
  )
  .configs(IntegrationTest)
  .settings(
    name := "scala-stellar-sdk",
    organization := "io.github.synesso",
    Defaults.itSettings,
    resolvers += Resolver.jcenterRepo,
    libraryDependencies ++= List(
      "ch.qos.logback" % "logback-classic" % "1.2.3",
      "com.softwaremill.retry" %% "retry" % "0.3.3",
      "com.squareup.okhttp3" % "okhttp" % "4.3.1",
      "com.squareup.okhttp3" % "logging-interceptor" % "4.3.1",
      "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2",
      "com.squareup.okio" % "okio" % "2.4.3",
      "commons-codec" % "commons-codec" % "1.14",
      "io.github.novacrypto" % "BIP39" % "2019.01.27",
      "net.i2p.crypto" % "eddsa" % "0.3.0",
      "org.json4s" %% "json4s-native" % "3.6.7",
      "org.scala-lang.modules" %% "scala-collection-compat" % "2.1.4",
      "org.typelevel" %% "cats-core" % "2.1.0",
      "tech.sparse" %%  "toml-scala" % "0.2.2",

      "com.squareup.okhttp3" % "mockwebserver" % "4.3.1" % "test",
      "org.specs2" %% "specs2-core" % "4.9.2" % "test,it",
      "org.specs2" %% "specs2-mock" % "4.9.2" % "test",
      "org.specs2" %% "specs2-scalacheck" % "4.9.2" % "test"
    ),
    coverageMinimum := 95,
    coverageFailOnMinimum := true,
    coverageExcludedPackages := "\\*DocExamples.scala",
  )