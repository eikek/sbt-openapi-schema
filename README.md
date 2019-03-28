# SBT OpenApi Schema Codegen

This is an sbt plugin to generate Scala or Java code given an openapi
3.x specification. Unlike other codegen tools, this focuses only on
the `#/components/schema` section. Also, it generates immutable
classes and optionally the corresponding JSON conversions.

It generates Scala or Java code:

- Scala: `case class`es are generated and JSON conversion via circe.
- Java: immutable classes (with builder pattern) are generated and
  JSON conversion via jackson (requires jackson > 2.9).
- JSON support is optional.

The scala/java variants are implemented in different plugins. The
implementation is based on the
[swagger-parser](https://github.com/swagger-api/swagger-parser)
project.

It is possible to customize the code generation.

## Usage

Add this plugin to your build in `project/plugins.sbt`:

```
addSbtPlugin("com.github.eikek" % "sbt-openapi-schema" % "0.1.0")
```

Then enable the plugin in some project:

```
enablePlugins(OpenApiSchema)
```

There are two required settings: `openapiSpec` and
`openapiTargetLanguage`. The first defines the openapi.yml file and
the other is either `Language.Java` or `Language.Scala`:

```
project.
  enablePlugins(OpenApiSchema).
  settings(
    openapiTargetLanguage := Language.Scala
    openapiSpec := (Compile/resourceDirectory).value/"test.yml"
  )
```

The sources are automatically generated when you run `compile`. The
task `openapiCodegen` can be used to run the generation independent
from the `compile` task.

## Configuration

The key `openapiJavaConfig` and `openapiScalaConfig` define some
configuration to customize the code generation.

For Java, it looks like this:
```
case class JavaConfig(mapping: CustomMapping = CustomMapping.none
  , json: JavaJson = JavaJson.none
  , builderParents: List[Superclass] = Nil) {

  def withJson(json: JavaJson): JavaConfig =
    copy(json = json)

  def addBuilderParent(sc: Superclass): JavaConfig =
    copy(builderParents = sc :: builderParents)

  def addMapping(cm: CustomMapping): JavaConfig =
    copy(mapping = mapping.andThen(cm))

  def setMapping(cm: CustomMapping): JavaConfig =
    copy(mapping = cm)
}
```

By default, no JSON support is added to the generated classes. This
can be changed via:

```
openapiJavaConfig := JavaConfig.default.copy(json = JavaJson.jackson)
```

This generates the required annotations for jackson. Note, that this
plugin doesn't change your `libraryDependencies` setting. So you need
to add the jackson dependency yourself.

The `CustomMapping` class allows to change the class names or use
different types (for example, you might want to change `LocalDate` to
`Date`).

It looks like this:
```
trait CustomMapping { self =>

  def changeType(td: TypeDef): TypeDef

  def changeSource(src: SourceFile): SourceFile

  def andThen(cm: CustomMapping): CustomMapping = new CustomMapping {
    def changeType(td: TypeDef): TypeDef =
      cm.changeType(self.changeType(td))

    def changeSource(src: SourceFile): SourceFile =
      cm.changeSource(self.changeSource(src))
  }
}
```

It allows to use different types via `changeType` or change the source
file. Here is a `build.sbt` example snippet:

```
import com.github.eikek.sbt.openapi._

libraryDependencies ++= Seq(
  "com.fasterxml.jackson.core" % "jackson-databind" % "2.9.8",
  "com.fasterxml.jackson.datatype" % "jackson-datatype-jsr310" % "2.9.8"
)

openapiSpec := (Compile/resourceDirectory).value/"test.yml"
openapiTargetLanguage := Language.Java
Compile/openapiJavaConfig := JavaConfig.default.
  withJson(JavaJson.jackson).
  addMapping(CustomMapping.forName({ case s => s + "Dto" }))

enablePlugins(OpenApiSchema)
```

It adds jackson JSON support and changes the name of all classes by
appending the suffix "Dto".


## TODOs

- support validation
  - openapi has several validation definitions
  - (java) support bean validation
- convert markdown description into html
- add unit tests
- add more integration tests
  - see [Testing SBT Plugins](https://www.scala-sbt.org/1.x/docs/Testing-sbt-plugins.html)
  - see `plugin/src/sbt-test/*`
