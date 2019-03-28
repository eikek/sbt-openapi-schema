package com.github.eikek.sbt.openapi.impl

object StringUtil {

  implicit class StringHelper(s: String) {
    def nullToEmpty: String =
      if (s == null) "" else s
  }

}
