package com.github.eikek.sbt.openapi

import com.github.eikek.sbt.openapi.impl.SingularSchemaClass
import minitest.SimpleTestSuite

object OpenApiSchemaSpec extends SimpleTestSuite {

  test("Separating schemas") {
    val startingSchemas = Seq(
      SingularSchemaClass("Foo", List(Property("thisIsAString", Type.String), Property("anotherField", Type.Int32)), discriminatorRef = Some("Stuff")),
      SingularSchemaClass("Bar", List(Property("barField", Type.String), Property("newBool", Type.Bool)), discriminatorRef = Some("Stuff")),
      SingularSchemaClass("Stuff", List(Property("type", Type.String, false, discriminator = true))),
      SingularSchemaClass("SingularThing", List(Property("first", Type.String), Property("second", Type.Bool)))
    )
    val (actualSingularSchemas, actualDiscriminantSchemas) = OpenApiSchema.separateSchemas(startingSchemas)

    val expectedSingularSchemas = Seq(SingularSchemaClass("SingularThing", List(Property("first", Type.String), Property("second", Type.Bool))))

    assertEquals(actualSingularSchemas, expectedSingularSchemas)
  }
}
