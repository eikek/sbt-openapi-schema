package com.github.eikek.sbt.openapi.impl

import com.github.eikek.sbt.openapi._

trait TypeMapping { self =>
  def apply(t: Type): Option[TypeDef]

  def ++(next: TypeMapping): TypeMapping =
    (t: Type) => self(t).orElse(next(t))
}

object TypeMapping {
  def apply(t: (Type, TypeDef), ts: (Type, TypeDef)*): TypeMapping = {
    val m = (t :: ts.toList).toMap
    t: Type => m.get(t)
  }

  def resolveFieldImports(tm: TypeMapping)(src: SourceFile): SourceFile =
    src.fields.map(_.prop.`type`).
      foldLeft(src){ (s, t) =>
        tm(t).map(_.imports).map(s.addImports).getOrElse(s)
      }
}
