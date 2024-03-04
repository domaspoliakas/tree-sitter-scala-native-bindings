import java.nio.file.Paths
import java.io.File

ThisBuild / scalaVersion := "3.3.0"
ThisBuild / version := "0.0.1"

Global / onChangedBuildSource := ReloadOnSourceChanges

resolvers ++= Resolver.sonatypeOssRepos("snapshots")

lazy val core =
  project
    .in(file("modules/core"))
    .enablePlugins(ScalaNativePlugin)
    .settings(
      name := "core",
      scalaVersion := "3.3.0",
      libraryDependencies ++= Seq(
        "org.typelevel" %%% "cats-core" % "2.9.0",
        "com.disneystreaming" %%% "weaver-cats" % "0.8.3" % Test
      ),
      testFrameworks += new TestFramework("weaver.framework.CatsEffect")
    ).settings(treesitterTestConfig)

lazy val treesitterTestConfig = Seq(
  Test / nativeConfig := {
    val base = (ThisBuild / baseDirectory).value / "tree-sitter"
    val conf = nativeConfig.value
    val staticLib = base / "libtree-sitter.a"

    conf
      .withLinkingOptions(
        conf.linkingOptions ++ List(
          staticLib.toString
        )
      )
      .withCompileOptions(
        conf.compileOptions ++ List(s"-I${base / "lib" / "include"}")
      )
  },
  libraryDependencies += "com.disneystreaming" %%% "weaver-cats" % "0.8.3" % Test,
  testFrameworks += new TestFramework("weaver.framework.CatsEffect")
)

lazy val json =
  project
    .in(file("modules/json"))
    .enablePlugins(ScalaNativePlugin)
    .dependsOn(core)
    .settings(treesitterTestConfig)

lazy val yaml =
  project
    .in(file("modules/yaml"))
    .enablePlugins(ScalaNativePlugin)
    .dependsOn(core)
    .settings(treesitterTestConfig)

lazy val sql =
  project
    .in(file("modules/sql"))
    .enablePlugins(ScalaNativePlugin)
    .dependsOn(core)
    .settings(treesitterTestConfig)

lazy val tests =
  project
    .in(file("modules/tests"))
    .enablePlugins(ScalaNativePlugin)
    .dependsOn(sql, yaml)
    .settings(treesitterTestConfig)

lazy val root = project
  .in(file("."))
  .aggregate(core, json, yaml, sql, tests)
