package com.github.eikek.sbt.openapi

import com.github.eikek.sbt.openapi.impl._
import minitest._

object CodegenSpec extends SimpleTestSuite {

  val test1 = getClass.getResource("/test1.yml")
  val schema = Parser.parse(test1.toString)

  test("Running scala") {
    val config = ScalaConfig.default
      .withJson(ScalaJson.circeSemiauto)
      .addMapping(CustomMapping.forSource { case s =>
        s.addParents(Superclass("MyTrait", Imports.empty, false))
      })

    println("=========")
    schema.values.foreach { sc =>
      println("-------------------------------------------------")
      println(ScalaCode.generate(sc, Pkg("com.test"), config))
    }
  }

  test("Running elm") {
    val config = ElmConfig.default
      .addMapping(CustomMapping.forName { case name => name + "Dto" })
      .withJson(ElmJson.decodePipeline)

    println("=========")
    schema.values.foreach { sc =>
      println("-------------------------------------------------")
      println(ElmCode.generate(sc, Pkg("Api.Data"), config))
    }
  }
}
