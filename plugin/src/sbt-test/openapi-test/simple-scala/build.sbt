import com.github.eikek.sbt.openapi._

name := "sbt-openapi-simple-scala-test"
version := "0.0.1"
scalaVersion := "2.13.5"

enablePlugins(OpenApiSchema)
openapiTargetLanguage := Language.Scala
openapiSpec := (Compile/resourceDirectory).value/"test.yml"
openapiScalaConfig := ScalaConfig.default.
  addMapping(CustomMapping.forType({
    case TypeDef("LocalDate", _)  =>
      TypeDef("Instant", Imports("java.time.Instant"))
  }))
