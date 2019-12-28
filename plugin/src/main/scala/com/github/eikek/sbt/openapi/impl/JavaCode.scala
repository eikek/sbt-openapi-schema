package com.github.eikek.sbt.openapi.impl

import com.github.eikek.sbt.openapi._
import PartConv._

object JavaCode {

  val primitiveTypeMapping: TypeMapping = {
    TypeMapping(
      Type.Bool -> TypeDef("boolean", Imports.empty),
      Type.String -> TypeDef("String", Imports.empty),
      Type.Int32 -> TypeDef("int", Imports.empty),
      Type.Int64 -> TypeDef("long", Imports.empty),
      Type.Float32 -> TypeDef("float", Imports.empty),
      Type.Float64 -> TypeDef("double", Imports.empty),
      Type.Uuid -> TypeDef("UUID", Imports("java.util.UUID")),
      Type.Url -> TypeDef("URL", Imports("java.net.URL")),
      Type.Uri -> TypeDef("URI", Imports("java.net.URI")),
      Type.Date(Type.TimeRepr.String) -> TypeDef("LocalDate", Imports("java.time.LocalDate")),
      Type.Date(Type.TimeRepr.Number) -> TypeDef("LocalDate", Imports("java.time.LocalDate")),
      Type.DateTime(Type.TimeRepr.String) -> TypeDef("LocalDateTime", Imports("java.time.LocalDateTime")),
      Type.DateTime(Type.TimeRepr.Number) -> TypeDef("LocalDateTime", Imports("java.time.LocalDateTime"))
    )
  }

  def boxed(td: TypeDef): TypeDef = td match {
    case TypeDef("boolean", is) => TypeDef("Boolean", is)
    case TypeDef("int", is) => TypeDef("Integer", is)
    case TypeDef("long", is) => TypeDef("Long", is)
    case TypeDef("float", is) => TypeDef("Float", is)
    case TypeDef("double", is) => TypeDef("Double", is)
    case _ => td
  }

  def defaultTypeMapping(cm: CustomMapping): TypeMapping = {
    case Type.Sequence(param) =>
      defaultTypeMapping(cm)(param).
        map(el => cm.changeType(TypeDef(s"List<${boxed(el).name}>", el.imports ++ Imports("java.util.List"))))
    case Type.Map(key, value) =>
      for {
        k <- defaultTypeMapping(cm)(key)
        v <- defaultTypeMapping(cm)(value)
      } yield cm.changeType(TypeDef(s"Map<${boxed(k).name},${boxed(v).name}>", k.imports ++ v.imports ++ Imports("java.util.Map")))
    case Type.Ref(name) =>
      val schema = SingularSchemaClass(name)
      Some(TypeDef(resolveSchema(schema, cm).name, Imports.empty))
    case t =>
        primitiveTypeMapping(t).map(cm.changeType)
  }

  object BuilderClass {
    def builderMethod: PartConv[SourceFile] =
      constant[SourceFile]("public static Builder newBuilder() {") ++
        constant("return new Builder();").map(_.indent(2)) ++
        constant("}")

    def apply(cfg: JavaConfig): PartConv[SourceFile] =
      PartConv(src => if (src.wrapper) Part.empty else make(cfg).toPart(src))

    def make(cfg: JavaConfig): PartConv[SourceFile] =
      builderMethod.map(_.newline) ++
        forList(annotation, _ ++ _).contramap(s => cfg.json.builderAnnotations(s)) ++
        constant("public static final class Builder") ~ DataClass.parents.contramap[SourceFile](_ => cfg.builderParents) ~ constant("{") ++
        DataClass.fieldDeclaration("private").map(_.indent(2).newline).contramap(_.fields) ++
        build.map(_.indent(2).newline) ++
        forList(setter, _ ++ _).map(_.indent(2)).contramap(_.fields) ++ constant("}")

