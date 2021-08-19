import Dependencies._
import ReleaseTransformations._

ThisBuild / scalaVersion  := "2.13.6"
ThisBuild / versionScheme := Some("early-semver")

addCommandAlias("ci", "; lint; test; scripted; publishLocal")
addCommandAlias(
  "lint",
  "; scalafmtSbtCheck; scalafmtCheckAll; Compile/scalafix --check; Test/scalafix --check"
)
addCommandAlias("fix", "; Compile/scalafix; Test/scalafix; scalafmtSbt; scalafmtAll")

val sharedSettings = Seq(
  name         := "sbt-openapi-schema",
  organization := "com.github.eikek",
  scalaVersion := "2.13.6",
  licenses     := Seq("MIT" -> url("http://spdx.org/licenses/MIT")),
  homepage     := Some(url("https://github.com/eikek/sbt-openapi-schema")),
  Compile / console / scalacOptions := Seq(),
  scalacOptions ++= Seq(
    "-encoding",
    "UTF-8",
    "-Xfatal-warnings", // fail when there are warnings
    "-deprecation",
    "-feature",
    "-unchecked",
    "-language:higherKinds",
    "-Xlint",
    "-Yno-adapted-args",
    "-Ywarn-dead-code",
    "-Ywarn-numeric-widen",
    "-Ywarn-unused-import",
    "-Wconf:cat=unused-nowarn:s"
  )
) ++ publishSettings

lazy val publishSettings = Seq(
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
  homepage               := Some(url("https://github.com/eikek/sbt-openapi-schema")),
  Test / publishArtifact := false,
  releaseCrossBuild      := false,
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
  publish         := {},
  publishLocal    := {},
  publishArtifact := false
)

lazy val testSettings = Seq(
  testFrameworks += new TestFramework("minitest.runner.Framework"),
  libraryDependencies ++= Seq(minitest, `logback-classic`).map(_ % "test")
)

val scalafixSettings = Seq(
  semanticdbEnabled := true,                        // enable SemanticDB
  semanticdbVersion := scalafixSemanticdb.revision, // use Scalafix compatible version
  ThisBuild / scalafixDependencies += "com.github.liancheng" %% "organize-imports" % "0.5.0"
)

lazy val plugin = project
  .in(file("plugin"))
  .withId("sbt-openapi-schema")
  .enablePlugins(SbtPlugin)
  .settings(sharedSettings)
  .settings(publishSettings)
  .settings(testSettings)
  .settings(scalafixSettings)
  .settings(
    sbtPlugin := true,
    libraryDependencies ++= Seq(`swagger-parser`, swaggerCodegen),
    scriptedLaunchOpts := {
      scriptedLaunchOpts.value ++
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
