package org.app

import java.time.LocalDate
import io.circe._

trait Codecs {

  implicit val dateDecoder: Decoder[LocalDate] =
    Decoder.decodeString.map(s => LocalDate.parse(s))

  implicit val dateEncoder: Encoder[LocalDate] =
    Encoder.encodeString.contramap(_.toString)

}

object Codecs extends Codecs
