package com.github.eikek.sbt.openapi

case class Part(cnt: String) {
  def ++(p: Part): Part =
    render match {
      case "" => p
      case s =>
        p.render match {
          case "" => this
          case s2 => Part(s +"\n" + s2)
        }
    }

  def +(p: Part): Part = render match {
    case "" => p
    case s if s.endsWith(p.render) => this
    case s => Part(s + p.render)
  }

  def ~(p: Part): Part =
    render match {
      case s if s.endsWith(" ") => Part(s + p.render)
      case s => Part(s + " " + p.render)
    }

  def newline: Part =
    render match {
      case "" => this
      case s => Part(s + "\n")
    }

  def prefix(s: String): Part =
    Part(render.split('\n').toList.map(l => s + l).mkString("\n"))

  def indent(n: Int): Part =
    prefix(List.fill(n)(" ").mkString)

  def semicolon: Part = render match {
    case "" => this
    case s =>
      Part(s.split('\n').toList.map(l => if (l.endsWith(";")) l else l + ";").mkString("\n"))
  }

  def capitalize: Part =
    Part(render.capitalize)

  def isEmpty: Boolean =
    cnt.trim.isEmpty

  def render: String =
    if (isEmpty) "" else cnt
}

object Part {
  val empty = Part("")
}
