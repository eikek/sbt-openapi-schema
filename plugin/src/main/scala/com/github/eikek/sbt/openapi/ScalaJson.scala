package com.github.eikek.sbt.openapi

import PartConv._

trait ScalaJson {

  def companion: PartConv[SourceFile]

  def resolve(src: SourceFile): SourceFile

}

object ScalaJson {
  val none = new ScalaJson {
    def companion = PartConv(_ => Part.empty)
    def resolve(src: SourceFile): SourceFile = src
  }

  val circeSemiauto = new ScalaJson {
    val props: PartConv[SourceFile] =
      constant("object") ~ sourceName ~ constant("{") ++
        constant("implicit val jsonDecoder: Decoder[").map(_.indent(2)) + sourceName + constant("] = deriveDecoder[") + sourceName + constant("]") ++
        constant("implicit val jsonEncoder: Encoder[").map(_.indent(2)) + sourceName + constant("] = deriveEncoder[") + sourceName + constant("]") ++
        constant("}")

    val wrapper: PartConv[SourceFile] =
      constant("object") ~ sourceName ~ constant("{") ++
        constant("implicit def jsonDecoder(implicit vd: Decoder[").map(_.indent(2)) +
        fieldType.contramap[SourceFile](_.fields.head) + constant("]): Decoder[") + sourceName + constant("] =") ++
        constant("vd.map(").map(_.indent(4)) + sourceName + constant(".apply)") ++
        constant("implicit def jsonEncoder(implicit ve: Encoder[").map(_.indent(2)) +
        fieldType.contramap[SourceFile](_.fields.head) + constant("]): Encoder[") + sourceName + constant("] =") ++
        constant("ve.contramap(_.value)").map(_.indent(4)) ++
        constant("}")

    def companion =
      cond(_.wrapper, wrapper, props)

    def resolve(src: SourceFile): SourceFile =
      src.addImports(Imports("io.circe._", "io.circe.generic.semiauto._"))
  }

  val circeSemiautoExtra = new ScalaJson {

    val discriminantProps: PartConv[SourceFile] =
      constant("implicit val jsonDecoder: Decoder[").map(_.indent(2)) + sourceName + constant("] = deriveDecoder[") + sourceName + constant("]") ++
        constant("implicit val jsonEncoder: Encoder[").map(_.indent(2)) + sourceName + constant("] = deriveEncoder[") + sourceName + constant("]")

    def props(makePrivate: Boolean): PartConv[SourceFile] =
      constant("object") ~ sourceName ~ constant("{") ++
        constant("implicit val customConfig: Configuration = Configuration.default.withDefaults.withDiscriminator(\"").map(_.indent(2)) + discriminantType + constant("\")") ++
        constant(s"${if (makePrivate) "private " else ""}implicit val jsonDecoder: Decoder[").map(_.indent(2)) + sourceName + constant("] = deriveDecoder[") + sourceName + constant("]") ++
        constant(s"${if (makePrivate) "private " else ""}implicit val jsonEncoder: Encoder[").map(_.indent(2)) + sourceName + constant("] = deriveEncoder[") + sourceName + constant("]") ++
        constant("}")

    def wrapper(makePrivate: Boolean): PartConv[SourceFile] =
      constant("object") ~ sourceName ~ constant("{") ++
        constant("implicit val customConfig: Configuration = Configuration.default.withDefaults.withDiscriminator(\"").map(_.indent(2)) + discriminantType + constant("\")") ++
        constant(s"${if (makePrivate) "private " else ""}implicit def jsonDecoder(implicit vd: Decoder[").map(_.indent(2)) +
        fieldType.contramap[SourceFile](_.fields.head) + constant("]): Decoder[") + sourceName + constant("] =") ++
        constant("vd.map(").map(_.indent(4)) + sourceName + constant(".apply)") ++
        constant(s"${if (makePrivate) "private " else ""}implicit def jsonEncoder(implicit ve: Encoder[").map(_.indent(2)) +
        fieldType.contramap[SourceFile](_.fields.head) + constant("]): Encoder[") + sourceName + constant("] =") ++
        constant("ve.contramap(_.value)").map(_.indent(4)) ++
        constant("}")

    override def companion: PartConv[SourceFile] = cond(_.internalSchemas.isEmpty, singular(makePrivate = false), discriminant)

    def singular(makePrivate: Boolean): PartConv[SourceFile] = cond(_.wrapper, wrapper(makePrivate), props(makePrivate))
    def discriminant: PartConv[SourceFile] =
      forList(singular(makePrivate = true), _ ++ _).contramap[SourceFile](_.internalSchemas) ++ discriminantProps

    override def resolve(src: SourceFile): SourceFile =
      src.addImports(Imports("io.circe._", "io.circe.generic.extras.semiauto._", "io.circe.generic.extras.Configuration"))
  }
}
