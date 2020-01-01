package com.github.eikek.sbt.openapi.impl

import com.github.eikek.sbt.openapi._

trait SchemaClass {
  val name: String
  val properties: List[Property]
  val doc: Doc
  val wrapper: Boolean
}

case class SingularSchemaClass(name: String
                               , properties: List[Property] = Nil
                               , doc: Doc = Doc.empty
                               , wrapper: Boolean = false, discriminatorRef: Option[String] = None) extends SchemaClass {
  def +(p: Property): SingularSchemaClass =
    copy(properties = p :: properties)

  def withDoc(doc: Doc): SingularSchemaClass =
    copy(doc = doc)
}

case class DiscriminantSchemaClass(name: String
                                   , properties: List[Property] = Nil
                                   , doc: Doc = Doc.empty
                                   , wrapper: Boolean = false, subSchemas: List[SingularSchemaClass]) extends SchemaClass {

}

object SchemaClass {

  def resolve(sc: SchemaClass, pkg: Pkg, tm: TypeMapping, cm: CustomMapping): SourceFile = {
    val topLevelFields = sc.properties.map(p => Field(p, Nil, tm(p.`type`).getOrElse(sys.error(s"No type for: $p"))))

    val internalSchemas = sc match {
      case dsc: DiscriminantSchemaClass =>
        dsc.subSchemas
          .map(ss => resolve(ss, pkg, tm, CustomMapping.none))
          .map(ss => ss.addFields(topLevelFields))
          .map(ss => ss.modify(_.copy(isInternal = true)))
      case _ => List.empty
    }

    SourceFile(name = sc.name
      , pkg = pkg
      , imports = Imports.empty
      , annot = Nil
      , ctorAnnot = Nil
      , doc = sc.doc
      , fields = topLevelFields
      , wrapper = sc.wrapper
      , internalSchemas = internalSchemas).
      modify(cm.changeSource).
      modify(s => s.addImports(Imports.flatten(s.parents.map(_.imports)))).
      modify(resolveFieldImports)
  }

  private def resolveFieldImports(src: SourceFile): SourceFile =
    src.fields.map(_.typeDef).
      foldLeft(src){ (s, td) =>
        s.addImports(td.imports)
      }
}
