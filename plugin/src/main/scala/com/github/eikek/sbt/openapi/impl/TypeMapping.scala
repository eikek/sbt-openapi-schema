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
}
