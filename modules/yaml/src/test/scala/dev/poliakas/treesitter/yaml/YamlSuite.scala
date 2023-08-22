package dev.poliakas.treesitter.yaml

import treesitter.all.*
import scala.scalanative.unsafe.*
import scala.scalanative.unsigned.*
import scala.scalanative.libc.string.strlen
import scala.scalanative.libc.stdio

object YamlSuite extends weaver.FunSuite {

  test("hek"): 
    Zone: zone => 
      given Zone = zone
      val parser = ts_parser_new()
      ts_parser_set_language(parser, tree_sitter_yaml())

      val source = toCString("""hello: hello
                               |hello: hello""".stripMargin)

      val tree = 
        ts_parser_parse_string(parser, null, source, strlen(source).toUInt)

      val root_node = ts_tree_root_node(tree)

      expect(1 == ts_node_child_count(root_node).toInt)
      val c1 = ts_node_child(root_node, 0.toUInt)
      expect("potato" == fromCString(ts_node_type(c1)))

}