    def build: PartConv[SourceFile] =
      constant("public") ~ string.contramap[SourceFile](_.name) ~ constant("build() {") ++
        constant("return new").map(_.indent(2)) ~ string.contramap(_.name) + constant("(") +
        DataClass.fieldNameList.contramap(_.fields) + constant(");") ++
        constant("}")

    def setter: PartConv[Field] =
      doc.contramap[Field](_.prop.doc) ++
      constant("public Builder set") + fieldName.map(_.capitalize) + constant("(") + DataClass.fieldType ~
        constant("value) {") ++ constant("this.").map(_.indent(2)) + fieldName ~ constant(" = value;") ++
        constant("return this;").map(_.indent(2)) ++
        constant("}").map(_.newline)
  }

  object DataClass {
    def apply(tm: TypeMapping, cfg: JavaConfig): PartConv[SourceFile] =
      forList(annotation, _ ++ _).contramap[SourceFile](_.annot) ++
        constant("public final class") ~ sourceName ~ parents.contramap[SourceFile](_.parents) ~ constant("{") ++
        fieldDeclaration("private final").map(_.indent(2).newline).contramap[SourceFile](_.fields) ++
        constructor.map(_.indent(2).newline) ++
        forList(getter, _ ++ _).map(_.indent(2).newline).contramap(_.fields) ++
        setter ++
        equals.map(_.newline) ++
        hashcode.map(_.indent(2).newline) ++
        tostring.map(_.indent(2).newline) ++
        BuilderClass(cfg).map(_.indent(2)) ++ constant("}")

    def parents: PartConv[List[Superclass]] =
      listSplit(
        constant("extends") ~ superclass,
        constant("implements") ~ forListSep(superclass, Part(", "))
      )

    def fieldType: PartConv[Field] = PartConv { f =>
      if (f.prop.nullable) Part(boxed(f.typeDef).name) else Part(f.typeDef.name)
    }

    def fieldDeclaration(modifier: String): PartConv[List[Field]] = {
      val pc: PartConv[Field] =
        forList(annotation, _ ++ _).contramap[Field](_.annot) ++
          constant(modifier) ~ fieldType ~ string.contramap[Field](_.prop.name).map(_.semicolon)

      forList(pc, _ ++ _)
    }

    def constructor: PartConv[SourceFile] =
      forList(annotation, _ ++ _).contramap[SourceFile](_.ctorAnnot) ++
      constant("public") ~ string.contramap[SourceFile](_.name) + constant("(") +
        typeNameList.contramap(_.fields) + constant(") {") ++
        fieldAssignList.contramap[SourceFile](_.fields).map(_.indent(2)) ++ constant("}")

    def getter: PartConv[Field] = {
      val fg = fieldType ~ cond(_.prop.`type` == Type.Bool, constant("is"), constant("get")) +
        fieldName.map(_.capitalize) + constant("() {") ++
        constant("return this.").map(_.indent(2)) + fieldName.map(_.semicolon) ++
        constant("}").map(_.newline)

      val opt = doc.contramap[Field](_.prop.doc) ++ constant("public Optional<") + fieldType + constant(">") ~
        cond(_.prop.`type` == Type.Bool, constant("is"), constant("get")) +
        fieldName.map(_.capitalize) + constant("Optional() {") ++
        constant("return Optional.ofNullable(this.").map(_.indent(2)) + fieldName + constant(");") ++
        constant("}").map(_.newline)

      cond[Field](_.prop.nullable, fg ++ opt, doc.contramap[Field](_.prop.doc) ++ constant("public") ~ fg)
    }

    def setter: PartConv[SourceFile] = PartConv { sc =>
      def withMethod: PartConv[Field] =
        doc.contramap[Field](_.prop.doc) ++
        constant("public") ~ constant(sc.name) ~ constant("with") + fieldName.map(_.capitalize) + constant("(") + fieldType ~ fieldName + constant(") {") ++
        constant("return new").map(_.indent(2)) ~ constant(sc.name) + constant("(")  + PartConv.ofPart(fieldNameList.toPart(sc.fields)) + constant(");") ++
        constant("}").map(_.newline)

      forList(withMethod, _ ++ _).map(_.indent(2).newline).toPart(sc.fields)
    }

