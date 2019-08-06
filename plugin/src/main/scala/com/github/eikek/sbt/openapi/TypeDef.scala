package com.github.eikek.sbt.openapi

case class TypeDef(name: String, imports: Imports, origin: Type)

object TypeDef {
  def apply(name: String, imports:Imports): Type => TypeDef =
    t => TypeDef(name, imports, t)
}