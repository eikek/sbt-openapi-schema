import sbt._

object Dependencies {
  object V {
    val munitVersion = "0.7.29"
    val munitCatsEffectVersion = "1.0.7"
    val swaggerParser = "2.1.20"
    val swaggerCodegen = "3.0.53"
  }

  val munit = Seq(
    "org.scalameta" %% "munit" % V.munitVersion,
    "org.scalameta" %% "munit-scalacheck" % V.munitVersion,
    "org.typelevel" %% "munit-cats-effect-3" % V.munitCatsEffectVersion
  )

  val `swagger-parser` = "io.swagger.parser.v3" % "swagger-parser" % V.swaggerParser

  val swaggerCodegen = "io.swagger.codegen.v3" % "swagger-codegen-cli" % V.swaggerCodegen

}
