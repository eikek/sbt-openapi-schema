package com.github.eikek.sbt.openapi.impl

import com.github.eikek.sbt.openapi._
import PartConv._

object ElmCode {

  val module: PartConv[SourceFile] =
    PartConv(src => Part(s"module ${src.pkg.name}.${src.name} exposing (..)"))

  val doc: PartConv[Doc] = PartConv(d =>
    if (d.isEmpty) Part.empty
    else Part("{--") ++ Part(d.text).prefix("  - ") ++ Part(" --}"))

  val primitiveTypeMapping: TypeMapping = {
    TypeMapping(
      Type.Bool -> TypeDef("Bool", Imports.empty),
      Type.String -> TypeDef("String", Imports.empty),
      Type.Int32 -> TypeDef("Int", Imports.empty),
      Type.Int64 -> TypeDef("Int", Imports.empty),
      Type.Float32 -> TypeDef("Float", Imports.empty),
      Type.Float64 -> TypeDef("Float", Imports.empty),
      Type.Uuid -> TypeDef("String", Imports.empty),
      Type.Url -> TypeDef("String", Imports.empty),
      Type.Uri -> TypeDef("String", Imports.empty),
      Type.Date -> TypeDef("String", Imports.empty),
      Type.DateTime -> TypeDef("String", Imports.empty),
    )
  }

  def emptyValue(pkg: Pkg): PartConv[TypeDef] =
    PartConv {
      case TypeDef("Bool", _) => Part("False")
      case TypeDef("String", _) => Part("\"\"")
      case TypeDef("Int", _) => Part("0")
      case TypeDef("Float", _) => Part("0.0")
      case TypeDef(n, _) if n startsWith "Maybe" => Part("Nothing")
      case TypeDef(n, _) if n startsWith "(List" => Part("[]")
      case TypeDef(n, _) if n startsWith "(Dict" => Part("Dict.empty")
      case TypeDef(name, _) => Part(s"${pkg.name}.${name}.empty")
    }

  def defaultTypeMapping(cm: CustomMapping, pkg: Pkg): TypeMapping =  {
      case Type.Sequence(param) =>
        defaultTypeMapping(cm, pkg)(param).
          map(el => cm.changeType(TypeDef(s"(List ${el.name})", el.imports)))
      case Type.Map(key, value) =>
        for {
          k <- defaultTypeMapping(cm, pkg)(key)
          v <- defaultTypeMapping(cm, pkg)(value)
        } yield cm.changeType(TypeDef(s"(Dict ${k.name},${v.name})", k.imports ++ v.imports ++ Imports("Dict")))
      case Type.Ref(name) =>
        val srcRef = SchemaClass(name)
        val refName = resolveSchema(srcRef, cm, pkg).name
        Some(TypeDef(refName, Imports(s"${pkg.name}.$refName exposing ($refName)")))
      case t =>
        primitiveTypeMapping(t)
  }

  def typeAlias: PartConv[SourceFile] = {
    val fieldPart: PartConv[Field] =
      cond(
        f => f.nullablePrimitive,
        fieldName + PartConv.of(": Maybe ") + fieldType,
        fieldName + PartConv.of(": ") + fieldType
      )
    constant("type alias") ~ sourceName ~ constant("=") ++
    constant("  {") ~ forListSep(fieldPart, Part("\n  , ")).contramap(_.fields) ++
    constant("  }")
  }

  def defaultValue(pkg: Pkg): PartConv[SourceFile] = {
    val fieldPart: PartConv[Field] =
      cond(
        f => f.nullablePrimitive,
        fieldName ~ constant("= Nothing"),
        fieldName ~ constant("=") ~ emptyValue(pkg).contramap(_.typeDef)
      )

    constant("empty:") ~  sourceName ++
    constant("empty =") ++
    constant("  {") ~ forListSep(fieldPart, Part("\n  , ")).contramap(_.fields) ++
    constant("  }")
  }


  def fileHeader: PartConv[SourceFile] =
    constant("{-- This file has been generated from an openapi spec. --}").map(_.newline) ++
    module.map(_.newline) ++
      imports.map(_.newline).contramap(_.imports) ++
      doc.contramap(_.doc)

  def generate(sc: SchemaClass, pkg: Pkg, cfg: ElmConfig): (String, String) = {
    val src = resolveSchema(sc, cfg.mapping, pkg).copy(pkg = pkg).modify(cfg.json.resolve)
    val conv = fileHeader ++ typeAlias.map(_.newline) ++ defaultValue(pkg).map(_.newline) ++ cfg.json.jsonCodec
    (src.name, conv.toPart(src).render)
  }

  def resolveSchema(sc: SchemaClass, cm: CustomMapping, pkg: Pkg): SourceFile = {
    val tm = defaultTypeMapping(cm, pkg)
    SchemaClass.resolve(sc, pkg, tm, cm)
  }
}
