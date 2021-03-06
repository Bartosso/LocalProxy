import sbt._

object Dependencies {

  object Versions {
    val cats       = "2.6.1"
    val catsEffect = "2.5.3"
    val http4s     = "0.21.22"
    val circe      = "0.13.0"
    val enumeratum = "1.7.0"
    val pureConfig = "0.16.0"
    val scalaCache = "0.28.0"
    val tofu       = "0.10.2"

    val kindProjector  = "0.13.2"
    val logback        = "1.2.3"
    val scalaCheck     = "1.15.4"
    val scalaTest      = "3.2.9"
    val catsScalaCheck = "0.3.0"
    val wiremock       = "2.27.2"
    val testContainers = "0.39.7"
  }

  object Libraries {
    def circe(artifact: String): ModuleID  = "io.circe"   %% artifact % Versions.circe
    def http4s(artifact: String): ModuleID = "org.http4s" %% artifact % Versions.http4s

    lazy val cats       = "org.typelevel" %% "cats-core"   % Versions.cats
    lazy val catsEffect = "org.typelevel" %% "cats-effect" % Versions.catsEffect

    lazy val http4sDsl          = http4s("http4s-dsl")
    lazy val http4sServer       = http4s("http4s-blaze-server")
    lazy val http4sClient       = http4s("http4s-blaze-client")
    lazy val http4sCirce        = http4s("http4s-circe")
    lazy val circeCore          = circe("circe-core")
    lazy val circeGeneric       = circe("circe-generic")
    lazy val circeGenericExt    = circe("circe-generic-extras")
    lazy val circeParser        = circe("circe-parser")
    lazy val enumeratum         = "com.beachape" %% "enumeratum" % Versions.enumeratum
    lazy val enumeratumCirce    = "com.beachape" %% "enumeratum-circe" % Versions.enumeratum
    lazy val pureConfig         = "com.github.pureconfig" %% "pureconfig" % Versions.pureConfig
    lazy val pureConfigCats     = "com.github.pureconfig" %% "pureconfig-cats-effect2" % "0.16.0"
    lazy val scalaCache         = "com.github.cb372" %% "scalacache-core" % Versions.scalaCache
    lazy val scalaCacheCE       = "com.github.cb372" %% "scalacache-cats-effect" % Versions.scalaCache
    lazy val coffeineCache      = "com.github.cb372" %% "scalacache-caffeine" % Versions.scalaCache
    lazy val rediseCache        = "com.github.cb372" %% "scalacache-redis" % Versions.scalaCache
    lazy val cacheCirce         = "com.github.cb372" %% "scalacache-circe" % Versions.scalaCache
    lazy val tofuLogs           = "tf.tofu" %% "tofu-logging" % Versions.tofu
    lazy val tofuLogsDerivation = "tf.tofu" %% "tofu-logging-derivation" % Versions.tofu

    // Compiler plugins
    lazy val kindProjector = "org.typelevel" %% "kind-projector" % Versions.kindProjector cross CrossVersion.full

    // Runtime
    lazy val logback = "ch.qos.logback" % "logback-classic" % Versions.logback

    // Test
    lazy val scalaTest      = "org.scalatest"          %% "scalatest"                      % Versions.scalaTest
    lazy val scalaCheck     = "org.scalacheck"         %% "scalacheck"                     % Versions.scalaCheck
    lazy val catsScalaCheck = "io.chrisdavenport"      %% "cats-scalacheck"                % Versions.catsScalaCheck
    lazy val wiremock       = "com.github.tomakehurst" % "wiremock"                        % Versions.wiremock
    lazy val testContainers = "com.dimafeng"           %% "testcontainers-scala-scalatest" % Versions.testContainers
  }

}
