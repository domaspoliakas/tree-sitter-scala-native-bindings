package dev.poliakas.treesitter.json

import scala.scalanative.unsafe.Ptr
import scala.scalanative.unsafe.extern
import treesitter.structs.TSLanguage

def tree_sitter_yaml(): Ptr[TSLanguage] = extern
