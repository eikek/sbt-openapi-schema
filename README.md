# SBT OpenApi Schema Codegen

[![Build Status](https://img.shields.io/travis/eikek/sbt-openapi-schema/master?style=flat-square)](https://travis-ci.org/eikek/sbt-openapi-schema)
[![Scala Steward badge](https://img.shields.io/badge/Scala_Steward-helping-blue.svg?style=flat-square&logo=data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAA4AAAAQCAMAAAARSr4IAAAAVFBMVEUAAACHjojlOy5NWlrKzcYRKjGFjIbp293YycuLa3pYY2LSqql4f3pCUFTgSjNodYRmcXUsPD/NTTbjRS+2jomhgnzNc223cGvZS0HaSD0XLjbaSjElhIr+AAAAAXRSTlMAQObYZgAAAHlJREFUCNdNyosOwyAIhWHAQS1Vt7a77/3fcxxdmv0xwmckutAR1nkm4ggbyEcg/wWmlGLDAA3oL50xi6fk5ffZ3E2E3QfZDCcCN2YtbEWZt+Drc6u6rlqv7Uk0LdKqqr5rk2UCRXOk0vmQKGfc94nOJyQjouF9H/wCc9gECEYfONoAAAAASUVORK5CYII=)](https://scala-steward.org)
[![License](https://img.shields.io/github/license/eikek/sbt-openapi-schema.svg?style=flat-square&color=steelblue)](https://github.com/eikek/sbt-openapi-schema/blob/master/LICENSE.txt)


This is an sbt plugin to generate Scala, Java or Elm code given an
openapi 3.x specification. Unlike other codegen tools, this focuses
only on the `#/components/schema` section. Also, it generates
immutable classes and optionally the corresponding JSON conversions.

- Scala: `case class`es are generated and JSON conversion via circe.
- Java: immutable classes (with builder pattern) are generated and
  JSON conversion via jackson (requires jackson > 2.9).
- Elm: (experimental) records are generated and constructors for
  "empty" values. It works only for objects. JSON conversion is
  generated using Elm's default encoding support and the
  [json-decode-pipeline](https://github.com/NoRedInk/elm-json-decode-pipeline)
  module for decoding.
- JSON support is optional.

The implementation is based on the
[swagger-parser](https://github.com/swagger-api/swagger-parser)
project.

It is possible to customize the code generation.

## Usage

Add this plugin to your build in `project/plugins.sbt`:

```
addSbtPlugin("com.github.eikek" % "sbt-openapi-schema" % "0.5.0")
```

Please check the git tags or maven central for the current version.
Then enable the plugin in some project:

```
enablePlugins(OpenApiSchema)
```

There are two required settings: `openapiSpec` and
`openapiTargetLanguage`. The first defines the openapi.yml file and
the other is a constant from the `Language` object:

```scala
import com.github.eikek.sbt.openapi._

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

The configuration is specific to the target language. There exists a
separate configuration object for Java, Scala and Elm.

The key `openapiJavaConfig` and `openapiScalaConfig` define some
configuration to customize the code generation.

For Java, it looks like this:
```scala
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
openapiJavaConfig := JavaConfig.default.withJson(JavaJson.jackson)
```

This generates the required annotations for jackson. Note, that this
plugin doesn't change your `libraryDependencies` setting. So you need
to add the jackson dependency yourself.

The `CustomMapping` class allows to change the class names or use
different types (for example, you might want to change `LocalDate` to
`Date`).

It looks like this:
```scala
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

There are convenient constructors in its companion object.

It allows to use different types via `changeType` or change the source
file. Here is a `build.sbt` example snippet:

```scala
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

The default output path for elm sources is `target/elm-src`. So in
your `elm.json` file, add this directory to the `source-directories`
list along with the main source dir. It may look something like this:

```json
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

While source files for scala and java are added to sbt's
`sourceGenerators` so that they get compiled with your sources, the
elm source files are not added anywhere, because there is no support
for Elm in sbt. However, in the `build.sbt` file, you can tell sbt to
generate the files before compiling your elm app. This can be
configured to run during resource generation. Example:

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
      openapiCodegen.value // generate api model files
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

## Discriminator Support

> Thanks to @mhertogs, who contributed this feature.

OpenAPI 3.0 enables to introduce subtyping on generated schemas by using [discriminators](https://github.com/OAI/OpenAPI-Specification/blob/master/versions/3.0.0.md#discriminatorObject).

Two of these are currently supported only in Scala : `oneOf` and `allOf`.   

#### Setup

In order to provide JSON conversion for these discriminators with Circe, we need to make use of [circe-generic-extras](https://github.com/circe/circe-generic-extras)

An example build.sbt using the plugin would look like the following:
```scala
import com.github.eikek.sbt.openapi._

libraryDependencies ++= Seq(
  "io.circe" %% "circe-generic-extras" % "0.11.1",
  "io.circe" %% "circe-core" % "0.11.1",
  "io.circe" %% "circe-generic" % "0.11.1",
  "io.circe" %% "circe-parser" % "0.11.1"
)

openapiSpec := (Compile/resourceDirectory).value/"test.yml"
openapiTargetLanguage := Language.Scala
openapiScalaConfig := ScalaConfig().
  withJson(ScalaJson.circeSemiautoExtra).
  addMapping(CustomMapping.forName({ case s => s + "Dto" }))

enablePlugins(OpenApiSchema)
```


#### Handle `allOf` keywords in Scala

Here is an example OpenAPI spec and the resulting Scala models with JSON conversions

```yaml
components:
  schemas:
    Pet:
      type: object
      discriminator:
        propertyName: petType
      properties:
        name:
          type: string
        petType:
          type: string
      required:
      - name
      - petType
    Cat:  ## "Cat" will be used as the discriminator value
      description: A representation of a cat
      allOf:
      - $ref: '#/components/schemas/Pet'
      - type: object
        properties:
          huntingSkill:
            type: string
            description: The measured skill for hunting
        required:
        - huntingSkill
    Dog:  ## "Dog" will be used as the discriminator value
      description: A representation of a dog
      allOf:
      - $ref: '#/components/schemas/Pet'
      - type: object
        properties:
          packSize:
            type: integer
            format: int32
            description: the size of the pack the dog is from
        required:
        - packSize
```

```scala
import io.circe._
import io.circe.generic.extras.semiauto._
import io.circe.generic.extras.Configuration

sealed trait PetDto {
  val name: String
}
object PetDto {
  implicit val customConfig: Configuration = Configuration.default.withDefaults.withDiscriminator("petType")

  case class Cat (
    huntingSkill: String, name: String
  ) extends PetDto

  case class Dog (
    packSize: Int, name: String
  ) extends PetDto

  object Cat {
    implicit val customConfig: Configuration = Configuration.default.withDefaults.withDiscriminator("petType")
    private implicit val jsonDecoder: Decoder[Cat] = deriveDecoder[Cat]
    private implicit val jsonEncoder: Encoder[Cat] = deriveEncoder[Cat]
  }

  object Dog {
    implicit val customConfig: Configuration = Configuration.default.withDefaults.withDiscriminator("petType")
    private implicit val jsonDecoder: Decoder[Dog] = deriveDecoder[Dog]
    private implicit val jsonEncoder: Encoder[Dog] = deriveEncoder[Dog]
  }

  implicit val jsonDecoder: Decoder[PetDto] = deriveDecoder[PetDto]
  implicit val jsonEncoder: Encoder[PetDto] = deriveEncoder[PetDto]
}

```

Notes about the above example:
- The internal schemas ("Dog" and "Cat") have private encoder/decoders so that they are only encoded and decoded as the trait interface. If you try to decode as a Dog or Cat type, the circe-generic-extras doesn't include the discriminant type
- The mapping functionality (adding "Dto") is only used on the sealed trait since the discriminant type uses the name of the inner case classes ("Dog" and "Cat").

#### Handle `oneOf` keywords in Scala

Another way of transform composed schemas into `sealed trait` hierarchies is to use `oneOf`.

```yaml 
Pet:
  type: object
  discriminator:
    propertyName: petType
  oneOf:
    - $ref: '#/components/schemas/Cat'
    - $ref: '#/components/schemas/Dog'
Cat:  ## "Cat" will be used as the discriminator value
  description: A representation of a cat
  properties:
    huntingSkill:
      type: string
      description: The measured skill for hunting
      enum:
        - clueless
        - lazy
        - adventurous
        - aggressive
    name:
      type: string
    petType:
      type: string
  required:
    - huntingSkill
    - name
    - petType
Dog:  ## "Dog" will be used as the discriminator value
  description: A representation of a dog
  properties:
    packSize:
      type: integer
      format: int32
      description: the size of the pack the dog is from
      default: 0
      minimum: 0
    name:
      type: string
    petType:
      type: string
  required:
    - packSize
    - name
    - petType
```

```scala
import io.circe._
import io.circe.generic.extras.semiauto._
import io.circe.generic.extras.Configuration

sealed trait PetDto {
} 
object PetDto {
  implicit val customConfig: Configuration = Configuration.default.withDefaults.withDiscriminator("petType")
  case class Cat (
    huntingSkill: String, name: String, petType: String
  ) extends PetDto
  case class Dog (
    packSize: Int, name: String, petType: String
  ) extends PetDto
  object Cat {
    implicit val customConfig: Configuration = Configuration.default.withDefaults.withDiscriminator("petType")
    private implicit val jsonDecoder: Decoder[Cat] = deriveDecoder[Cat]
    private implicit val jsonEncoder: Encoder[Cat] = deriveEncoder[Cat]
  }
  object Dog {
    implicit val customConfig: Configuration = Configuration.default.withDefaults.withDiscriminator("petType")
    private implicit val jsonDecoder: Decoder[Dog] = deriveDecoder[Dog]
    private implicit val jsonEncoder: Encoder[Dog] = deriveEncoder[Dog]
  }
  implicit val jsonDecoder: Decoder[PetDto] = deriveDecoder[PetDto]
  implicit val jsonEncoder: Encoder[PetDto] = deriveEncoder[PetDto]
} 
```

Unlike `allOf`, `oneOf` doesn't permit subschemas to inherit fields from their parent. This kind of relation fits well to algebraic data types encodings in Scala.

## TODOs

- support validation
  - openapi has several validation definitions
  - (java) support bean validation
- convert markdown description into html
- add unit tests
- add more integration tests
  - see [Testing SBT Plugins](https://www.scala-sbt.org/1.x/docs/Testing-sbt-plugins.html)
  - see `plugin/src/sbt-test/*`
