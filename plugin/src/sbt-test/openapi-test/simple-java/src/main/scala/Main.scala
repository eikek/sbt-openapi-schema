import org.myapi._
import java.net.URI
import java.net.URL
import java.util.Optional
import java.time._

object Main {

  def main(args: Array[String]): Unit = {
    val room = new Room("main room", 68, URI.create("test:a:b")).withName("main room 2")
    val person = new Person("Hans", null, Instant.now, new URL("http://test.com"))
    println(s"room = $room, person = $person")
  }
}
