package com.github.eikek.sbt.openapi

sealed trait Type {
  def isCollection: Boolean
}
object Type {

  trait PrimitiveType extends Type {
    val isCollection = false
  }
  trait CollectionType extends Type {
    val isCollection = true
  }

  case object Bool extends PrimitiveType
  case object String extends PrimitiveType
  case object Int32 extends PrimitiveType
  case object Int64 extends PrimitiveType
  case object Float32 extends PrimitiveType
  case object Float64 extends PrimitiveType
  case object BigDecimal extends PrimitiveType
  case object Uuid extends PrimitiveType
  case object Url extends PrimitiveType
  case object Uri extends PrimitiveType
  case class Date(repr: TimeRepr) extends PrimitiveType
  case class DateTime(repr: TimeRepr) extends PrimitiveType
  case class Sequence(param: Type) extends CollectionType
  case class Map(key: Type, value: Type) extends CollectionType
  case class Ref(name: String) extends PrimitiveType

  case object Json extends PrimitiveType

  sealed trait TimeRepr
  object TimeRepr {
    case object Number extends TimeRepr
    case object String extends TimeRepr
  }
}
