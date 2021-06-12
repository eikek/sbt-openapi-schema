import com.github.eikek.sbt.openapi._

name := "sbt-openapi-simple-test"
version := "0.0.1"
scalaVersion := "2.12.14"

libraryDependencies ++= Seq(
  "io.circe" %% "circe-core" % "0.11.1",
  "io.circe" %% "circe-generic" % "0.11.1",
  "io.circe" %% "circe-parser" % "0.11.1"
)

openapiSpec := (Compile/resourceDirectory).value/"test.yml"
openapiTargetLanguage := Language.Scala
openapiScalaConfig := ScalaConfig().
  withJson(ScalaJson.circeSemiauto).
  addMapping(CustomMapping.forSource({ case src => src.addImports(Imports("org.app.Codecs._")) })).
  addMapping(CustomMapping.forName({ case s => s + "Dto" }))

enablePlugins(OpenApiSchema)
