package com.github.eikek.sbt.openapi

import com.github.eikek.sbt.openapi.impl.{DiscriminantSchemaClass, SingularSchemaClass}
import minitest.SimpleTestSuite

object OpenApiSchemaSpec extends SimpleTestSuite {

  private val expectedResult = Seq(
      SingularSchemaClass("SingularThing", List(Property("first", Type.String), Property("second", Type.Bool))),
      DiscriminantSchemaClass("Stuff", List(Property("type", Type.String, false, discriminator = true)), subSchemas = List(
        SingularSchemaClass("Foo", List(Property("thisIsAString", Type.String), Property("anotherField", Type.Int32)), allOfRef = Some("Stuff")),
        SingularSchemaClass("Bar", List(Property("barField", Type.String), Property("newBool", Type.Bool)), allOfRef = Some("Stuff"))
      ))
    )

  test("Separating allOf schemas") {
    val startingSchemas = Seq(
      SingularSchemaClass("Foo", List(Property("thisIsAString", Type.String), Property("anotherField", Type.Int32)), allOfRef = Some("Stuff")),
      SingularSchemaClass("Bar", List(Property("barField", Type.String), Property("newBool", Type.Bool)), allOfRef = Some("Stuff")),
      SingularSchemaClass("Stuff", List(Property("type", Type.String, false, discriminator = true))),
      SingularSchemaClass("SingularThing", List(Property("first", Type.String), Property("second", Type.Bool)))
    )
    val actualSchemas = OpenApiSchema.groupDiscriminantSchemas(startingSchemas)

    assertEquals(actualSchemas, expectedResult)
  }

  test("Separating oneOf schemas") {
    val startingSchemas = Seq(
      SingularSchemaClass("Foo", List(Property("thisIsAString", Type.String), Property("anotherField", Type.Int32))),
      SingularSchemaClass("Bar", List(Property("barField", Type.String), Property("newBool", Type.Bool))),
      SingularSchemaClass("Stuff", List(Property("type", Type.String, false, discriminator = true)), oneOfRef = List("Foo", "Bar")),
      SingularSchemaClass("SingularThing", List(Property("first", Type.String), Property("second", Type.Bool)))
    )
    val actualSchemas = OpenApiSchema.groupDiscriminantSchemas(startingSchemas)

    assertEquals(actualSchemas, expectedResult)
  }

}
