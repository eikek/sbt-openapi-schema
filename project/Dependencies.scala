import sbt._

object Dependencies {
  object V {
    val munitVersion = "1.3.6"
    val munitScalacheckVersion = "1.3.1"
    val munitCatsEffectVersion = "2.2.1"
    val swaggerParser = "2.1.48"
    val swaggerCodegen = "3.0.85"
  }

  val munit = Seq(
    "org.scalameta" %% "munit" % V.munitVersion,
    "org.scalameta" %% "munit-scalacheck" % V.munitScalacheckVersion,
    "org.typelevel" %% "munit-cats-effect" % V.munitCatsEffectVersion
  )

  val `swagger-parser` = "io.swagger.parser.v3" % "swagger-parser" % V.swaggerParser

  val swaggerCodegen = "io.swagger.codegen.v3" % "swagger-codegen-cli" % V.swaggerCodegen

}
