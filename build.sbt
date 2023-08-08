ThisBuild / scalaVersion := "3.3.0"

lazy val root = project.in(file("."))
  .aggregate(core.native)

lazy val core = p

import com.indoorvivants.detective.Platform.OS.*
import com.indoorvivants.detective.Platform
import bindgen.interface.Binding
import bindgen.interface.LogLevel
import java.nio.file.Paths
import java.io.File

Global / onChangedBuildSource := ReloadOnSourceChanges

resolvers ++= Resolver.sonatypeOssRepos("snapshots")

lazy val native =
  project
    .in(file("modules/core"))
    .enablePlugins(ScalaNativePlugin, BindgenPlugin)
    .settings(
      name := "native",
      scalaVersion := "3.3.0",
      bindgenBindings := Seq(
        Binding(
          baseDirectory.value / "tree-sitter" / "lib" / "include" / "tree_sitter" / "api.h",
          "treesitter",
          cImports = List("tree_sitter/api.h"),
          clangFlags = List("-std=gnu99")
        )
      ),
      Compile / resourceGenerators += Def.task {
        val jsonParserLocation =
          baseDirectory.value / "tree-sitter-json" / "src"

        val resourcesFolder = (Compile / resourceManaged).value / "scala-native"

        val fileNames = List(
          "parser.c"
          // "scanner.c"
        )

        fileNames.foreach { fileName =>
          IO.copyFile(jsonParserLocation / fileName, resourcesFolder / fileName)
        }

        fileNames.map(fileName => resourcesFolder / fileName)
      },
      nativeConfig := {
        val base = baseDirectory.value / "tree-sitter"
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
      }
    )

