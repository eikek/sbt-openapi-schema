package com.github.eikek.sbt.openapi

import com.github.eikek.sbt.openapi.PartConv._

trait ElmJson {

  def resolve(src: SourceFile): SourceFile

  def jsonCodec: PartConv[SourceFile]
}

object ElmJson {
  val none = new ElmJson {
    def resolve(src: SourceFile): SourceFile = src

    def jsonCodec = PartConv.empty
  }

  val decodePipeline = new ElmJson {

    sealed trait Direction {
      def name: String
    }
    object Direction {
      case object Dec extends Direction {
        val name = "Decode"
      }
      case object Enc extends Direction {
        val name = "Encode"
      }
    }

    def resolve(src: SourceFile): SourceFile =
      src.addImports(
        Imports(
          "Json.Decode as Decode",
          "Json.Decode.Pipeline as P",
          "Json.Encode as Encode"
        )
      )

    private def codecForType(dir: Direction, pkg: Pkg, t: Type): Part = t match {
      case Type.Sequence(param) =>
        val dec = codecForType(dir, pkg, param)
        Part.concat(Part(s"(${dir.name}.list "), dec, Part(")"))
      case Type.Map(kt, vt) =>
        Part.concat(Part(s"(${dir.name}.dict "), codecForType(dir, pkg, vt), Part(")"))
      case Type.Ref(name) =>
        if (dir == Direction.Dec) Part(s"${pkg.name}.$name.decoder")
        else Part(s"${pkg.name}.$name.encode")
      case Type.Bool =>
        Part(s"${dir.name}.bool")
      case Type.Int32 =>
        Part(s"${dir.name}.int")
      case Type.Int64 =>
        Part(s"${dir.name}.int")
      case Type.Float32 =>
        Part(s"${dir.name}.float")
      case Type.Float64 =>
        Part(s"${dir.name}.float")
      case Type.DateTime(Type.TimeRepr.Number) =>
        Part(s"${dir.name}.int")
      case Type.Date(Type.TimeRepr.Number) =>
        Part(s"${dir.name}.int")
      case _ =>
        Part(s"${dir.name}.string")
    }

    def codecForField(dir: Direction): PartConv[(Pkg, Field)] = PartConv {
      case (pkg, field) =>
        val codec = field.prop.`type` match {
          case Type.Ref(_) =>
            if (dir == Direction.Dec) Part(s"${pkg.name}.${field.typeDef.name}.decoder")
            else Part(s"${pkg.name}.${field.typeDef.name}.encode")
          case _ =>
            codecForType(dir, pkg, field.prop.`type`)
        }
        if (field.nullablePrimitive) {
          if (dir == Direction.Dec)
            Part.concat(Part(s"(${dir.name}.maybe "), codec, Part(")")) ~ Part("Nothing")
          else
            Part.concat(
              Part("(Maybe.map "),
              codec,
              Part(" >> Maybe.withDefault Encode.null)")
            )
        } else {
          codec
        }
    }

    private val requiredOrOptional: PartConv[(Pkg, Field)] =
      PartConv { case (_, field) =>
        if (field.nullablePrimitive) Part("P.optional")
        else Part("P.required")
      }

    private val decodeField: PartConv[(Pkg, Field)] =
      constant("|>").map(_.indent(4)) ~ requiredOrOptional ~
        fieldName.contramap[(Pkg, Field)](_._2).map(_.quoted) ~ codecForField(
          Direction.Dec
        )

    private val decoderObject: PartConv[SourceFile] =
      constant("decoder: Decode.Decoder") ~ sourceName ++
        constant("decoder =") ++
        constant("Decode.succeed").map(_.indent(2)) ~ sourceName ++
        forList(decodeField, _ ++ _).contramap(src => src.fields.map(f => (src.pkg, f)))

    private val decoderWrapper: PartConv[SourceFile] =
      constant("decoder: Decode.Decoder") ~ sourceName ++
        constant("decoder =") ++
        constant("Decode.map").map(_.indent(2)) ~ sourceName ~ codecForField(
          Direction.Dec
        ).contramap(src => (src.pkg, src.fields.head))

    private val encodeField: PartConv[(Pkg, Field)] =
      constant("(") ~ fieldName.contramap[(Pkg, Field)](_._2).map(_.quoted) + constant(
        ", ("
      ) +
        codecForField(Direction.Enc) ~ constant("value.") + fieldName
          .contramap[(Pkg, Field)](_._2) + constant(") )")

    private val encoderObject: PartConv[SourceFile] =
      constant("encode:") ~ sourceName ~ constant("->") ~ constant("Encode.Value") ++
        constant("encode value =") ++
        constant("Encode.object").map(_.indent(2)) ++
        constant("[").map(_.indent(4)) ~
        forListSep(encodeField, Part("\n    , ")).contramap(src =>
          src.fields.map(f => (src.pkg, f))
        ) ++
        constant("]").map(_.indent(4))

    private val encoderWrapper: PartConv[SourceFile] =
      constant("encode:") ~ sourceName ~ constant("->") ~ constant("Encode.Value") ++
        constant("encode value =") ++
        codecForField(Direction.Enc)
          .map(_.indent(2))
          .contramap[SourceFile](src => (src.pkg, src.fields.head)) ~ constant(
          "value.value"
        )

    private val decoder: PartConv[SourceFile] =
      cond(_.wrapper, decoderWrapper, decoderObject)

    private val encoder: PartConv[SourceFile] =
      cond(_.wrapper, encoderWrapper, encoderObject)

    def jsonCodec =
      decoder.map(_.newline) ++ encoder
  }
}
