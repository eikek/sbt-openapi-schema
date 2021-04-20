package com.github.eikek.sbt.openapi

import scala.concurrent.duration._

import _root_.io.swagger.codegen.v3.cli.SwaggerCodegen
import com.github.eikek.sbt.openapi.impl._
import sbt.Keys._
import sbt._

object OpenApiSchema extends AutoPlugin {

  object autoImport {
    sealed trait Language {
      self: Product =>
      def extension: String = productPrefix.toLowerCase
    }
    object Language {
      case object Java  extends Language
      case object Scala extends Language
      case object Elm   extends Language
    }

    val openapiSpec    = settingKey[File]("The openapi specification")
    val openapiPackage = settingKey[Pkg]("The package to place the generated files into")
    val openapiJavaConfig =
      settingKey[JavaConfig]("Configuration for generating java files")
    val openapiScalaConfig =
      settingKey[ScalaConfig]("Configuration for generating scala files")
    val openapiElmConfig = settingKey[ElmConfig]("Configuration for generating elm files")
    val openapiTargetLanguage =
      settingKey[Language]("The target language: either Language.Scala or Language.Java.")
    val openapiOutput    = settingKey[File]("The directory where files are generated")
    val openapiCodegen   = taskKey[Seq[File]]("Run the code generation")
    val openapiStaticDoc = taskKey[File]("Generate a static HTML documentation")
    val openapiStaticArgs = settingKey[Seq[String]](
      "Additional options to the command. In- and out-file are provided by the plugin. It is `-l html2' by default"
    )
    val openapiStaticOut =
      settingKey[File]("The target directory for static documentation")
  }

  import autoImport._

  val defaultSettings = Seq(
    openapiPackage := Pkg("org.myapi"),
    openapiJavaConfig := JavaConfig(),
    openapiScalaConfig := ScalaConfig(),
    openapiElmConfig := ElmConfig(),
    openapiOutput := {
      openapiTargetLanguage.value match {
        case Language.Elm => (Compile / target).value / "elm-src"
        case _            => (Compile / sourceManaged).value
      }
    },
    openapiCodegen := {
      val out      = openapiOutput.value
      val logger   = streams.value.log
      val cfgJava  = openapiJavaConfig.value
      val cfgScala = openapiScalaConfig.value
      val cfgElm   = openapiElmConfig.value
      val spec     = openapiSpec.value
      val pkg      = openapiPackage.value
      val lang     = openapiTargetLanguage.value
      generateCode(logger, out, lang, cfgJava, cfgScala, cfgElm, spec, pkg)
    },
    openapiStaticOut := (Compile / resourceManaged).value / "openapiDoc",
    openapiStaticArgs := Seq("-l", "html2"),
    openapiStaticDoc := {
      val logger = streams.value.log
      val out    = openapiStaticOut.value
      val spec   = openapiSpec.value
      val args   = openapiStaticArgs.value
      createOpenapiStaticDoc(logger, spec, args, out)
    }
  )

  override def projectSettings =
    defaultSettings ++ Seq(
      Compile / sourceGenerators ++= {
        if (openapiTargetLanguage.value == Language.Elm) Seq.empty
        else Seq((Compile / openapiCodegen).taskValue)
      }
    )

  def generateCode(
      logger: Logger,
      out: File,
      lang: Language,
      cfgJava: JavaConfig,
      cfgScala: ScalaConfig,
      cfgElm: ElmConfig,
      spec: File,
      pkg: Pkg
  ): Seq[File] = {

    val targetPath = pkg.name.split('.').foldLeft(out)(_ / _)
    IO.createDirectories(Seq(targetPath))

    val schemas: Seq[SingularSchemaClass] = Parser.parse(spec.toString).values.toList
    val groupedSchemas                    = groupDiscriminantSchemas(schemas)

    val files = groupedSchemas.map { sc =>
      val (name, code) = (lang, sc) match {
        case (Language.Scala, _) => ScalaCode.generate(sc, pkg, cfgScala)
        case (Language.Java, _: SingularSchemaClass) =>
          JavaCode.generate(sc, pkg, cfgJava)
        case (Language.Elm, _: SingularSchemaClass) => ElmCode.generate(sc, pkg, cfgElm)
        case _                                      => sys.error(s"Java and Elm not yet supported for discriminants")
      }
      val file = targetPath / (name + "." + lang.extension)
      if (!file.exists || IO.read(file) != code) {
        logger.info(s"Writing file $file")
        IO.write(file, code)
      }
      file
    }

    IO.listFiles(targetPath).filter(f => !files.contains(f)).foreach { f =>
      logger.info(s"Deleting unused file $f")
      IO.delete(f)
    }

    files
  }

  def groupDiscriminantSchemas(
      parserResult: Seq[SingularSchemaClass]
  ): Seq[SchemaClass] = {
    val allSchemas = parserResult.map { ssc =>
      val referencing = parserResult.find(_.oneOfRef.contains(ssc.name))
      referencing match {
        case Some(ref) => ssc.copy(allOfRef = Some(ref.name))
        case None      => ssc
      }
    }

    val discriminantSchemasMap =
      allSchemas.filter(_.allOfRef.isDefined).groupBy(_.allOfRef.get)
    val discriminantSchemas = discriminantSchemasMap.map {
      case (allOfRef, childSchemas) =>
        val allOfRoot = allSchemas.collectFirst {
          case ssc if ssc.name == allOfRef => ssc
        }.get
        DiscriminantSchemaClass(
          allOfRoot.name,
          allOfRoot.properties,
          allOfRoot.doc,
          allOfRoot.wrapper,
          childSchemas.toList
        )
    }.toSeq

    val singularSchemas: Seq[SingularSchemaClass] = allSchemas
      .filterNot(ssc => discriminantSchemasMap.contains(ssc.name))
      .filterNot(ssc =>
        discriminantSchemasMap.values.toList.flatten.map(_.name).contains(ssc.name)
      )

    singularSchemas ++ discriminantSchemas
  }

  def createOpenapiStaticDoc(
      logger: Logger,
      openapi: File,
      args: Seq[String],
      out: File
  ): File = {
    val cl = Thread.currentThread.getContextClassLoader
    val command =
      Seq("generate", "-i", openapi.toString) ++ args ++ Seq("-o", out.toString)
    logger.info(s"Creating static html rest documentation: ${command.toList}")
    IO.createDirectory(out)
    val file = out / "index.html"
    Thread.currentThread.setContextClassLoader(classOf[SwaggerCodegen].getClassLoader)
    try {
      SwaggerCodegen.main(command.toArray)
      // the above command starts a new thread that does the work so
      // the call returns immediately, but the file is still being
      // generated
      val sw = Stopwatch.start
      while (!file.exists && sw.isBelow(20.seconds))
        Thread.sleep(100)
    } finally Thread.currentThread.setContextClassLoader(cl)
    if (!file.exists) {
      sys.error(
        s"Documentation generation failed. No file produced. '$file' doesn't exist."
      )
    }
    logger.info(s"Generated static file ${file}")
    file
  }

  final case class Stopwatch(start: Long) {
    def isBelow(fd: FiniteDuration): Boolean =
      Duration.fromNanos(System.nanoTime - start) < fd
  }
  object Stopwatch {
    def start: Stopwatch = Stopwatch(System.nanoTime)
  }
}
