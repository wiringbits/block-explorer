addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.6.13")

addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.5.1")

addSbtPlugin("com.codacy" % "sbt-codacy-coverage" % "1.3.12")

addSbtPlugin("org.duhemm" % "sbt-errors-summary" % "0.6.0")

resolvers += Resolver.bintrayIvyRepo("kamon-io", "sbt-plugins")
addSbtPlugin("io.kamon" % "sbt-aspectj-runner-play-2.6" % "1.1.0")
