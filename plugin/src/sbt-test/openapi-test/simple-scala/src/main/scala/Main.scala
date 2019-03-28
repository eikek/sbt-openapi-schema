import org.myapi._

object Main {

  def main(args: Array[String]): Unit = {
    val room = Room("main room", Some(68))
    println(s"room = $room")
  }
}
