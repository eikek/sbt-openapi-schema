package com.github.eikek.sbt.openapi

import sbt._
import sbt.Keys._
import com.github.eikek.sbt.openapi.impl._

object OpenApiSchema extends AutoPlugin {

  object autoImport {
    sealed trait Language {
      self: Product =>
      def extension: String = productPrefix.toLowerCase
    }
    object Language {
      case object Java extends Language
      case object Scala extends Language
    }

    val openapiSpec = settingKey[File]("The openapi specification")
    val openapiPackage = settingKey[Pkg]("The package to place the generated files into")
    val openapiJavaConfig = settingKey[JavaConfig]("Configuration for generating java files")
    val openapiScalaConfig = settingKey[ScalaConfig]("Configuration for generating scala files")
    val openapiTargetLanguage = settingKey[Language]("The target language: either Language.Scala or Language.Java.")
    val openapiCodegen = taskKey[Seq[File]]("Run the code generation")
  }

  import autoImport._

  val defaultSettings = Seq(
    openapiPackage := Pkg("org.myapi"),
    openapiJavaConfig := JavaConfig(),
    openapiScalaConfig := ScalaConfig(),
    openapiCodegen := {
      val out = (Compile/sourceManaged).value
      val logger = streams.value.log
      val cfgJava = openapiJavaConfig.value
      val cfgScala = openapiScalaConfig.value
      val spec = openapiSpec.value
      val pkg = openapiPackage.value
      val lang = openapiTargetLanguage.value
      generateCode(logger, out, lang, cfgJava, cfgScala, spec, pkg)
    }
  )

  override def projectSettings =
    defaultSettings ++ Seq(
      Compile / sourceGenerators += (Compile / openapiCodegen).taskValue
    )

  private def generateCode(logger: Logger
    , out: File
    , lang: Language
    , cfgJava: JavaConfig
    , cfgScala: ScalaConfig
    , spec: File
    , pkg: Pkg): Seq[File] = {

    val targetPath = pkg.name.split('.').foldLeft(out)(_ / _)
    val schemas = Parser.parse(spec.toString).values.toList

    IO.createDirectories(Seq(targetPath))
    schemas.map { sc =>
      val (name, code) =
        if (lang == Language.Scala) ScalaCode.generate(sc, pkg, cfgScala)
        else JavaCode.generate(sc, pkg, cfgJava)
      val file = targetPath / (name + "." + lang.extension)
      if (!file.exists || IO.read(file) != code) {
        logger.info(s"Writing file $file")
        IO.write(file, code)
      }
      file
    }
  }
}
