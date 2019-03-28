import sbt._

object Dependencies {

  val `swagger-parser` = "io.swagger.parser.v3" % "swagger-parser" % "2.0.9"

  val slf4j = "org.slf4j" % "slf4j-api" % "1.7.26"

  val `logback-classic` = "ch.qos.logback" % "logback-classic" % "1.2.3"

  // https://github.com/monix/minitest
  // Apache 2.0
  val minitest = "io.monix" %% "minitest" % "2.3.2"
  val `minitest-laws` = "io.monix" %% "minitest-laws" % "2.3.0"

  val `jackson-core` = "com.fasterxml.jackson.core" % "jackson-core"
  val `jackson-annotations` = "com.fasterxml.jackson.core" % "jackson-annotations"
}
