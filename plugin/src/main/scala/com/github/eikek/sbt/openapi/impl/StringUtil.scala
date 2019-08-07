package com.github.eikek.sbt.openapi.impl

object StringUtil {

  implicit class StringHelper(s: String) {
    def nullToEmpty: String =
      if (s == null) "" else s

    def asNonEmpty: Option[String] =
      Option(s).map(_.trim).filter(_.nonEmpty)
  }

}
