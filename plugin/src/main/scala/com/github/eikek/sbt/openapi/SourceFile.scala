package com.github.eikek.sbt.openapi

case class SourceFile(name: String
  , pkg: Pkg
  , imports: Imports
  , annot: List[Annotation]
  , ctorAnnot: List[Annotation]
  , doc: Doc
  , fields: List[Field]
  , parents: List[Superclass] = Nil
  , wrapper: Boolean) {

  def addImports(is: Imports): SourceFile =
    copy(imports = imports ++ is)

  def addAnnotation(a: Annotation): SourceFile =
    copy(annot = a :: annot)

  def addCtorAnnotation(a: Annotation): SourceFile =
    copy(ctorAnnot = a :: ctorAnnot)

  def addParents(s0: Superclass, sn: Superclass*): SourceFile =
    copy(parents = (s0 :: sn.toList ::: parents).distinct)

  def modify(f: SourceFile => SourceFile): SourceFile =
    f(this)
}
