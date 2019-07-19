package com.github.eikek.sbt.openapi

case class Field(prop: Property, annot: List[Annotation], typeDef: TypeDef) {

  val nullablePrimitive = prop.nullable && !prop.`type`.isCollection

}
