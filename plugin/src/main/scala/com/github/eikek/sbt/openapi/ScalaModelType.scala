package com.github.eikek.sbt.openapi

sealed trait ScalaModelType

object ScalaModelType {
  case object CaseClass extends ScalaModelType
  case object Trait extends ScalaModelType
}