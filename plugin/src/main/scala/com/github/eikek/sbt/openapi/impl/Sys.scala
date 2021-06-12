package com.github.eikek.sbt.openapi.impl

import scala.sys.process._

import sbt.util.Logger

trait Sys {
  def exec(cmd: Seq[String]): Int
  def execSuccess(cmd: Seq[String]): Unit
}

object Sys {

  def apply(logger: Logger): Sys =
    new Impl(new Output(logger))

  final private class Impl(pl: ProcessLogger) extends Sys {

    def exec(cmd: Seq[String]): Int =
      Process(cmd).!(pl)

    def execSuccess(cmd: Seq[String]): Unit =
      exec(cmd) match {
        case 0 => ()
        case n =>
          val cmdStr = cmd.mkString(" ")
          sys.error(s"Command '${cmdStr}' finished with error: $n")
      }
  }

  final private class Output(logger: Logger) extends ProcessLogger {
    def buffer[T](f: => T): T = f
    def err(s: => String): Unit =
      logger.error(s)

    def out(s: => String): Unit =
      logger.info(s)
  }
}
