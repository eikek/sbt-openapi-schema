import sbt._

object Dependencies {
  object V {
    val munitVersion = "1.1.0"
    val munitCatsEffectVersion = "2.1.0"
    val swaggerParser = "2.1.29"
    val swaggerCodegen = "3.0.75"
  }

  val munit = Seq(
    "org.scalameta" %% "munit" % V.munitVersion,
    "org.scalameta" %% "munit-scalacheck" % V.munitVersion,
    "org.typelevel" %% "munit-cats-effect" % V.munitCatsEffectVersion
  )

  val `swagger-parser` = "io.swagger.parser.v3" % "swagger-parser" % V.swaggerParser

  val swaggerCodegen = "io.swagger.codegen.v3" % "swagger-codegen-cli" % V.swaggerCodegen

}
