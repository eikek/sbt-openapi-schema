import Dependencies._
import ReleaseTransformations._

val scala3Version = "3.9.0"
val scala212Version = "2.12.21"

ThisBuild / versionScheme := Some("early-semver")

addCommandAlias("ci", "; lint; test; scripted; publishLocal")
addCommandAlias(
  "lint",
  "; scalafmtSbtCheck; scalafmtCheckAll; Compile/scalafix --check; Test/scalafix --check"
)
addCommandAlias("fix", "; Compile/scalafix; Test/scalafix; scalafmtSbt; scalafmtAll")

val testSettings = Seq(
  libraryDependencies ++= Dependencies.munit.map(_ % Test),
  testFrameworks += TestFrameworks.MUnit
)

val sharedSettings = Seq(
  name := "sbt-openapi-schema",
  organization := "com.github.eikek",
  licenses := Seq("MIT" -> uri("http://spdx.org/licenses/MIT")),
  homepage := Some(uri("https://github.com/eikek/sbt-openapi-schema")),
  Compile / console / scalacOptions := Seq(),
  scalacOptions ++= {
    val common = Seq(
      "-encoding",
      "UTF-8",
      "-deprecation",
      "-feature",
      "-unchecked"
    )
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, _)) =>
        common ++ Seq(
          "-Xfatal-warnings", // fail when there are warnings
          "-language:higherKinds",
          "-Xlint",
          "-Yno-adapted-args",
          "-Ywarn-dead-code",
          "-Ywarn-numeric-widen",
          "-Ywarn-unused-import",
          "-Wconf:cat=unused-nowarn:s"
        )
      case _ =>
        common ++ Seq(
          "-Werror", // fail when there are warnings
          "-source:3.3", // `Schema[_]` wildcards are only deprecated from source 3.4
          "-Wunused:imports"
        )
    }
  }
) ++ publishSettings

lazy val publishSettings = Seq(
  scmInfo := Some(
    ScmInfo(
      uri("https://github.com/eikek/sbt-openapi-schema.git"),
      "scm:git:git@github.com:eikek/sbt-openapi-schema.git"
    )
  ),
  developers := List(
    Developer(
      id = "eikek",
      name = "Eike Kettner",
      url = uri("https://github.com/eikek"),
      email = ""
    )
  ),
  homepage := Some(uri("https://github.com/eikek/sbt-openapi-schema")),
  Test / publishArtifact := false,
  releaseCrossBuild := false,
  releaseProcess := Seq[ReleaseStep](
    checkSnapshotDependencies,
    inquireVersions,
    runClean,
    runTest,
    setReleaseVersion,
    commitReleaseVersion,
    tagRelease,
    releaseStepCommandAndRemaining("publishSigned"),
    releaseStepCommand("sonatypeBundleRelease"),
    setNextVersion,
    commitNextVersion,
    pushChanges
  )
)

lazy val noPublish = Seq(
  publish := {},
  publishLocal := {},
  publishArtifact := false
)

val scalafixSettings = Seq(
  semanticdbEnabled := true, // enable SemanticDB
  semanticdbVersion := scalafixSemanticdb.revision // use Scalafix compatible version
)

lazy val plugin = (projectMatrix in file("plugin"))
  .enablePlugins(SbtPlugin)
  .settings(sharedSettings)
  .settings(testSettings)
  .settings(scalafixSettings)
  .settings(
    sbtPlugin := true,
    libraryDependencies ++= Seq(`swagger-parser`, swaggerCodegen),
    libraryDependencies ++= {
      CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, _)) =>
          Seq("org.scala-lang.modules" %% "scala-collection-compat" % "2.14.0")
        case _ => Nil
      }
    },
    scriptedLaunchOpts :=
      scriptedLaunchOpts.value ++
        Seq("-Xmx1024M", "-Dplugin.version=" + version.value),
    scriptedBufferLog := false
  )
  .jvmPlatform(scalaVersions = Seq(scala3Version, scala212Version))

lazy val root = project
  .in(file("."))
  .settings(noPublish)
  .settings(
    scalaVersion := scala3Version
  )
  .aggregate(plugin.projectRefs *)
