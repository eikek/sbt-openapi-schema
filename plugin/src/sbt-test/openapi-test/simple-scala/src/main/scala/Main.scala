import org.myapi._

import java.time._

object Main {

  def main(args: Array[String]): Unit = {
    val room   = Room("main room", Some(68))
    val person = Person(Some("Hans"), Some("Hanslein"), Some(Instant.now))
    println(s"room = $room, person = $person")
  }
}
