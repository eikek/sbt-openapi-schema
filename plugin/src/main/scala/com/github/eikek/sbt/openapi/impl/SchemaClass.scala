package com.github.eikek.sbt.openapi.impl

import com.github.eikek.sbt.openapi._

case class SchemaClass(name: String
  , properties: List[Property] = Nil
  , doc: Doc = Doc.empty
  , wrapper: Boolean = false
  , discriminatorRef: Option[String] = None) {

  def +(p: Property): SchemaClass =
    copy(properties = p :: properties)

  def withDoc(doc: Doc): SchemaClass =
    copy(doc = doc)
}

object SchemaClass {

  def resolve(sc: SchemaClass, pkg: Pkg, tm: TypeMapping, cm: CustomMapping): SourceFile = {
    SourceFile(name = sc.name
      , pkg = pkg
      , imports = Imports.empty
      , annot = Nil
      , ctorAnnot = Nil
      , doc = sc.doc
      , fields = sc.properties.map(p => Field(p, Nil, tm(p.`type`).getOrElse(sys.error(s"No type for: $p"))))
      , wrapper = sc.wrapper).
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
