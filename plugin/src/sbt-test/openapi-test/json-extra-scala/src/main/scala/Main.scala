import org.myapi._
import java.time.LocalDate
import io.circe._, io.circe.generic.extras.semiauto._, io.circe.parser._
import io.circe.syntax._

object Main {

  def main(args: Array[String]): Unit = {
    testJson(RoomDto("main room dto", Some(68)))
    testJson(SimpleStringDto("bla bla bla"))
    testJson(CourseDto("course10", LocalDate.now, RoomDto("heat room", Some(12)), List(PersonDto(Some("hugo"), "meyer", None))))
    testJson(ExtractedData1Dto(Map("c1" -> CourseDto("course10", LocalDate.now, RoomDto("heat room", Some(12)), List(PersonDto(Some("hugo"), "meyer", None))))))
    testJson(ExtractedData2Dto(Map("c1" -> "v1", "c2" -> "v2")))
    val firstDiscriminator: DiscriminatorObjectDto = DiscriminatorObjectDto.FirstDiscriminatorSubObjectDto(uniqueString = Some("v1"), sharedString = Some("v2"), anotherSharedBoolean = true)
    val secondDiscriminator: DiscriminatorObjectDto = DiscriminatorObjectDto.SecondDiscriminatorObjectDto(uniqueInteger = 2, otherUniqueBoolean = None, sharedString = Some("v4"), anotherSharedBoolean = true)
    testJson(firstDiscriminator)
    testJson(secondDiscriminator)
    val catDiscriminator: PetDto = PetDto.CatDto("claw", "Sprinkles")
    val dogDiscriminator: PetDto = PetDto.DogDto(10, "Fido")
    testJson(catDiscriminator)
    testJson(dogDiscriminator)
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
