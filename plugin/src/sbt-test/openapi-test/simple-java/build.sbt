name := "sbt-openapi-simple-test"
version := "0.0.1"
scalaVersion := "2.12.8"

enablePlugins(OpenApiSchema)
openapiTargetLanguage := Language.Java
openapiSpec := (Compile/resourceDirectory).value/"test.yml"
