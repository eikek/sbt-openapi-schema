package org.myapp

import org.myapi._

import java.time._

case class Ident(id: String)

object Main {

  def main(args: Array[String]): Unit = {
    val room = Room("main room", Some(68))
    val person = Person(Some("Hans"), Some("Hanslein"), Some(Instant.now))
    println(s"room = $room, person = $person")

    val mapper = Mapper(Ident("id1"),  List(Ident("id2")), Option(Ident("id3")))
    println(s"mapper=$mapper")
  }
}
