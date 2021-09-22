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

  def apply(
      tf: PartialFunction[TypeDef, TypeDef],
      sf: PartialFunction[SourceFile, SourceFile]
  ): CustomMapping =
    new CustomMapping {
      def changeType(td: TypeDef) = tf.lift(td).getOrElse(td)
      def changeSource(src: SourceFile) = sf.lift(src).getOrElse(src)
    }

  def forType(f: PartialFunction[TypeDef, TypeDef]): CustomMapping =
    apply(f, PartialFunction.empty)

  def forSource(f: PartialFunction[SourceFile, SourceFile]): CustomMapping =
    apply(PartialFunction.empty, f)

  def forName(f: PartialFunction[String, String]): CustomMapping = {
    def changeRef(field: Field): Field =
      field.prop.`type` match {
        case Type.Ref(name) =>
          field.copy(prop =
            field.prop.copy(`type` = Type.Ref(f.lift(name).getOrElse(name)))
          )
        case Type.Sequence(Type.Ref(name)) =>
          field.copy(prop =
            field.prop.copy(`type` =
              Type.Sequence(Type.Ref(f.lift(name).getOrElse(name)))
            )
          )
        case Type.Map(kt, vt) =>
          val ktn = kt match {
            case Type.Ref(name) => Type.Ref(f.lift(name).getOrElse(name))
            case _              => kt
          }
          val vtn = vt match {
            case Type.Ref(name) => Type.Ref(f.lift(name).getOrElse(name))
            case _              => vt
          }
          field.copy(prop = field.prop.copy(`type` = Type.Map(ktn, vtn)))
        case _ => field
      }

    forSource { case s =>
      s.copy(name = f.lift(s.name).getOrElse(s.name))
        .copy(fields = s.fields.map(changeRef))
    }
  }

  def forField(f: PartialFunction[Field, Field]): CustomMapping =
    forSource { case s =>
      val newFields = s.fields.map(field => f.lift(field).getOrElse(field))
      s.copy(fields = newFields)
    }

  def forFormatType(f: PartialFunction[String, Field => Field]): CustomMapping =
    forField {
      case field if f.isDefinedAt(field.prop.format.getOrElse("")) =>
        f(field.prop.format.getOrElse(""))(field)
    }

  val none = apply(PartialFunction.empty, PartialFunction.empty)
}
