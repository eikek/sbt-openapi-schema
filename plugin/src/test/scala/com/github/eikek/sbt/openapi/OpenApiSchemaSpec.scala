package com.github.eikek.sbt.openapi

import com.github.eikek.sbt.openapi.impl.{DiscriminantSchemaClass, SingularSchemaClass}
import minitest.SimpleTestSuite

object OpenApiSchemaSpec extends SimpleTestSuite {

  test("Separating schemas") {
    val startingSchemas = Seq(
      SingularSchemaClass("Foo", List(Property("thisIsAString", Type.String), Property("anotherField", Type.Int32)), discriminatorRef = Some("Stuff")),
      SingularSchemaClass("Bar", List(Property("barField", Type.String), Property("newBool", Type.Bool)), discriminatorRef = Some("Stuff")),
      SingularSchemaClass("Stuff", List(Property("type", Type.String, false, discriminator = true))),
      SingularSchemaClass("SingularThing", List(Property("first", Type.String), Property("second", Type.Bool)))
    )
    val actualSchemas = OpenApiSchema.groupDiscriminantSchemas(startingSchemas)

    val expectedSchemas = Seq(
      SingularSchemaClass("SingularThing", List(Property("first", Type.String), Property("second", Type.Bool))),
      DiscriminantSchemaClass("Stuff", List(Property("type", Type.String, false, discriminator = true)), subSchemas = List(
        SingularSchemaClass("Foo", List(Property("thisIsAString", Type.String), Property("anotherField", Type.Int32)), discriminatorRef = Some("Stuff")),
        SingularSchemaClass("Bar", List(Property("barField", Type.String), Property("newBool", Type.Bool)), discriminatorRef = Some("Stuff"))
      ))
    )

    assertEquals(actualSchemas, expectedSchemas)
  }
}
