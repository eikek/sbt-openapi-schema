name := "sbt-openapi-simple-scala-test"
version := "0.0.1"
scalaVersion := "2.12.8"

enablePlugins(OpenApiSchema)
openapiTargetLanguage := Language.Scala
openapiSpec := (Compile/resourceDirectory).value/"test.yml"
