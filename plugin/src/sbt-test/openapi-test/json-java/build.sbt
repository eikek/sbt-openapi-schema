import com.github.eikek.sbt.openapi._

name := "sbt-openapi-simple-test"
version := "0.0.1"
scalaVersion := "2.12.8"

libraryDependencies ++= Seq(
  "com.fasterxml.jackson.core" % "jackson-databind" % "2.9.8",
  "com.fasterxml.jackson.datatype" % "jackson-datatype-jsr310" % "2.9.8"
)

openapiSpec := (Compile/resourceDirectory).value/"test.yml"
openapiTargetLanguage := Language.Java
openapiJavaConfig := JavaConfig.default.
  withJson(JavaJson.jackson).
  addMapping(CustomMapping.forName({ case s => s + "Dto" }))

enablePlugins(OpenApiSchema)
