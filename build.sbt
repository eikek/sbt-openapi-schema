import Dependencies._
import ReleaseTransformations._

scalaVersion in ThisBuild := "2.13.5"

val sharedSettings = Seq(
  name := "sbt-openapi-schema",
  organization := "com.github.eikek",
  scalaVersion := "2.13.5",
  licenses := Seq("MIT" -> url("http://spdx.org/licenses/MIT")),
  homepage := Some(url("https://github.com/eikek")),
  scalacOptions in (Compile, console) := Seq(),
  scalacOptions ++= Seq(
    "-encoding", "UTF-8",
    "-Xfatal-warnings", // fail when there are warnings
    "-deprecation",
    "-feature",
    "-unchecked",
    "-language:higherKinds",
    "-Xlint",
    "-Yno-adapted-args",
    "-Ywarn-dead-code",
    "-Ywarn-numeric-widen",
    "-Ywarn-unused-import"
  )
) ++ publishSettings

lazy val publishSettings = Seq(
  publishMavenStyle := true,
  scmInfo := Some(
    ScmInfo(
      url("https://github.com/eikek/sbt-openapi-schema.git"),
      "scm:git:git@github.com:eikek/sbt-openapi-schema.git"
    )
  ),
  developers := List(
    Developer(
      id = "eikek",
      name = "Eike Kettner",
      url = url("https://github.com/eikek"),
      email = ""
    )
  ),
  homepage := Some(url("https://github.com/eikek/sbt-openapi-schema")),
  publishTo := sonatypePublishToBundle.value,
  publishArtifact in Test := false,
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

lazy val testSettings = Seq(
  testFrameworks += new TestFramework("minitest.runner.Framework"),
  libraryDependencies ++= Seq(minitest, `logback-classic`).map(_ % "test")
)

lazy val plugin = project.in(file("plugin")).
  enablePlugins(SbtPlugin).
  settings(sharedSettings).
  settings(publishSettings).
  settings(testSettings).
  settings(
    sbtPlugin := true,
    libraryDependencies ++= Seq(`swagger-parser`, swaggerCodegen),
    scriptedLaunchOpts := { scriptedLaunchOpts.value ++
      Seq("-Xmx1024M", "-Dplugin.version=" + version.value)
    },
    scriptedBufferLog := false
  )

lazy val root = project
  .in(file("."))
  .settings(sharedSettings)
  .settings(noPublish)
  .settings(
    name := "sbt-openapi-schema"
  )
  .aggregate(plugin)
