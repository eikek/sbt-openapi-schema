package org.myapp

import org.myapi._

import java.time._

case class Ident(id: String)
case class RoomImpl(name: String, seats: Option[Int]) extends Room
case class PersonImpl(firstname: Option[String], lastname: Option[String], dob: Option[Instant]) extends Person
case class MapperImpl(id: Ident, secondary: List[Ident], fallback: Option[Ident]) extends Mapper
object PetImpl {
  case class Cat(huntingSkill: String, name: String) extends Pet.Cat
  case class Dog(packSize: Int, name: String) extends Pet.Dog
}

object Main {

  def main(args: Array[String]): Unit = {
    val room = RoomImpl("main room", Some(68))
    val person = PersonImpl(Some("Hans"), Some("Hanslein"), Some(Instant.now))
    println(s"room = $room, person = $person")

    val cat = PetImpl.Cat("rabbits", "Tom")
    val dog = PetImpl.Dog(1, "Lassie")
    println(s"cat = $cat, dog = $dog")

    val mapper = MapperImpl(Ident("id1"), List(Ident("id2")), Option(Ident("id3")))
    println(s"mapper=$mapper")
  }
}
