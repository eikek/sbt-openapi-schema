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
      case object Elm extends Language
    }

    val openapiSpec = settingKey[File]("The openapi specification")
    val openapiPackage = settingKey[Pkg]("The package to place the generated files into")
    val openapiJavaConfig = settingKey[JavaConfig]("Configuration for generating java files")
    val openapiScalaConfig = settingKey[ScalaConfig]("Configuration for generating scala files")
    val openapiElmConfig = settingKey[ElmConfig]("Configuration for generating elm files")
    val openapiTargetLanguage = settingKey[Language]("The target language: either Language.Scala or Language.Java.")
    val openapiOutput = settingKey[File]("The directory where files are generated")
    val openapiCodegen = taskKey[Seq[File]]("Run the code generation")
  }

  import autoImport._

  val defaultSettings = Seq(
    openapiPackage := Pkg("org.myapi"),
    openapiJavaConfig := JavaConfig(),
    openapiScalaConfig := ScalaConfig(),
    openapiElmConfig := ElmConfig(),
    openapiOutput := {
      openapiTargetLanguage.value match {
        case Language.Elm => (Compile/target).value/"elm-src"
        case _ => (Compile/sourceManaged).value
      }
    },
    openapiCodegen := {
      val out = openapiOutput.value
      val logger = streams.value.log
      val cfgJava = openapiJavaConfig.value
      val cfgScala = openapiScalaConfig.value
      val cfgElm = openapiElmConfig.value
      val spec = openapiSpec.value
      val pkg = openapiPackage.value
      val lang = openapiTargetLanguage.value
      generateCode(logger, out, lang, cfgJava, cfgScala, cfgElm, spec, pkg)
    }
  )

  override def projectSettings =
    defaultSettings ++ Seq(
      Compile / sourceGenerators ++= {
        if (openapiTargetLanguage.value == Language.Elm) Seq.empty
        else Seq((Compile / openapiCodegen).taskValue)
      },
      Compile /resourceGenerators ++= {
        if (openapiTargetLanguage.value != Language.Elm) Seq.empty
        else Seq((Compile / openapiCodegen).taskValue)
      }
    )

  private def generateCode(logger: Logger
    , out: File
    , lang: Language
    , cfgJava: JavaConfig
    , cfgScala: ScalaConfig
    , cfgElm: ElmConfig
    , spec: File
    , pkg: Pkg): Seq[File] = {

    val targetPath = pkg.name.split('.').foldLeft(out)(_ / _)
    val schemas = Parser.parse(spec.toString).values.toList

    IO.createDirectories(Seq(targetPath))
    val files = schemas.map { sc =>
      val (name, code) = lang match {
        case Language.Scala => ScalaCode.generate(sc, pkg, cfgScala)
        case Language.Java => JavaCode.generate(sc, pkg, cfgJava)
        case Language.Elm => ElmCode.generate(sc, pkg, cfgElm)
      }
      val file = targetPath / (name + "." + lang.extension)
      if (!file.exists || IO.read(file) != code) {
        logger.info(s"Writing file $file")
        IO.write(file, code)
      }
      file
    }
    IO.listFiles(targetPath).
      filter(f => !files.contains(f)).
      foreach { f =>
        logger.info(s"Deleting unused file $f")
        IO.delete(f)
      }

    files
  }
}
