package com.github.eikek.sbt.openapi

import minitest._
import com.github.eikek.sbt.openapi.impl._

object CodegenSpec extends SimpleTestSuite {

  val test1  = getClass.getResource("/test1.yml")
  val schema = Parser.parse(test1.toString)

  test("Running scala") {
    val config = ScalaConfig.default
      .withJson(ScalaJson.circeSemiauto)
      .addMapping(CustomMapping.forSource({ case s =>
        s.addParents(Superclass("MyTrait", Imports.empty, false))
      }))

    println("=========")
    schema.values.foreach { sc =>
      println("-------------------------------------------------")
      println(ScalaCode.generate(sc, Pkg("com.test"), config))
    }
  }

  test("Running java") {
    val typeMapping: CustomMapping =
      CustomMapping.forType {
        case TypeDef(s, _) if s.startsWith("List<") =>
          TypeDef(
            s.replaceFirst("List", "PList"),
            Imports("ch.bluecare.commons.data.PList")
          )
      }

    val config = JavaConfig.default
      .withJson(JavaJson.jackson)
      .addMapping(typeMapping)
      .addMapping(CustomMapping.forSource({ case s => s.copy(name = s.name + "Dto") }))
      .addMapping(CustomMapping.forSource({ case s =>
        s.addParents(Superclass("MyFunnyClass", Imports("org.myapp.MyFunnyclass"), false))
      }))
      .addBuilderParent(
        Superclass("MyBuilderHelper", Imports("org.mylib.BuilderHelper"), true)
      )

    println("=========")
    schema.values.foreach { sc =>
      println("-------------------------------------------------")
      println(JavaCode.generate(sc, Pkg("com.test"), config)._2)
    }
  }

  test("Running elm") {
    val config = ElmConfig.default
      .addMapping(CustomMapping.forName({ case name => name + "Dto" }))
      .withJson(ElmJson.decodePipeline)

    println("=========")
    schema.values.foreach { sc =>
      println("-------------------------------------------------")
      println(ElmCode.generate(sc, Pkg("Api.Data"), config))
    }
  }
}
