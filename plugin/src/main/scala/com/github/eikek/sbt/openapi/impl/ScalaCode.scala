package com.github.eikek.sbt.openapi.impl

import com.github.eikek.sbt.openapi._
import PartConv._

object ScalaCode {
  val primitiveTypeMapping: TypeMapping = {
    TypeMapping(
      Type.Bool -> TypeDef("Boolean", Imports.empty),
      Type.String -> TypeDef("String", Imports.empty),
      Type.Int32 -> TypeDef("Int", Imports.empty),
      Type.Int64 -> TypeDef("Long", Imports.empty),
      Type.Float32 -> TypeDef("Float", Imports.empty),
      Type.Float64 -> TypeDef("Double", Imports.empty),
      Type.Uuid -> TypeDef("UUID", Imports("java.util.UUID")),
      Type.Url -> TypeDef("URL", Imports("java.net.URL")),
      Type.Uri -> TypeDef("URI", Imports("java.net.URI")),
      Type.Date(Type.TimeRepr.String) -> TypeDef("LocalDate", Imports("java.time.LocalDate")),
      Type.Date(Type.TimeRepr.Number) -> TypeDef("LocalDate", Imports("java.time.LocalDate")),
      Type.DateTime(Type.TimeRepr.String) -> TypeDef("LocalDateTime", Imports("java.time.LocalDateTime")),
      Type.DateTime(Type.TimeRepr.Number) -> TypeDef("LocalDateTime", Imports("java.time.LocalDateTime"))
    )
  }

  def defaultTypeMapping(cm: CustomMapping): TypeMapping =  {
      case Type.Sequence(param) =>
        defaultTypeMapping(cm)(param).
          map(el => cm.changeType(TypeDef(s"List[${el.name}]", el.imports)))
      case Type.Map(key, value) =>
        for {
          k <- defaultTypeMapping(cm)(key)
          v <- defaultTypeMapping(cm)(value)
        } yield cm.changeType(TypeDef(s"Map[${k.name},${v.name}]", k.imports ++ v.imports))
      case Type.Ref(name) =>
        val srcRef = SingularSchemaClass(name)
        Some(TypeDef(resolveSchema(srcRef, cm).name, Imports.empty))
      case t =>
        primitiveTypeMapping(t).map(cm.changeType)
  }

  def enclosingObject(enclosingTraitName: String, cfg: ScalaConfig): PartConv[SourceFile] = {
    val parents: PartConv[List[Superclass]] =
      listSplit(
        constant[Superclass]("extends") ~ superclass,
        constant("with") ~ forListSep(superclass, Part(", "))
      )

    val fieldPart: PartConv[Field] =
      cond(
        f => f.nullablePrimitive,
        fieldName + PartConv.of(": Option[") + fieldType + PartConv.of("]"),
        fieldName + PartConv.of(": ") + fieldType
      )

    val internalCaseClass = constant("case class") ~ sourceName ~ forList(annotation, _ ++ _).contramap[SourceFile](_.ctorAnnot) ~ constant("(") ++
      forListSep(fieldPart, Part(", ")).map(_.indent(2)).contramap(_.fields.filterNot(_.prop.discriminator)) ++
      constant(s") extends $enclosingTraitName")

    val internalCaseClasses: PartConv[SourceFile] =
      forList(internalCaseClass, _ ++ _).contramap(_.internalSchemas)

    constant("object") ~ sourceName ~ forList(annotation, _ ++ _).contramap[SourceFile](_.ctorAnnot) ~ constant("{") ++
      constant("implicit val customConfig: Configuration = Configuration.default.withDefaults.withDiscriminator(\"type\")").map(_.indent(2)) ++
      internalCaseClasses.map(_.indent(2)) ++
      cfg.json.companion.map(_.indent(2)) ++
      constant("}") ~ parents.map(_.newline).contramap(_.parents)
  }

  def sealedTrait: PartConv[SourceFile] = {
    val fieldPart: PartConv[Field] =
      cond(
        f => f.nullablePrimitive,
        constant("val") ~ fieldName + PartConv.of(": Option[") + fieldType + PartConv.of("]"),
        constant("val") ~ fieldName + PartConv.of(": ") + fieldType
      )
    val parents: PartConv[List[Superclass]] =
      listSplit(
        constant[Superclass]("extends") ~ superclass,
        constant("with") ~ forListSep(superclass, Part(", "))
      )
    constant("sealed trait") ~ sourceName ~ forList(annotation, _ ++ _).contramap[SourceFile](_.ctorAnnot) ~ constant("{") ++
      forListSep(fieldPart, Part("; ")).map(_.indent(2)).contramap(_.fields.filterNot(_.prop.discriminator)) ++
      constant("}") ~ parents.map(_.newline).contramap(_.parents)
  }

  def caseClass: PartConv[SourceFile] = {
    val fieldPart: PartConv[Field] =
      cond(
        f => f.nullablePrimitive,
        fieldName + PartConv.of(": Option[") + fieldType + PartConv.of("]"),
        fieldName + PartConv.of(": ") + fieldType
      )
    val parents: PartConv[List[Superclass]] =
      listSplit(
        constant[Superclass]("extends") ~ superclass,
        constant("with") ~ forListSep(superclass, Part(", "))
      )
    constant("case class") ~ sourceName ~ forList(annotation, _ ++ _).contramap[SourceFile](_.ctorAnnot) ~ constant("(") ++
      forListSep(fieldPart, Part(", ")).map(_.indent(2)).contramap(_.fields) ++
      constant(")") ~ parents.map(_.newline).contramap(_.parents)
  }

  def fileHeader: PartConv[SourceFile] =
    pkg.contramap[SourceFile](_.pkg) ++
      imports.map(_.newline).contramap(_.imports) ++
      doc.contramap(_.doc)

  def generate(sc: SchemaClass, pkg: Pkg, cfg: ScalaConfig): (String, String) = {
    sc match {
      case ssc: SingularSchemaClass =>
        val src = resolveSchema(sc, cfg.mapping).copy(pkg = pkg).modify(cfg.json.resolve)
        val conv = fileHeader ++ caseClass ++ cfg.json.companion
        (src.name, conv.toPart(src).render)
      case dsc: DiscriminantSchemaClass =>
        val src = resolveSchema(sc, cfg.mapping).copy(pkg = pkg).modify(cfg.json.resolve)
        val conv = fileHeader ++ sealedTrait ++ enclosingObject(src.name, cfg)
        (src.name, conv.toPart(src).render)
    }
  }

  def resolveSchema(sc: SchemaClass, cm: CustomMapping): SourceFile = {
    val tm = defaultTypeMapping(cm)
    SchemaClass.resolve(sc, Pkg(""), tm, cm)
  }
}
