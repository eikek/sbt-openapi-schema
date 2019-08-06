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
      case t@Type.Sequence(param) =>
        defaultTypeMapping(cm)(param).
          map(el => cm.changeType(TypeDef(s"List[${el.name}]", el.imports, t)))
      case t@Type.Map(key, value) =>
        for {
          k <- defaultTypeMapping(cm)(key)
          v <- defaultTypeMapping(cm)(value)
        } yield cm.changeType(TypeDef(s"Map[${k.name},${v.name}]", k.imports ++ v.imports, t))
      case t@Type.Ref(name) =>
        val srcRef = SchemaClass(name)
        Some(TypeDef(resolveSchema(srcRef, cm).name, Imports.empty, t))
      case t =>
        primitiveTypeMapping(t).map(cm.changeType)
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
    val src = resolveSchema(sc, cfg.mapping).copy(pkg = pkg).modify(cfg.json.resolve)
    val conv = fileHeader ++ caseClass ++ cfg.json.companion
    (src.name, conv.toPart(src).render)
  }

  def resolveSchema(sc: SchemaClass, cm: CustomMapping): SourceFile = {
    val tm = defaultTypeMapping(cm)
    SchemaClass.resolve(sc, Pkg(""), tm, cm)
  }
}
