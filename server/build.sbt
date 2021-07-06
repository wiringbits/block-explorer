name := "xsn-block-explorer"
organization := "com.xsn"
scalaVersion := "2.12.10"

fork in Test := true

scalacOptions ++= Seq(
  "-Xfatal-warnings",
  "-unchecked",
  "-deprecation",
  "-feature",
  "-target:jvm-1.8",
  "-encoding",
  "UTF-8",
  "-Xfuture",
  "-Xlint:missing-interpolator",
  "-Yno-adapted-args",
  "-Ywarn-dead-code",
  "-Ywarn-numeric-widen",
  "-Ywarn-value-discard",
  "-Ywarn-unused",
  "-Ywarn-unused-import",
  "-P:silencer:checkUnused"
)

val playsonifyVersion = "2.2.0"

lazy val root = (project in file("."))
  .enablePlugins(PlayScala, JavaAgent)

// Some options are very noisy when using the console and prevent us using it smoothly, let's disable them
val consoleDisabledOptions =
  Seq("-Xfatal-warnings", "-Ywarn-unused", "-Ywarn-unused-import")
scalacOptions in (Compile, console) ~= (_ filterNot consoleDisabledOptions.contains)

// remove play noisy warnings
import play.sbt.routes.RoutesKeys
RoutesKeys.routesImport := Seq.empty

// don't include play generated classes into code coverage
coverageExcludedPackages := "<empty>;Reverse.*;router\\.*"

// silence noisy warnings
libraryDependencies ++= Seq(
  compilerPlugin("com.github.ghik" %% "silencer-plugin" % "1.3.4"),
  "com.github.ghik" %% "silencer-lib" % "1.3.4" % Provided
)

libraryDependencies ++= Seq(guice, evolutions, jdbc, ws)

libraryDependencies ++= Seq(
  "com.alexitc" %% "playsonify-core" % playsonifyVersion,
  "com.alexitc" %% "playsonify-play" % playsonifyVersion,
  "com.alexitc" %% "playsonify-sql" % playsonifyVersion,
  "com.alexitc" %% "playsonify-play-test" % playsonifyVersion % Test
)

libraryDependencies += "com.google.inject" % "guice" % "5.0.1"
libraryDependencies += "org.scalactic" %% "scalactic" % "3.2.9"
libraryDependencies += "org.playframework.anorm" %% "anorm" % "2.6.10"
libraryDependencies += "org.postgresql" % "postgresql" % "42.2.22"

libraryDependencies += "org.slf4j" % "slf4j-api" % "1.7.31"
libraryDependencies += "ch.qos.logback" % "logback-core" % "1.2.3"
libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.3"
libraryDependencies += "io.sentry" % "sentry-logback" % "5.0.1"

libraryDependencies ++= Seq(
  "com.beachape" %% "enumeratum" % "1.7.0"
)

libraryDependencies += "io.scalaland" %% "chimney" % "0.3.5"

libraryDependencies += "com.google.guava" % "guava" % "30.1-jre"

libraryDependencies += "com.sendgrid" % "sendgrid-java" % "4.0.1"

libraryDependencies += "io.kamon" %% "kamon-bundle" % "2.2.2"
libraryDependencies += "io.kamon" %% "kamon-apm-reporter" % "2.2.2"

libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "5.1.0" % Test
libraryDependencies += "org.mockito" %% "mockito-scala" % "1.16.37" % Test
libraryDependencies += "org.mockito" %% "mockito-scala-scalatest" % "1.16.37" % Test

libraryDependencies ++= Seq(
  "com.spotify" % "docker-client" % "8.16.0" % "test",
  "com.whisk" %% "docker-testkit-scalatest" % "0.9.9" % "test",
  "com.whisk" %% "docker-testkit-impl-spotify" % "0.9.9" % "test"
)
