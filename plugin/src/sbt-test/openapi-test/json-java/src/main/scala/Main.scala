import org.myapi._
import scala.reflect._
import com.fasterxml.jackson.databind.ObjectMapper;
import scala.collection.JavaConverters._
import java.time.LocalDate
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule

object Main {
  private val m = {
    val om = new ObjectMapper()
    om.registerModule(new JavaTimeModule())
    om
  }

  def main(args: Array[String]): Unit = {
    List(new RoomDto("main room dto", 68)
      , new SimpleStringDto("bla bla bla")
      , new ExtractedData1Dto(Map("c1" -> new CourseDto("course10", LocalDate.now, new RoomDto("heat room", 12), List(new PersonDto("hugo", "meyer", null)).asJava)).asJava)
      , new ExtractedData2Dto(Map("c1" -> "x1", "c2" -> "x2").asJava)
      , new CourseDto("course10", LocalDate.now, new RoomDto("heat room", 12), List(new PersonDto("hugo", "meyer", null)).asJava),
      new NestedArrayDto(List(List[Integer](1, 2).asJava, List[Integer](30, 40).asJava).asJava)
    ).foreach(testJson _)

    assert(toJson(new SimpleStringDto("bla")) == "\"bla\"")
  }

  def testJson(any: Any): Unit = {
    println(s"Value: $any")
    val clazz = any.getClass
    val jsonStr = toJson(any)
    println(s"JSON: $jsonStr  BACK: ${toValue(jsonStr, clazz)}")
    assert(any == toValue(jsonStr, clazz))
  }

  def toJson(any: Any): String = {
    m.writeValueAsString(any)
  }

  def toValue[A: ClassTag](js: String): A =
    toValue(js, classTag[A].runtimeClass.asInstanceOf[Class[A]])

  def toValue[A](js: String, ct: Class[A]): A =
    m.readValue(js, ct)
}
