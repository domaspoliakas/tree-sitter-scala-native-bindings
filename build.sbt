import java.nio.file.Paths
import java.io.File

ThisBuild / scalaVersion := "3.3.0"

Global / onChangedBuildSource := ReloadOnSourceChanges

resolvers ++= Resolver.sonatypeOssRepos("snapshots")

lazy val core =
  project
    .in(file("modules/core"))
    .enablePlugins(ScalaNativePlugin)
    .settings(
      name := "core",
      scalaVersion := "3.3.0"
    )

lazy val coreRef = LocalProject("core")

lazy val json = 
  project
    .in(file("modules/json"))
    .enablePlugins(ScalaNativePlugin)
    .dependsOn(core)
    .settings(
      Compile / resourceGenerators += Def.task {
        val jsonParserLocation =
          (root / baseDirectory).value / "tree-sitter-json" / "src"

        val resourcesFolder = (Compile / resourceManaged).value / "scala-native"

        val fileNames = List(
          "parser.c"
        )

        fileNames.foreach { fileName =>
          IO.copyFile(jsonParserLocation / fileName, resourcesFolder / fileName)
        }

        fileNames.map(fileName => resourcesFolder / fileName)
      },
      nativeConfig := {
        val base = (root / baseDirectory).value / "tree-sitter"
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
      libraryDependencies +=  "com.disneystreaming" %%% "weaver-cats" % "0.8.3" % Test,
      testFrameworks += new TestFramework("weaver.framework.CatsEffect")
    )

lazy val jsonRef = LocalProject("json")

lazy val root = project.in(file("."))
  .aggregate(coreRef, jsonRef)