    val hashcode: PartConv[SourceFile] =
      constant[SourceFile]("@Override") ++ constant("public int hashCode() {") ++
        constant("return Objects.hash(").map(_.indent(2)) + fieldNameList.contramap[SourceFile](_.fields) + constant(");") ++
        constant("}")

    def equals: PartConv[SourceFile] = {
      val header: PartConv[SourceFile] =
        constant[SourceFile]("@Override") ++ constant("public boolean equals(Object other) {") ++
          constant("if (other == this) {").map(_.indent(2)) ++
          constant("return true;").map(_.indent(4)) ++
          constant("}").map(_.indent(2)) ++
          constant("if (other == null || getClass() != other.getClass()) {").map(_.indent(2)) ++
          constant("return false;").map(_.indent(4)) ++
          constant("}").map(_.indent(2))

      val eq: PartConv[Field] =
        constant("Objects.equals(")+ fieldName + constant(", v.") + fieldName + constant(")")

      (header ++ sourceName.map(_.indent(2)) ~ constant("v = (") + sourceName + constant(") other;") ++
        constant("return").map(_.indent(2)) ~ forListSep(eq, Part(" && ")).contramap(_.fields) + constant(";") ++
        constant("}")).map(_.indent(2))
    }

    def tostring: PartConv[SourceFile] = {
      val pc: PartConv[SourceFile] =
        forListSep(fieldName + constant("=%s"), Part(", ")).contramap(_.fields)

      constant[SourceFile]("@Override") ++
      constant("public String toString() {") ++
      constant("return String.format(\"").map(_.indent(2)) + sourceName + constant("[") + pc + constant("]\", ") ++
      fieldNameList.map(_.indent(4)).contramap(_.fields) + constant(");") ++ constant("}")
    }

    def typeNameList: PartConv[List[Field]] = {
      val pc: PartConv[Field] = PartConv { f =>
        fieldType.toPart(f) ~ Part(f.prop.name)
      }
      forListSep(pc, Part(", "))
    }
    def fieldNameList: PartConv[List[Field]] = {
      val pc: PartConv[Field] = PartConv(f => Part(f.prop.name))
      forListSep(pc, Part(", "))
    }
    def fieldAssignList: PartConv[List[Field]] = {
      val pc: PartConv[Field] = PartConv(f => Part("this.") + Part(f.prop.name) ~ Part("=") ~ Part(f.prop.name).semicolon)
      forList(pc, (a, b) => a ++ b)
    }
  }

  def fileHeader: PartConv[SourceFile] =
    pkg.contramap[SourceFile](_.pkg).map(_.semicolon.newline) ++
      imports.contramap[SourceFile](_.imports).map(_.semicolon.newline) ++
      constant("// This is a generated file. Do not edit.").map(_.newline) ++
      doc.contramap(_.doc)

  def generate(sc: SchemaClass, pkg: Pkg, cfg: JavaConfig): (String, String) = {
    val tm = defaultTypeMapping(cfg.mapping)
    val src = resolveSource(resolveSchema(sc, cfg.mapping), cfg).copy(pkg = pkg)
    val conv = fileHeader ++ DataClass(tm, cfg)
    (src.name, conv.toPart(src).render)
  }

  def resolveSource(src: SourceFile, cfg: JavaConfig): SourceFile = {
    src.
      addImports(Imports.flatten(cfg.builderParents.map(_.imports))).
      addImports(Imports("java.util.Objects", "java.util.Optional")).
      modify(cfg.json.resolve)
  }

  def resolveSchema(sc: SchemaClass, cm: CustomMapping): SourceFile = {
    val tm = defaultTypeMapping(cm)
    SchemaClass.resolve(sc, Pkg(""), tm, cm)
  }
}
