ThisBuild / scalaVersion := "2.13.3"

val specs2 = "4.10.6"
val okhttp = "4.9.0"

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
      "scaladoc.stellar.base_url" -> "latest/api/"
    ),
    apidocRootPackage := "stellar.sdk"
  )
  .enablePlugins(ParadoxMaterialThemePlugin).settings(
  paradoxMaterialTheme in Compile ~= {
    _
      .withRepository(url("https://github.com/synesso/scala-stellar-sdk").toURI)
      .withSocial(uri("https://github.com/synesso"), uri("https://keybase.io/jem"))
  }
)
  .enablePlugins(SiteScaladocPlugin)
  .enablePlugins(ParadoxSitePlugin)
  .enablePlugins(GhpagesPlugin).settings(
  scmInfo := Some(ScmInfo(url("https://github.com/synesso/scala-stellar-sdk"), "scm:git:git@github.com:synesso/scala-stellar-sdk.git")),
  git.remoteRepo := scmInfo.value.get.connection.replace("scm:git:", "")
)
  .configs(IntegrationTest)
  .settings(
    name := "scala-stellar-sdk",
    organization := "io.github.synesso",
    homepage := Some(url("https://github.com/synesso/scala-stellar-sdk")),
    startYear := Some(2018),
    description := "Perform Stellar (distributed payments platform) operations from your Scala application. " +
      "Build and submit transactions, query the state of the network and stream updates.",
    developers := List(
      Developer("jem", "Jem Mawson", "jem.mawson@gmail.com", url = url("https://keybase.io/jem"))
    ),
    crossScalaVersions := Seq("2.12.10", "2.13.3"),
    Defaults.itSettings,
    resolvers ++= List(
      Resolver.jcenterRepo,
      "jitpack" at "https://jitpack.io"
    ),
    libraryDependencies ++= List(
      "ch.qos.logback" % "logback-classic" % "1.2.3",
      "com.github.synesso" % "stellar-xdr-jre" % "15.1.0.3",
      "com.softwaremill.retry" %% "retry" % "0.3.3",
      "com.squareup.okhttp3" % "okhttp" % okhttp,
      "com.squareup.okhttp3" % "logging-interceptor" % okhttp,
      "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2",
      "com.squareup.okio" % "okio" % "2.10.0",
      "commons-codec" % "commons-codec" % "1.15",
      "io.github.novacrypto" % "BIP39" % "2019.01.27",
      "net.i2p.crypto" % "eddsa" % "0.3.0",
      "org.json4s" %% "json4s-native" % "3.6.10",
      "org.scala-lang.modules" %% "scala-collection-compat" % "2.4.1",
      "org.typelevel" %% "cats-core" % "2.3.1",
      "tech.sparse" %%  "toml-scala" % "0.2.2",

      "com.github.julien-truffaut" %% "monocle-core"  % "2.0.5" % "test",
      "com.github.julien-truffaut" %% "monocle-macro" % "2.0.5" % "test",
      "com.squareup.okhttp3" % "mockwebserver" % okhttp % "test",
      "org.typelevel" %% "cats-effect" % "2.3.1",
      "org.specs2" %% "specs2-core" % specs2 % "test,it",
      "org.specs2" %% "specs2-mock" % specs2 % "test",
      "org.specs2" %% "specs2-scalacheck" % specs2 % "test"
    ),
    coverageMinimum := 95,
    coverageFailOnMinimum := true,
    coverageExcludedPackages := "\\*DocExamples.scala",
  )