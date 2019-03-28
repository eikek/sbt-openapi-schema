package com.github.eikek.sbt.openapi

trait Annotation {
  def render: String
}

object Annotation {
  def apply(code: String): Annotation =
    new Annotation { val render = code }
}
