package com.github.eikek.sbt.openapi

trait CustomMapping { self =>

  def changeType(td: TypeDef): TypeDef

  def changeSource(src: SourceFile): SourceFile

  def andThen(cm: CustomMapping): CustomMapping = new CustomMapping {
    def changeType(td: TypeDef): TypeDef =
      cm.changeType(self.changeType(td))

    def changeSource(src: SourceFile): SourceFile =
      cm.changeSource(self.changeSource(src))
  }
}

object CustomMapping {

  def apply(tf: PartialFunction[TypeDef, TypeDef]
    , sf: PartialFunction[SourceFile, SourceFile]): CustomMapping =
    new CustomMapping {
      def changeType(td: TypeDef) = tf.lift(td).getOrElse(td)
      def changeSource(src: SourceFile) = sf.lift(src).getOrElse(src)
    }

  def forType(f: PartialFunction[TypeDef, TypeDef]): CustomMapping =
    apply(f, PartialFunction.empty)

  def forSource(f: PartialFunction[SourceFile, SourceFile]): CustomMapping =
    apply(PartialFunction.empty, f)

  def forName(f: PartialFunction[String, String]): CustomMapping =
    forSource({ case s => s.copy(name = f.lift(s.name).getOrElse(s.name)) })

  val none = apply(PartialFunction.empty, PartialFunction.empty)
}
