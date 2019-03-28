package com.github.eikek.sbt.openapi

case class Imports(lines: List[String]) {
  def ++(is: Imports): Imports =
    Imports((lines ++ is.lines).distinct)
}
object Imports {
  val empty = Imports(Nil)

  def apply(i: String, is: String*): Imports =
    Imports(i :: is.toList)

  def flatten(l: Seq[Imports]): Imports =
    l.foldLeft(empty)(_ ++ _)
}
