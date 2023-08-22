package dev.poliakas.treesitter.yaml

import scala.scalanative.unsafe.Ptr
import scala.scalanative.unsafe.extern
import treesitter.structs.TSLanguage

def tree_sitter_yaml(): Ptr[TSLanguage] = extern

val Language = dev.poliakas.treesitter.core.Language.from(tree_sitter_yaml())

