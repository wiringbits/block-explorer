name := "xsn-block-explorer"
organization := "com.xsn"
scalaVersion := "2.12.2"

fork in Test := true

scalacOptions ++= Seq(
//  "-Xfatal-warnings",
  "-unchecked",
  "-deprecation",
  "-feature",
  "-target:jvm-1.8",
  "-encoding", "UTF-8",
  "-Xfuture",
  "-Xlint:missing-interpolator",
  "-Yno-adapted-args",
  "-Ywarn-dead-code",
  "-Ywarn-numeric-widen",
  "-Ywarn-value-discard",
  "-Ywarn-unused",
  "-Ywarn-unused-import"
)


val playsonifyVersion = "1.2.0"

lazy val root = (project in file("."))
    .enablePlugins(PlayScala)

// don't include play generated classes into code coverage
coverageExcludedPackages := "<empty>;Reverse.*;router\\.*"

libraryDependencies ++= Seq(guice, evolutions, jdbc, ws)

libraryDependencies += "com.alexitc" %% "playsonify" % playsonifyVersion
libraryDependencies += "com.google.inject" % "guice" % "4.1.0"
libraryDependencies += "org.scalactic" %% "scalactic" % "3.0.4"
libraryDependencies += "org.playframework.anorm" %% "anorm" % "2.6.1"
libraryDependencies += "org.postgresql" % "postgresql" % "9.4.1212"

libraryDependencies += "org.slf4j" % "slf4j-api" % "1.7.25"
libraryDependencies += "ch.qos.logback" % "logback-core" % "1.2.3"
libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.3"
libraryDependencies += "io.sentry" % "sentry-logback" % "1.7.13"

libraryDependencies ++= Seq(
  "com.beachape" %% "enumeratum" % "1.5.13"
)

libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.0" % Test
libraryDependencies += "com.alexitc" %% "playsonifytest" % playsonifyVersion % Test
libraryDependencies += "org.mockito" % "mockito-core" % "2.15.0" % Test

libraryDependencies ++= Seq(
  "com.spotify" % "docker-client" % "8.9.1",
  "com.whisk" %% "docker-testkit-scalatest" % "0.9.5" % "test",
  "com.whisk" %% "docker-testkit-impl-spotify" % "0.9.5" % "test")
