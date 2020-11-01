package com.github.eikek.sbt.openapi.impl

import com.github.eikek.sbt.openapi._
import minitest.SimpleTestSuite

object ScalaCodeSpec extends SimpleTestSuite {

  test("Discriminant Schema") {
    val schemaClass = DiscriminantSchemaClass(
      "Stuff",
      properties = List(
        Property("shared1", Type.String, nullable = true),
        Property("type", Type.String, discriminator = true)
      ),
      subSchemas = List(
        SingularSchemaClass(
          "Foo",
          List(
            Property("thisIsAString", Type.String),
            Property("anotherField", Type.Int32)
          ),
          allOfRef = Some("Stuff")
        ),
        SingularSchemaClass(
          "Bar",
          List(Property("barField", Type.String), Property("newBool", Type.Bool)),
          allOfRef = Some("Stuff")
        )
      )
    )

    val result = ScalaCode.generate(
      schemaClass,
      Pkg("com.test"),
      ScalaConfig().withJson(ScalaJson.circeSemiautoExtra)
    )

    println(result._2)
  }
}
