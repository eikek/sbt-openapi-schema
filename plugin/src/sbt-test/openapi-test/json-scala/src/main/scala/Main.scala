import org.myapi._
import java.time.LocalDate
import io.circe._, io.circe.generic.semiauto._, io.circe.parser._
import io.circe.syntax._

object Main {

  def main(args: Array[String]): Unit = {
    testJson(RoomDto("main room dto", Some(68)))
    testJson(SimpleStringDto("bla bla bla"))
    testJson(
      CourseDto(
        "course10",
        LocalDate.now,
        RoomDto("heat room", Some(12)),
        List(PersonDto(Some("hugo"), "meyer", None))
      )
    )
    testJson(
      ExtractedData1Dto(
        Map(
          "c1" -> CourseDto(
            "course10",
            LocalDate.now,
            RoomDto("heat room", Some(12)),
            List(PersonDto(Some("hugo"), "meyer", None))
          )
        )
      )
    )
    testJson(ExtractedData2Dto(Map("c1" -> "v1", "c2" -> "v2")))
    testJson(CustomJsonDto("a name", Some(Json.obj("test" -> Json.fromInt(5))), Json.fromString("help")))
  }

  def testJson[A](a: A)(implicit d: Decoder[A], e: Encoder[A]): Unit = {
    val jsonStr = toJson(a)
    val backA = toValue(jsonStr)
    println(s"JSON: $jsonStr  BACK: $backA")
    assert(backA == a)
  }

  def toJson[A](a: A)(implicit enc: Encoder[A]): String =
    a.asJson.noSpaces

  def toValue[A](json: String)(implicit d: Decoder[A]): A =
    parse(json).right.get.as[A].right.get

}
