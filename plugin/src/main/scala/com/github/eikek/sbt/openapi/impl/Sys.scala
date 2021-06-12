package com.github.eikek.sbt.openapi.impl

import scala.sys.process._

object Sys {

  def exec(cmd: Seq[String]): Int = {
    Process(cmd).!
  }

  def execSuccess(cmd: Seq[String]): Unit =
    exec(cmd) match{
      case 0 => ()
      case n =>
        val cmdStr = cmd.mkString(" ")
        sys.error(s"Command ${cmdStr} finished with error: $n")
    }

}
