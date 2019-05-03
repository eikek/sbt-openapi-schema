package com.github.eikek.sbt.openapi

trait Annotation {
  def render: String
}

object Annotation {
  def apply(code: String): Annotation =
    new Annotation { val render = fixAnnotationString(code) }

  private def fixAnnotationString(str: String): String =
    if (!str.startsWith("@")) fixAnnotationString("@" + str)
    else if (!str.endsWith(")")) str + "()"
    else str
}
