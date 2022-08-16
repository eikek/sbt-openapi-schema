import sbt._

object Dependencies {

  val `swagger-parser` = "io.swagger.parser.v3" % "swagger-parser" % "2.1.2"

  val swaggerCodegen = "io.swagger.codegen.v3" % "swagger-codegen-cli" % "3.0.35"

  val slf4j = "org.slf4j" % "slf4j-api" % "1.7.26"

  val `logback-classic` = "ch.qos.logback" % "logback-classic" % "1.2.11"

  // https://github.com/monix/minitest
  // Apache 2.0
  val minitest = "io.monix" %% "minitest" % "2.9.6"
  val `minitest-laws` = "io.monix" %% "minitest-laws" % "2.7.0"

}
