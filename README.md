# SBT OpenApi Schema Codegen

This is an sbt plugin to generate Scala, Java or Elm code given an
openapi 3.x specification. Unlike other codegen tools, this focuses
only on the `#/components/schema` section. Also, it generates
immutable classes (scala and java) and optionally the corresponding
JSON conversions.

It generates Scala, Java or Elm code:

- Scala: `case class`es are generated and JSON conversion via circe.
- Java: immutable classes (with builder pattern) are generated and
  JSON conversion via jackson (requires jackson > 2.9).
- Elm: (experimental) type aliases are generated and constructors for
  "empty" values. It works only for objects. JSON conversion is
  generated using Elm's default encoding support and the
  [json-decode-pipeline](https://github.com/NoRedInk/elm-json-decode-pipeline)
  module for decoding.
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


## Elm

There is some experimental support for generating Elm data structures
and corresponding JSON conversion functions. When using the
`decodePipeline` json variant, you need to install these packages:

```
elm install elm/json
elm install NoRedInk/elm-json-decode-pipeline
```

While source files for scala and java are added to sbt's
`sourceGenerators` so that they get compiled with your sources, the
elm source files are added to the `resourceGenerators` list. The
default output path for elm sources is `target/elm-src`. So in your
`elm.json` file, add this directory to the `source-directories` list
along with the main source dir. It may look something like this:

```
{
    "type": "application",
    "source-directories": [
        "modules/webapp/target/elm-src",
        "modules/webapp/src/main/elm"
    ],
    "elm-version": "0.19.0",
    "dependencies": {
        "direct": {
            "NoRedInk/elm-json-decode-pipeline": "1.0.0",
            "elm/browser": "1.0.1",
            "elm/core": "1.0.2",
            "elm/html": "1.0.0",
            "elm/json": "1.1.3"
        },
        "indirect": {
            "elm/time": "1.0.0",
            "elm/url": "1.0.0",
            "elm/virtual-dom": "1.0.2"
        }
    },
    "test-dependencies": {
        "direct": {},
        "indirect": {}
    }
}
```

It always generates type aliases for records.

In the `build.sbt` file, it is then easy to compile all elm files
during resource generation. Example:

``` scala

// Put resulting js file into the webjar location
def compileElm(logger: Logger, wd: File, outBase: File, artifact: String, version: String): Seq[File] = {
  logger.info("Compile elm files ...")
  val target = outBase/"META-INF"/"resources"/"webjars"/artifact/version/"my-app.js"
  val proc = Process(Seq("elm", "make", "--output", target.toString) ++ Seq(wd/"src"/"main"/"elm"/"Main.elm").map(_.toString), Some(wd))
  val out = proc.!!
  logger.info(out)
  Seq(target)
}

val webapp = project.in(file("webapp")).
  enablePlugins(OpenApiSchema).
  settings(
    openapiTargetLanguage := Language.Elm,
    openapiPackage := Pkg("Api.Model"),
    openapiSpec := (Compile/resourceDirectory).value/"openapi.yml",
    openapiElmConfig := ElmConfig().withJson(ElmJson.decodePipeline),
    Compile/resourceGenerators += (Def.task {
      compileElm(streams.value.log
        , (Compile/baseDirectory).value
        , (Compile/resourceManaged).value
        , name.value
        , version.value)
    }).taskValue,
    watchSources += Watched.WatchSource(
      (Compile/sourceDirectory).value/"elm"
        , FileFilter.globFilter("*.elm")
        , HiddenFileFilter
    )
  )
```

This example assumes a `elm.json` project file in the source root.

## TODOs

- support validation
  - openapi has several validation definitions
  - (java) support bean validation
- convert markdown description into html
- add unit tests
- add more integration tests
  - see [Testing SBT Plugins](https://www.scala-sbt.org/1.x/docs/Testing-sbt-plugins.html)
  - see `plugin/src/sbt-test/*`
