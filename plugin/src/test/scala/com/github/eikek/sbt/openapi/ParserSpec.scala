package com.github.eikek.sbt.openapi

import com.github.eikek.sbt.openapi.impl.Parser
import minitest.SimpleTestSuite

object ParserSpec extends SimpleTestSuite {

  test("Parsing out properties from discriminator schema") {
    val test1 = getClass.getResource("/test1.yml")
    val schema = Parser.parse(test1.toString)

    val actual = schema("DiscriminatorObject")

    assertEquals(actual.name, "DiscriminatorObject")
    assertEquals(actual.allOfRef, None)
    val propsWithNoDocs: Set[Property] =
      actual.properties.map(_.copy(doc = Doc.empty)).toSet
    assertEquals(
      propsWithNoDocs,
      Set(
        Property("type", Type.String, false, None, None, None, Doc.empty, true),
        Property("sharedString", Type.String, true, None, None, None, Doc.empty, false),
        Property(
          "anotherSharedBoolean",
          Type.Bool,
          false,
          None,
          None,
          None,
          Doc.empty,
          false
        )
      )
    )
  }

  test("Parsing out properties from composed schema with discriminator") {
    val test1 = getClass.getResource("/test1.yml")
    val schema = Parser.parse(test1.toString)

    val actual = schema("FirstDiscriminatorSubObject")

    assertEquals(actual.name, "FirstDiscriminatorSubObject")
    assertEquals(actual.allOfRef, Some("DiscriminatorObject"))
    val propsWithNoDocs: Set[Property] =
      actual.properties.map(_.copy(doc = Doc.empty)).toSet
    assertEquals(
      propsWithNoDocs,
      Set(
        Property("uniqueString", Type.String, true, None, None, None, Doc.empty, false)
      )
    )
  }

  test(
    "Parsing out properties from composed schema with discriminator (different order)"
  ) {
    val test1 = getClass.getResource("/test1.yml")
    val schema = Parser.parse(test1.toString)

    val actual = schema("SecondDiscriminatorObject")

    assertEquals(actual.name, "SecondDiscriminatorObject")
    assertEquals(actual.allOfRef, Some("DiscriminatorObject"))
    val propsWithNoDocs: Set[Property] =
      actual.properties.map(_.copy(doc = Doc.empty)).toSet
    assertEquals(
      propsWithNoDocs,
      Set(
        Property("uniqueInteger", Type.Int32, false, None, None, None, Doc.empty, false),
        Property("otherUniqueBoolean", Type.Bool, true)
      )
    )
  }

  test("Parsing out properties from flat schema") {
    val test1 = getClass.getResource("/test1.yml")
    val schema = Parser.parse(test1.toString)

    val actual = schema("Room")

    assertEquals(actual.name, "Room")
    assertEquals(actual.allOfRef, None)
    val propsWithNoDocs: Set[Property] =
      actual.properties.map(_.copy(doc = Doc.empty)).toSet
    assertEquals(
      propsWithNoDocs,
      Set(
        Property("name", Type.String, false, None, None, None, Doc.empty, false),
        Property("seats", Type.Int32, true, Some("int32"), None, None, Doc.empty, false)
      )
    )
  }
}
