ThisBuild / scalaVersion := "2.13.2"
ThisBuild / version := "0"
ThisBuild / organization := "de.martinpallmann"
ThisBuild / organizationName := "Martin Pallmann"
ThisBuild / licenses ++= Seq(("MIT", url("http://opensource.org/licenses/MIT")))
ThisBuild / startYear := Some(2020)
ThisBuild / scalacOptions ++= Seq(
  "-Xfatal-warnings",
  "-deprecation",
  "-feature",
  "-unchecked",
  "-language:implicitConversions",
  "-language:higherKinds"
)

val header = HeaderLicense.Custom(
  """|Copyright (c) 2019 by Rob Norris
     |Copyright (c) 2020 by Martin Pallmann
     |This software is licensed under the MIT License (MIT).
     |For more information see LICENSE or https://opensource.org/licenses/MIT
     |""".stripMargin
)

val calibanVersion = "0.8.1"
val openTracingVersion = "0.33.0"
val verifyVersion = "0.2.0"
val zioVersion = "1.0.0-RC20"

lazy val core = project
  .enablePlugins(AutomateHeaderPlugin)
  .settings(
    moduleName := "ariadne-core",
    libraryDependencies ++= Seq("dev.zio" %% "zio" % zioVersion),
    headerLicense := Some(header)
  )

lazy val opentracing = project
  .enablePlugins(AutomateHeaderPlugin)
  .settings(
    moduleName := "ariadne-opentracing",
    libraryDependencies ++= Seq(
      "io.opentracing" % "opentracing-api" % openTracingVersion,
      "io.opentracing" % "opentracing-mock" % openTracingVersion % Test,
      "com.eed3si9n.verify" %% "verify" % verifyVersion % Test
    ),
    headerLicense := Some(header),
    testFrameworks += new TestFramework("verify.runner.Framework")
  )
  .dependsOn(core)

lazy val examples = project
  .enablePlugins(AutomateHeaderPlugin)
  .settings(
    moduleName := "ariadne-examples",
    libraryDependencies ++= Seq(
      "io.opentracing" % "opentracing-mock" % openTracingVersion,
      "com.github.ghostdogpr" %% "caliban" % calibanVersion,
      "com.github.ghostdogpr" %% "caliban-uzhttp" % calibanVersion
    ),
    headerLicense := Some(header),
    fork in run := true
  )
  .dependsOn(opentracing)
