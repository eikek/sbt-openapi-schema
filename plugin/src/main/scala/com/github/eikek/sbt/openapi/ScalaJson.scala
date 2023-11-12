package com.github.eikek.sbt.openapi

import com.github.eikek.sbt.openapi.PartConv._

trait ScalaJson {

  def companion: PartConv[SourceFile]

  def resolve(src: SourceFile): SourceFile

}

object ScalaJson {
  val none = new ScalaJson {
    def companion = PartConv(_ => Part.empty)
    def resolve(src: SourceFile): SourceFile = src
  }

  private def replaceJsonType(src: SourceFile): SourceFile = {
    val circeJson = TypeDef("io.circe.Json", Imports.empty)
    def isJson(f: Field) = f.typeDef.name.equalsIgnoreCase("json")

    src.copy(fields =
      src.fields.map(f => if (isJson(f)) f.copy(typeDef = circeJson) else f)
    )
  }

  val circeSemiauto = new ScalaJson {
    val props: PartConv[SourceFile] =
      constant("object") ~ sourceName ~ constant("{") ++
        constant("implicit val jsonDecoder: io.circe.Decoder[").map(
          _.indent(2)
        ) + sourceName + constant(
          "] = io.circe.generic.semiauto.deriveDecoder["
        ) + sourceName + constant("]") ++
        constant("implicit val jsonEncoder: io.circe.Encoder[").map(
          _.indent(2)
        ) + sourceName + constant(
          "] = io.circe.generic.semiauto.deriveEncoder["
        ) + sourceName + constant("]") ++
        constant("}")

    val wrapper: PartConv[SourceFile] =
      constant("object") ~ sourceName ~ constant("{") ++
        constant("implicit def jsonDecoder(implicit vd: io.circe.Decoder[").map(
          _.indent(2)
        ) +
        fieldType.contramap[SourceFile](_.fields.head) + constant(
          "]): io.circe.Decoder["
        ) + sourceName + constant("] =") ++
        constant("vd.map(").map(_.indent(4)) + sourceName + constant(".apply)") ++
        constant("implicit def jsonEncoder(implicit ve: io.circe.Encoder[").map(
          _.indent(2)
        ) +
        fieldType.contramap[SourceFile](_.fields.head) + constant(
          "]): io.circe.Encoder["
        ) + sourceName + constant("] =") ++
        constant("ve.contramap(_.value)").map(_.indent(4)) ++
        constant("}")

    def companion =
      cond(_.wrapper, wrapper, props)

    def resolve(src: SourceFile): SourceFile =
      src.modify(replaceJsonType)
  }

  val circeSemiautoExtra = new ScalaJson {

    val discriminantProps: PartConv[SourceFile] =
      constant("implicit val jsonDecoder: io.circe.Decoder[") + sourceName + constant(
        "] = io.circe.generic.extras.semiauto.deriveDecoder["
      ) + sourceName + constant("]") ++
        constant("implicit val jsonEncoder: io.circe.Encoder[") + sourceName + constant(
          "] = io.circe.generic.extras.semiauto.deriveEncoder["
        ) + sourceName + constant("]")

    val props: PartConv[SourceFile] =
      constant("object") ~ sourceName ~ constant("{") ++
        constant(
          "implicit val customConfig: io.circe.generic.extras.Configuration = io.circe.generic.extras.Configuration.default.withDefaults.withDiscriminator(\""
        ).map(_.indent(2)) + discriminantType + constant("\")") ++
        (accessModifier + constant("implicit val jsonDecoder: io.circe.Decoder[")).map(
          _.indent(2)
        ) + sourceName + constant(
          "] = io.circe.generic.extras.semiauto.deriveDecoder["
        ) + sourceName + constant("]") ++
        (accessModifier + constant("implicit val jsonEncoder: io.circe.Encoder[")).map(
          _.indent(2)
        ) + sourceName + constant(
          "] = io.circe.generic.extras.semiauto.deriveEncoder["
        ) + sourceName + constant("]") ++
        constant("}")

    val wrapper: PartConv[SourceFile] =
      constant("object") ~ sourceName ~ constant("{") ++
        constant(
          "implicit val customConfig: io.circe.generic.extras.Configuration = io.circe.generic.extras.Configuration.default.withDefaults.withDiscriminator(\""
        ).map(_.indent(2)) + discriminantType + constant("\")") ++
        (accessModifier + constant(
          "implicit def jsonDecoder(implicit vd: io.circe.Decoder["
        )).map(_.indent(2)) +
        fieldType.contramap[SourceFile](_.fields.head) + constant(
          "]): io.circe.Decoder["
        ) + sourceName + constant("] =") ++
        constant("vd.map(").map(_.indent(4)) + sourceName + constant(".apply)") ++
        (accessModifier + constant(
          "implicit def jsonEncoder(implicit ve: io.circe.Encoder["
        )).map(_.indent(2)) +
        fieldType.contramap[SourceFile](_.fields.head) + constant(
          "]): io.circe.Encoder["
        ) + sourceName + constant("] =") ++
        constant("ve.contramap(_.value)").map(_.indent(4)) ++
        constant("}")

    override def companion: PartConv[SourceFile] =
      cond(_.internalSchemas.isEmpty, singular, discriminant)

    def singular: PartConv[SourceFile] = cond(_.wrapper, wrapper, props)
    def discriminant: PartConv[SourceFile] =
      forList(singular, _ ++ _)
        .contramap[SourceFile](_.internalSchemas) ++ discriminantProps

    override def resolve(src: SourceFile): SourceFile =
      src.modify(replaceJsonType)
  }
}
