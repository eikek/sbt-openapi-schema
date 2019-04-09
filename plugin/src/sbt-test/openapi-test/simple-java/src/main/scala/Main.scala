import org.myapi._
import java.net.URI

object Main {

  def main(args: Array[String]): Unit = {
    val room = new Room("main room", 68, URI.create("test:a:b"))
    println(s"room = $room")
  }
}
