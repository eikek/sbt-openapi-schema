package com.github.eikek.sbt.openapi

case class Doc(text: String) {
  def isEmpty: Boolean = text.trim.isEmpty
}
object Doc {
  val empty = Doc("")
}
